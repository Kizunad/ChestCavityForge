package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.entity;

import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.guzhenren.XindeItemKeys;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.tuning.HunDaoRuntimeTuning;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge.ResourceHandle;
import net.tigereye.chestcavity.soul.playerghost.PlayerGhostEntity;

/** Hostile world-unique Hun Dao avatar with bespoke growth rules. */
public class HunDaoSoulAvatarWorldBossEntity extends HunDaoSoulAvatarEntity {

  private static final ResourceLocation XINDE_DROP_ID =
      ResourceLocation.parse(XindeItemKeys.XINDE_HUNDAO_ZHUNZONGSHI_WEICANWU);

  private static final TagKey<Item> AUCTION_TAG =
      TagKey.create(
          Registries.ITEM, ResourceLocation.fromNamespaceAndPath("guzhenren", "paimaihang_jiage_1000w"));

  private boolean initializedResources;
  private long nextTeleportTick;

  public HunDaoSoulAvatarWorldBossEntity(
      EntityType<? extends HunDaoSoulAvatarWorldBossEntity> type, Level level) {
    super(type, level);
    this.xpReward = 1000;
  }

  public static AttributeSupplier.Builder createAttributes() {
    return PathfinderMob.createMobAttributes()
        .add(Attributes.MAX_HEALTH, 400.0D)
        .add(Attributes.ATTACK_DAMAGE, 18.0D)
        .add(Attributes.ARMOR, 12.0D)
        .add(Attributes.MOVEMENT_SPEED, 0.32D)
        .add(Attributes.FOLLOW_RANGE, 48.0D);
  }

  @Override
  protected void registerGoals() {
    this.goalSelector.addGoal(0, new FloatGoal(this));
    this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2D, true));
    this.goalSelector.addGoal(4, new RandomStrollGoal(this, 0.7D));
    this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 12.0F));
    this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
    this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    this.targetSelector.addGoal(
        3,
        new NearestAttackableTargetGoal<>(
            this, LivingEntity.class, 5, true, false, this::isValidAdditionalTarget));
  }

  private boolean isValidAdditionalTarget(LivingEntity entity) {
    if (entity == null || entity == this) {
      return false;
    }
    if (!entity.isAlive()) {
      return false;
    }
    if (entity instanceof HunDaoSoulAvatarWorldBossEntity) {
      return false;
    }
    if (entity instanceof PlayerGhostEntity) {
      return false;
    }
    return !isAlliedTo(entity);
  }

  @Override
  public void tick() {
    super.tick();
    if (level().isClientSide) {
      return;
    }
    ensureInitializedResources();
    tickTeleportAbility();
  }

  private void ensureInitializedResources() {
    if (initializedResources) {
      // 保护：若存档缺失导致魂魄为 0，则回填为初始值，避免出生即视为死亡
      if (getResourceState().getMaxHunpo() <= 0.0D) {
        double initial = HunDaoRuntimeTuning.SoulAvatarWorldBoss.INITIAL_HUNPO;
        getResourceState().setHunpoSnapshot(initial, initial);
        ResourceOps.openHandle(this)
            .ifPresent(
                handle -> {
                  handle.writeDouble("hunpo", initial);
                  handle.writeDouble("zuida_hunpo", initial);
                });
        refreshDimensions();
      }
      return;
    }
    initializedResources = true;
    double initial = HunDaoRuntimeTuning.SoulAvatarWorldBoss.INITIAL_HUNPO;
    getResourceState().setHunpoSnapshot(initial, initial);
    ResourceOps.openHandle(this)
        .ifPresent(
            handle -> {
              handle.writeDouble("hunpo", initial);
              handle.writeDouble("zuida_hunpo", initial);
            });
    refreshDimensions();
    nextTeleportTick = level().getGameTime() + 20_000L; // 1000s 延时首跳，避免瞬间离开
    logSpawnState();
  }

  private void tickTeleportAbility() {
    long now = level().getGameTime();
    if (now < nextTeleportTick) {
      return;
    }
    double hunpoThreshold =
        ResourceOps.openHandle(this)
                .map(handle -> handle.getHunpo().orElse(0.0D))
                .orElse(0.0D)
            / 2.0D;
    if (!(hunpoThreshold > 0.0D)) {
      scheduleNextTeleport(now);
      return;
    }
    double radius = HunDaoRuntimeTuning.SoulAvatarWorldBoss.TELEPORT_SCAN_RADIUS;
    AABB area = getBoundingBox().inflate(radius);
    List<LivingEntity> candidates =
        level().getEntitiesOfClass(
            LivingEntity.class,
            area,
            entity -> isValidAdditionalTarget(entity) && entity.getHealth() < hunpoThreshold);
    if (candidates.isEmpty()) {
      scheduleNextTeleport(now);
      return;
    }
    LivingEntity target = candidates.get(random.nextInt(candidates.size()));
    teleportNear(target);
    scheduleNextTeleport(now);
  }

  private void teleportNear(LivingEntity target) {
    Vec3 pos = target.position();
    double offsetX = (random.nextDouble() - 0.5D) * 4.0D;
    double offsetZ = (random.nextDouble() - 0.5D) * 4.0D;
    double newX = pos.x + offsetX;
    double newZ = pos.z + offsetZ;
    int blockX = net.minecraft.util.Mth.floor(newX);
    int blockZ = net.minecraft.util.Mth.floor(newZ);
    int surfaceY =
        level().getHeight(
            net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            blockX,
            blockZ);
    double newY = Math.max(target.getY(), surfaceY);
    teleportTo(newX, newY, newZ);
    setTarget(target);
    logTeleport(target, newX, newY, newZ);
  }

  private void scheduleNextTeleport(long now) {
    nextTeleportTick = now + HunDaoRuntimeTuning.SoulAvatarWorldBoss.TELEPORT_COOLDOWN_TICKS;
  }

  @Override
  public boolean killedEntity(ServerLevel level, LivingEntity victim) {
    boolean result = super.killedEntity(level, victim);
    if (!level.isClientSide) {
      absorbVictim(victim);
    }
    return result;
  }

  private void absorbVictim(LivingEntity victim) {
    double maxGain =
        victim.getMaxHealth() * HunDaoRuntimeTuning.SoulAvatarWorldBoss.MAX_HUNPO_PER_HP;
    double hunpoGain =
        ResourceOps.openHandle(victim)
                .map(handle -> handle.getHunpo().orElse(0.0D))
                .orElse(0.0D)
            * HunDaoRuntimeTuning.SoulAvatarWorldBoss.HUNPO_GAIN_MULTIPLIER;
    ResourceOps.openHandle(this)
        .ifPresent(
            handle -> {
              if (maxGain > 0.0D) {
                handle.adjustDouble("zuida_hunpo", maxGain, true);
              }
              if (hunpoGain > 0.0D) {
                handle.adjustHunpo(hunpoGain, true);
              }
              double current = handle.getHunpo().orElse(0.0D);
              double maxHunpo = handle.getMaxHunpo().orElse(0.0D);
              getResourceState().setHunpoSnapshot(current, maxHunpo);
              refreshDimensions();
            });
  }

  @Override
  public void die(DamageSource source) {
    if (!level().isClientSide) {
      handleDeathRewards(source);
    }
    super.die(source);
  }

  private void handleDeathRewards(DamageSource source) {
    dropXindeLoot();
    LivingEntity killer = resolveTrueKiller(source);
    if (killer == null) {
      return;
    }
    grantKillerBonuses(killer);
    giveRandomAuctionItem(killer);
    ChestCavity.LOGGER.info(
        "[hun_dao][boss] died at ({}, {}, {}), killer={} source={}",
        getX(),
        getY(),
        getZ(),
        killer,
        source == null ? "unknown" : source.getMsgId());
  }

  private void dropXindeLoot() {
    if (!(level() instanceof ServerLevel serverLevel)) {
      return;
    }
    Optional<Item> item =
        Optional.ofNullable(serverLevel.registryAccess().registryOrThrow(Registries.ITEM).get(XINDE_DROP_ID));
    if (item.isEmpty()) {
      return;
    }
    int count = 1 + random.nextInt(5);
    ItemStack stack = new ItemStack(item.get(), count);
    spawnAtLocation(stack, 0.0F);
  }

  private void grantKillerBonuses(LivingEntity killer) {
    double bonusMax = 1000.0D + getResourceState().getMaxHunpo();
    double carryOver = getResourceState().getMaxHunpo();
    ResourceOps.openHandle(killer)
        .ifPresent(
            handle -> {
              if (bonusMax > 0.0D) {
                handle.adjustDouble("zuida_hunpo", bonusMax, true);
              }
              if (carryOver > 0.0D) {
                handle.adjustHunpo(carryOver, true);
              }
            });
  }

  private void giveRandomAuctionItem(LivingEntity killer) {
    ItemStack stack = rollAuctionItem();
    if (stack.isEmpty()) {
      return;
    }
    if (killer instanceof Player player) {
      boolean added = player.addItem(stack);
      if (!added) {
        player.drop(stack, false);
      }
      return;
    }
    spawnItemStackNear(killer, stack);
  }

  private ItemStack rollAuctionItem() {
    if (!(level() instanceof ServerLevel serverLevel)) {
      return ItemStack.EMPTY;
    }
    Registry<Item> registry = serverLevel.registryAccess().registryOrThrow(Registries.ITEM);
    Optional<HolderSet.Named<Item>> holderSet = registry.getTag(AUCTION_TAG);
    if (holderSet.isEmpty() || holderSet.get().size() == 0) {
      return ItemStack.EMPTY;
    }
    Holder<Item> holder = holderSet.get().getRandomElement(serverLevel.random).orElse(null);
    if (holder == null) {
      return ItemStack.EMPTY;
    }
    return new ItemStack(holder.value());
  }

  private void spawnItemStackNear(LivingEntity entity, ItemStack stack) {
    if (stack.isEmpty() || !(level() instanceof ServerLevel serverLevel)) {
      return;
    }
    ItemEntity drop = new ItemEntity(serverLevel, entity.getX(), entity.getY() + 0.5D, entity.getZ(), stack);
    drop.setDefaultPickUpDelay();
    serverLevel.addFreshEntity(drop);
  }

  @Nullable
  private LivingEntity resolveTrueKiller(DamageSource source) {
    if (source == null) {
      return null;
    }
    return resolveLivingEntity(source.getEntity());
  }

  @Nullable
  private LivingEntity resolveLivingEntity(@Nullable Entity source) {
    if (source == null) {
      return null;
    }
    if (source instanceof LivingEntity living) {
      return living;
    }
    if (source instanceof Projectile projectile) {
      return resolveLivingEntity(projectile.getOwner());
    }
    if (source instanceof OwnableEntity ownable) {
      return resolveLivingEntity(ownable.getOwner());
    }
    return null;
  }

  @Override
  public void addAdditionalSaveData(CompoundTag tag) {
    super.addAdditionalSaveData(tag);
    tag.putBoolean("HunDaoWorldBossInit", initializedResources);
    tag.putLong("HunDaoWorldBossNextTeleport", nextTeleportTick);
  }

  @Override
  public void readAdditionalSaveData(CompoundTag tag) {
    super.readAdditionalSaveData(tag);
    initializedResources = tag.getBoolean("HunDaoWorldBossInit");
    nextTeleportTick = tag.getLong("HunDaoWorldBossNextTeleport");
  }

  @Override
  public int getLifetimeTicks() {
    // 世界 Boss 永不因寿命移除
    return -1;
  }

  @Override
  public void setLifetimeTicks(int ticks) {
    // no-op: 世界 Boss 的存活时间固定为永久
  }

  @Override
  public void remove(Entity.RemovalReason reason) {
    if (!level().isClientSide) {
      if (reason == Entity.RemovalReason.DISCARDED) {
        ChestCavity.LOGGER.warn(
            "[hun_dao][boss] removed: reason={} at ({}, {}, {}); trace=\n{}",
            reason,
            getX(),
            getY(),
            getZ(),
            org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace(new Exception("removed")));
      } else {
        ChestCavity.LOGGER.info(
            "[hun_dao][boss] removed: reason={} at ({}, {}, {})", reason, getX(), getY(), getZ());
      }
    }
    super.remove(reason);
  }

  private void logSpawnState() {
    ChestCavity.LOGGER.info(
        "[hun_dao][boss] spawned at ({}, {}, {}), nextTeleport={}",
        getX(),
        getY(),
        getZ(),
        nextTeleportTick);
  }

  private void logTeleport(LivingEntity target, double x, double y, double z) {
    ChestCavity.LOGGER.info(
        "[hun_dao][boss] teleport to ({}, {}, {}) target={} hp={}",
        x,
        y,
        z,
        target,
        target == null ? 0.0D : target.getHealth());
  }
}
