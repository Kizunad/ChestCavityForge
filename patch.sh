#!/usr/bin/env bash
set -euo pipefail

ROOT="$(pwd)"
[[ -f "$ROOT/gradlew" ]] || { echo "请在包含 gradlew 的仓库根目录运行"; exit 1; }

JAVA_DIR="src/main/java"
BASE_PKG="net/tigereye/chestcavity"
SOUL_DIR="$JAVA_DIR/$BASE_PKG/soul"
ENTITY_DIR="$SOUL_DIR/entity"
DATA_DIR="$ENTITY_DIR/data"

mkdir -p "$ENTITY_DIR" "$DATA_DIR"

echo "==> 覆写修正: SoulClanEntity.java（Merchant & 交易API 兼容 1.21+）"
cp -f "$ENTITY_DIR/SoulClanEntity.java" "$ENTITY_DIR/SoulClanEntity.java.bak" 2>/dev/null || true
cat > "$ENTITY_DIR/SoulClanEntity.java" <<'EOF'
package net.tigereye.chestcavity.soul.entity;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.registration.CCEntities;
import net.tigereye.chestcavity.soul.entity.goal.FollowElderGoal;
import net.tigereye.chestcavity.soul.entity.goal.SplitOnLowHpGoal;
import net.tigereye.chestcavity.soul.util.ChestCavityInsertOps;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;

public class SoulClanEntity extends PathfinderMob implements Merchant {

  public enum Variant { TRADER, GUARD, ELDER }

  private static final double ITEM_RADIUS = 4.0;
  private static final double ITEM_PULL   = 0.35;

  private static final float  SPLIT_HP_THRESHOLD = 10.0f;
  private static final int    SPLIT_CD_TICKS     = 20 * 15;
  private static final int    AREA_MAX_COUNT     = 12;
  private static final double AREA_RADIUS        = 12.0;

  private static final int    GUARD_HEAL_ON_KILL = 4;   // TODO: 之后用事件实现
  private static final double ELDER_MAGNET_DIST  = 32.0;

  private Variant variant = Variant.GUARD;
  private int splitCooldown = 0;
  private UUID elderId = null;

  private MerchantOffers offers;
  private Player tradingPlayer;
  private int merchantXp = 0;

  public SoulClanEntity(net.minecraft.world.entity.EntityType<? extends PathfinderMob> type, Level level) {
    super(type, level);
  }

  public static AttributeSupplier.Builder createAttributes() {
    return Mob.createMobAttributes()
        .add(Attributes.MAX_HEALTH, 24.0)
        .add(Attributes.MOVEMENT_SPEED, 0.28)
        .add(Attributes.ATTACK_DAMAGE, 4.0)
        .add(Attributes.FOLLOW_RANGE, 24.0)
        .add(Attributes.ARMOR, 2.0);
  }

  @Override
  protected void registerGoals() {
    this.goalSelector.addGoal(0, new FloatGoal(this));
    this.goalSelector.addGoal(1, new FollowElderGoal(this, () -> SoulClanManager.findElder(this.level()), ELDER_MAGNET_DIST));
    this.goalSelector.addGoal(2, new SplitOnLowHpGoal(this, SPLIT_HP_THRESHOLD, SPLIT_CD_TICKS, AREA_RADIUS, AREA_MAX_COUNT));
    this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.2, false));
    this.goalSelector.addGoal(4, new RandomStrollGoal(this, 1.0));
    this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));

    this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Monster.class, true));
  }

  @Override
  public void tick() {
    super.tick();
    if (level().isClientSide) return;

    SoulClanManager.onTickRegister(this);
    if (splitCooldown > 0) splitCooldown--;

    // 简易吸物：牵引→贴近后尝试塞入胸腔
    AABB box = new AABB(this.blockPosition()).inflate(ITEM_RADIUS);
    for (ItemEntity item : level().getEntitiesOfClass(ItemEntity.class, box)) {
      if (!item.isAlive()) continue;
      ItemStack stack = item.getItem();
      if (stack.isEmpty()) continue;

      Vec3 dir = new Vec3(this.getX() - item.getX(), this.getY(0.5) - item.getY(), this.getZ() - item.getZ());
      Vec3 push = dir.normalize().scale(ITEM_PULL * 0.1);
      item.setDeltaMovement(item.getDeltaMovement().add(push));

      if (this.distanceTo(item) <= 1.2f) {
        ItemStack remain = ChestCavityInsertOps.tryInsert(this, stack);
        if (remain.isEmpty()) {
          item.discard();
          this.level().playSound(null, this.blockPosition(), SoundEvents.ITEM_PICKUP, this.getSoundSource(), 0.2f, 1.2f);
        } else if (remain.getCount() < stack.getCount()) {
          item.setItem(remain);
          this.level().playSound(null, this.blockPosition(), SoundEvents.ITEM_PICKUP, this.getSoundSource(), 0.2f, 1.0f);
        }
      }
    }

    if (variant != Variant.ELDER) {
      SoulClanManager.findElder(level()).ifPresent(leader -> this.elderId = leader.getUUID());
    }
  }

  @Override
  public void die(DamageSource source) {
    super.die(source);
    if (!level().isClientSide && variant == Variant.ELDER) {
      Entity killer = source.getEntity();
      if (killer instanceof ServerPlayer sp) {
        GuzhenrenResourceBridge.open(sp).ifPresent(handle ->
            handle.adjustDouble("niantou", 20.0, true, "niantou_zhida"));
      }
      SoulClanManager.onElderDead((ServerLevel) level(), this.getUUID());
    }
  }

  public boolean trySplit() {
    if (splitCooldown > 0 || this.getHealth() > SPLIT_HP_THRESHOLD) return false;

    AABB box = new AABB(this.blockPosition()).inflate(AREA_RADIUS);
    int localCount = level().getEntitiesOfClass(SoulClanEntity.class, box, Entity::isAlive).size();
    int cap = Math.max(1, SoulClanManager.getAreaCap((ServerLevel) level(), AREA_MAX_COUNT));
    if (localCount >= cap) return false;

    Variant childVar = pickChildVariant();
    Vec3 pos = this.position().add((random.nextDouble()-0.5)*2.0, 0, (random.nextDouble()-0.5)*2.0);
    SoulClanEntity child = CCEntities.SOUL_CLAN.get().create(level());
    if (child != null) {
      child.moveTo(pos.x, pos.y, pos.z, this.getYRot(), this.getXRot());
      child.setVariant(childVar);
      child.setSplitCooldown(SPLIT_CD_TICKS);
      level().addFreshEntity(child);

      this.hurt(this.damageSources().generic(), 4.0f);
      this.splitCooldown = SPLIT_CD_TICKS;
      this.playSound(SoundEvents.SLIME_JUMP, 0.9f, 1.1f);

      if (childVar == Variant.ELDER) {
        SoulClanManager.tryElectElder((ServerLevel) level(), child);
      }
      return true;
    }
    return false;
  }

  private Variant pickChildVariant() {
    int r = this.random.nextInt(100);
    if (r < 60) return Variant.GUARD;
    if (r < 95) return Variant.TRADER;
    return Variant.ELDER;
  }

  public void setVariant(Variant v) { this.variant = v; }
  public Variant getVariant() { return variant; }
  public void setSplitCooldown(int cd) { this.splitCooldown = cd; }

  // —— 交互：交易（仅交易体） ——
  @Override
  public InteractionResult mobInteract(Player player, InteractionHand hand) {
    if (level().isClientSide) return InteractionResult.sidedSuccess(true);
    if (variant != Variant.TRADER) return InteractionResult.PASS;

    if (offers == null) {
      offers = new MerchantOffers();
      offers.add(new MerchantOffer(
          new ItemCost(Items.COBBLESTONE, 16),
          new ItemStack(Items.BREAD, 4),
          16, 3, 0.05f));
    }
    this.setTradingPlayer(player);
    if (player instanceof ServerPlayer sp) {
      sp.openTradingScreen(this, Component.translatable("entity.chestcavity.soul_clan.trader"));
      return InteractionResult.CONSUME;
    }
    return InteractionResult.PASS;
  }

  // —— Merchant 接口（1.21+） ——
  @Override public void setTradingPlayer(Player player) { this.tradingPlayer = player; }
  @Override public Player getTradingPlayer() { return tradingPlayer; }
  @Override public MerchantOffers getOffers() { return offers == null ? new MerchantOffers() : offers; }
  @Override public void overrideOffers(MerchantOffers offers) { this.offers = offers; }
  @Override public void notifyTrade(MerchantOffer offer) {}
  @Override public void notifyTradeUpdated(ItemStack stack) {}
  @Override public int getVillagerXp() { return merchantXp; }
  @Override public void overrideXp(int xp) { this.merchantXp = xp; }
  @Override public boolean showProgressBar() { return false; }
  @Override public boolean isClientSide() { return this.level().isClientSide; }
  @Override public SoundEvent getNotifyTradeSound() { return SoundEvents.VILLAGER_TRADE; }

  // —— NBT —— //
  private static final String TAG_VARIANT = "Variant";
  private static final String TAG_CD = "SplitCD";
  private static final String TAG_XP = "MerchantXp";

  @Override
  public void addAdditionalSaveData(CompoundTag tag) {
    super.addAdditionalSaveData(tag);
    tag.putString(TAG_VARIANT, variant.name());
    tag.putInt(TAG_CD, splitCooldown);
    tag.putInt(TAG_XP, merchantXp);
  }

  @Override
  public void readAdditionalSaveData(CompoundTag tag) {
    super.readAdditionalSaveData(tag);
    if (tag.contains(TAG_VARIANT)) {
      try { this.variant = Variant.valueOf(tag.getString(TAG_VARIANT)); } catch (Exception ignore) {}
    }
    this.splitCooldown = tag.getInt(TAG_CD);
    if (tag.contains(TAG_XP)) this.merchantXp = tag.getInt(TAG_XP);
  }
}
EOF

echo "==> 覆写修正: SoulClanWorldData.java（SavedData.Factory 两参签名）"
cp -f "$DATA_DIR/SoulClanWorldData.java" "$DATA_DIR/SoulClanWorldData.java.bak" 2>/dev/null || true
cat > "$DATA_DIR/SoulClanWorldData.java" <<'EOF'
package net.tigereye.chestcavity.soul.entity.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public final class SoulClanWorldData extends SavedData {
  private static final String DATA_NAME = "chestcavity_soul_clan";
  private static final String TAG_ELDER = "Elder";
  private static final String TAG_CAP   = "AreaCap";

  private java.util.UUID elderId;
  private int areaCap = 12;

  public static SoulClanWorldData get(ServerLevel level) {
    return level.getDataStorage().computeIfAbsent(
        new SavedData.Factory<>(SoulClanWorldData::new, SoulClanWorldData::load),
        DATA_NAME
    );
  }

  public static SoulClanWorldData load(CompoundTag tag, HolderLookup.Provider registries) {
    SoulClanWorldData d = new SoulClanWorldData();
    if (tag.hasUUID(TAG_ELDER)) d.elderId = tag.getUUID(TAG_ELDER);
    int cap = tag.getInt(TAG_CAP);
    d.areaCap = cap > 0 ? cap : 12;
    return d;
  }

  @Override
  public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
    if (elderId != null) tag.putUUID(TAG_ELDER, elderId);
    tag.putInt(TAG_CAP, areaCap);
    return tag;
  }

  public java.util.UUID getElderId() { return elderId; }
  public void setElderId(java.util.UUID id) { elderId = id; setDirty(); }
  public int getAreaCapOr(int fallback) { return areaCap > 0 ? areaCap : fallback; }
  public void setAreaCap(int value) { areaCap = Math.max(1, value); setDirty(); }
}
EOF

echo "==> 清缓存并构建"
./gradlew --stop >/dev/null 2>&1 || true
rm -rf .gradle/configuration-cache .gradle/spotless >/dev/null 2>&1 || true
./gradlew --no-configuration-cache spotlessApply -x test
./gradlew --no-configuration-cache build -x test

echo "🎉 修补完成。若需全量：./gradlew clean build"

