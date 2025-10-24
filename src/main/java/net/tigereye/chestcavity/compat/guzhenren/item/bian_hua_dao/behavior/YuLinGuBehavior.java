package net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.behavior;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.entity.summon.OwnedSharkEntity;
import net.tigereye.chestcavity.compat.guzhenren.item.common.AbstractGuzhenrenOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.tigereye.chestcavity.interfaces.ChestCavityEntity;
import net.tigereye.chestcavity.listeners.OrganIncomingDamageListener;
import net.tigereye.chestcavity.listeners.OrganOnHitListener;
import net.tigereye.chestcavity.listeners.OrganRemovalContext;
import net.tigereye.chestcavity.listeners.OrganRemovalListener;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.util.NetworkUtil;
import org.slf4j.Logger;

/**
 * 鱼鳞蛊行为实现，负责维持“潮生鳞护”与三项主动技能的状态支撑。
 *
 * <p>实现目标：
 *
 * <ul>
 *   <li>处理鱼甲/鲨牙甲的凝聚与维持，包括饱食消耗与潮湿缓冲；
 *   <li>为主动技能提供查询接口（湿润、甲胄、召唤记录等）；
 *   <li>跟踪鱼甲进度、召唤的鲨鱼数量以及升级节奏。
 * </ul>
 */
public final class YuLinGuBehavior extends AbstractGuzhenrenOrganBehavior
    implements OrganSlowTickListener,
        OrganOnHitListener,
        OrganIncomingDamageListener,
        OrganRemovalListener {

  public static final YuLinGuBehavior INSTANCE = new YuLinGuBehavior();

  private static final Logger LOGGER = LogUtils.getLogger();

  private static final String MOD_ID = "guzhenren";
  private static final ResourceLocation ORGAN_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "yu_lin_gu");
  private static final ResourceLocation SHUI_JIA_GU_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "shui_jia_gu");
  private static final ResourceLocation JIAO_WEI_GU_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "jiao_wei_gu");

  /** 维持鱼甲的最大进度。 */
  private static final int FISH_ARMOR_MAX_PROGRESS = 10;

  /** 鲨牙甲触发所需的进度。 */
  private static final int SHARK_ARMOR_THRESHOLD = 10;

  /** 潮湿缓冲时长（秒） -> 游戏刻。 */
  private static final int WET_BUFFER_TICKS = 20 * 4;

  /** 潮生鳞护饱食维持：每 4 秒 1 点。 */
  private static final double HUNGER_COST_PER_SECOND = 1.0 / 4.0;

  /** 水下保命回复冷却（基础/最终）。 */
  private static final int WATER_HEAL_COOLDOWN_TICKS = 20 * 8;

  private static final int WATER_HEAL_COOLDOWN_FINAL_TICKS = 20 * 12;

  /** 召唤物上限。 */
  private static final int MAX_SUMMONS = 5;

  private static final String STATE_ROOT = "YuLinGu";
  private static final String PROGRESS_KEY = "FishArmorProgress";
  private static final String HAS_FISH_ARMOR_KEY = "HasFishArmor";
  private static final String HAS_SHARK_ARMOR_KEY = "HasSharkArmor";
  private static final String SHARK_TIER_UNLOCKED_KEY = "SharkTierUnlocked";
  private static final String HUNGER_PROGRESS_KEY = "HungerDebt";
  private static final String LAST_WET_TICK_KEY = "LastWetTick";
  private static final String WATER_HEAL_READY_AT_KEY = "WaterHealReadyAt";
  private static final String SUMMON_SEQUENCE_KEY = "SummonSequence";
  private static final String ACTIVE_SUMMONS_KEY = "ActiveSummons";

  private static final ResourceLocation WATER_HEAL_COOLDOWN_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "cooldowns/yu_lin_gu_water_heal");

  private YuLinGuBehavior() {}

  @Override
  public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
    if (!matchesOrgan(organ, ORGAN_ID) || entity == null || entity.level().isClientSide()) {
      return;
    }
    if (!(entity instanceof Player player)) {
      return;
    }
    if (cc == null) {
      return;
    }

    Level level = player.level();
    long gameTime = level.getGameTime();
    OrganState state = organState(organ, STATE_ROOT);

    boolean moistNow = player.isInWaterRainOrBubble();
    if (moistNow) {
      state.setLong(LAST_WET_TICK_KEY, gameTime);
    }

    boolean hasFishArmor = state.getBoolean(HAS_FISH_ARMOR_KEY, false);
    boolean hasSharkArmor = state.getBoolean(HAS_SHARK_ARMOR_KEY, false);
    int progress = Mth.clamp(state.getInt(PROGRESS_KEY, 0), 0, FISH_ARMOR_MAX_PROGRESS);

    if (!hasFishArmor && progress >= FISH_ARMOR_MAX_PROGRESS) {
      hasFishArmor = true;
      state.setBoolean(HAS_FISH_ARMOR_KEY, true);
      NetworkUtil.sendOrganSlotUpdate(cc, organ);
      level.playSound(
          null,
          player.blockPosition(),
          SoundEvents.TURTLE_EGG_CRACK,
          SoundSource.PLAYERS,
          0.7f,
          1.2f);
    }

    if (hasFishArmor) {
      double hungerDebt = Math.max(0.0, state.getDouble(HUNGER_PROGRESS_KEY, 0.0));
      hungerDebt += HUNGER_COST_PER_SECOND;
      int hungerToConsume = (int) hungerDebt;
      if (hungerToConsume > 0) {
        hungerDebt -= hungerToConsume;
        drainHunger(player, hungerToConsume);
      }
      state.setDouble(HUNGER_PROGRESS_KEY, hungerDebt);

      applyArmorBuffs(player, hasSharkArmor);

      if (!isPlayerMoist(player, state, gameTime)) {
        hasFishArmor = false;
        state.setBoolean(HAS_FISH_ARMOR_KEY, false);
        state.setBoolean(HAS_SHARK_ARMOR_KEY, false);
        progress = Math.max(0, progress - 2);
        state.setInt(PROGRESS_KEY, progress);
        NetworkUtil.sendOrganSlotUpdate(cc, organ);
        level.playSound(
            null, player.blockPosition(), SoundEvents.FISH_SWIM, SoundSource.PLAYERS, 0.5f, 0.8f);
      }
    }

    handleWaterHeal(player, cc, organ, state, hasSharkArmor, gameTime);
    if (level instanceof ServerLevel serverLevel) {
      tickSummons(serverLevel, player, gameTime);
    }
  }

  @Override
  public float onHit(
      DamageSource source,
      LivingEntity attacker,
      LivingEntity target,
      ChestCavityInstance cc,
      ItemStack organ,
      float damage) {
    if (!matchesOrgan(organ, ORGAN_ID)) {
      return damage;
    }
    if (!(attacker instanceof Player player)) {
      return damage;
    }
    if (target == null || !target.isAlive()) {
      return damage;
    }
    OrganState state = organState(organ, STATE_ROOT);
    boolean hasFishArmor = state.getBoolean(HAS_FISH_ARMOR_KEY, false);
    if (hasFishArmor) {
      grantProgress(player, cc, organ, state, 1);
    }
    recordWetContact(player, organ);
    return damage;
  }

  @Override
  public float onIncomingDamage(
      DamageSource source,
      LivingEntity victim,
      ChestCavityInstance cc,
      ItemStack organ,
      float damage) {
    if (!matchesOrgan(organ, ORGAN_ID)) {
      return damage;
    }
    if (!(victim instanceof Player player) || damage <= 0.0f) {
      return damage;
    }
    OrganState state = organState(organ, STATE_ROOT);
    if (state.getBoolean(HAS_FISH_ARMOR_KEY, false)) {
      int bonus = Mth.clamp((int) Math.floor(damage / 4.0f), 0, 2);
      if (bonus > 0) {
        grantProgress(player, cc, organ, state, bonus);
      }
    }
    return damage;
  }

  @Override
  public void onRemoved(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
    if (!matchesOrgan(organ, ORGAN_ID)) {
      return;
    }
    OrganState state = organState(organ, STATE_ROOT);

    if (!entity.level().isClientSide()) {
      List<OwnedSharkEntity> summons = loadSummonsFromState(state);
      if (!summons.isEmpty()) {
        LOGGER.info(
            "YuLinGuBehavior: Discarding {} summoned entities for player {}.",
            summons.size(),
            entity.getName().getString());
        for (OwnedSharkEntity summon : summons) {
          summon.discard(entity.level());
        }
      }
    }

    state.setBoolean(HAS_FISH_ARMOR_KEY, false);
    state.setBoolean(HAS_SHARK_ARMOR_KEY, false);
    state.setDouble(HUNGER_PROGRESS_KEY, 0.0);
    state.setInt(PROGRESS_KEY, 0);
    // 清理召唤物列表
    saveSummonsToState(state, new ArrayList<>());
  }

  public void onEquip(
      ChestCavityInstance cc, ItemStack organ, List<OrganRemovalContext> staleRemovalContexts) {
    if (!matchesOrgan(organ, ORGAN_ID) || cc == null) {
      return;
    }
    registerRemovalHook(cc, organ, this, staleRemovalContexts);
  }

  public void ensureAttached(ChestCavityInstance cc) {
    // 目前无额外的账本或属性需求，此方法占位以满足注册规范。
  }

  /** 玩家接触到水或雨时调用，刷新潮湿缓冲。 */
  public void recordWetContact(Player player, ItemStack organ) {
    if (player == null || organ == null || organ.isEmpty()) {
      return;
    }
    OrganState state = organState(organ, STATE_ROOT);
    state.setLong(LAST_WET_TICK_KEY, player.level().getGameTime());
  }

  public boolean hasFishArmor(ItemStack organ) {
    return organState(organ, STATE_ROOT).getBoolean(HAS_FISH_ARMOR_KEY, false);
  }

  public boolean hasSharkArmor(ItemStack organ) {
    return organState(organ, STATE_ROOT).getBoolean(HAS_SHARK_ARMOR_KEY, false);
  }

  public int unlockedSharkTier(ItemStack organ) {
    return Math.max(1, organState(organ, STATE_ROOT).getInt(SHARK_TIER_UNLOCKED_KEY, 1));
  }

  public void unlockSharkTier(ItemStack organ, int tier) {
    if (tier <= 0) {
      return;
    }
    OrganState state = organState(organ, STATE_ROOT);
    int previous = Math.max(1, state.getInt(SHARK_TIER_UNLOCKED_KEY, 1));
    if (tier > previous) {
      state.setInt(SHARK_TIER_UNLOCKED_KEY, tier);
      state.setBoolean(HAS_SHARK_ARMOR_KEY, true);
    }
  }

  public void addProgress(Player player, ChestCavityInstance cc, ItemStack organ, int amount) {
    if (organ == null || organ.isEmpty()) {
      return;
    }
    OrganState state = organState(organ, STATE_ROOT);
    grantProgress(player, cc, organ, state, amount);
  }

  public int nextSummonSequence(ItemStack organ) {
    OrganState state = organState(organ, STATE_ROOT);
    int sequence = Math.max(0, state.getInt(SUMMON_SEQUENCE_KEY, 0)) + 1;
    state.setInt(SUMMON_SEQUENCE_KEY, sequence);
    return sequence;
  }

  public List<OwnedSharkEntity> getSummons(Player owner) {
    if (owner == null) {
      return List.of();
    }
    ItemStack organ = findYuLinGuOrgan(owner);
    if (organ.isEmpty()) {
      return List.of();
    }
    OrganState state = organState(organ, STATE_ROOT);
    return loadSummonsFromState(state);
  }

  public void addSummon(Player owner, OwnedSharkEntity summon) {
    if (owner == null || summon == null) {
      return;
    }
    synchronized (owner) {
      ItemStack organ = findYuLinGuOrgan(owner);
      if (organ.isEmpty()) {
        return;
      }
      OrganState state = organState(organ, STATE_ROOT);
      List<OwnedSharkEntity> summons = loadSummonsFromState(state);
      summons.add(summon);
      saveSummonsToState(state, summons);

      ChestCavityInstance cc =
          ChestCavityEntity.of(owner).map(ChestCavityEntity::getChestCavityInstance).orElse(null);
      if (cc != null) {
        NetworkUtil.sendOrganSlotUpdate(cc, organ);
      }
    }
  }

  public void removeSummon(Player owner, OwnedSharkEntity summon) {
    if (owner == null || summon == null) {
      return;
    }
    synchronized (owner) {
      ItemStack organ = findYuLinGuOrgan(owner);
      if (organ.isEmpty()) {
        return;
      }
      OrganState state = organState(organ, STATE_ROOT);
      List<OwnedSharkEntity> summons = loadSummonsFromState(state);
      summons.removeIf(candidate -> Objects.equals(candidate.entityId(), summon.entityId()));
      saveSummonsToState(state, summons);

      ChestCavityInstance cc =
          ChestCavityEntity.of(owner).map(ChestCavityEntity::getChestCavityInstance).orElse(null);
      if (cc != null) {
        NetworkUtil.sendOrganSlotUpdate(cc, organ);
      }
    }
  }

  public boolean isPlayerMoist(Player player) {
    if (player == null) {
      return false;
    }
    OrganState state = organState(player.getMainHandItem(), STATE_ROOT);
    return isPlayerMoist(player, state, player.level().getGameTime());
  }

  public boolean isPlayerMoist(Player player, ItemStack organ) {
    if (player == null || organ == null || organ.isEmpty()) {
      return false;
    }
    return isPlayerMoist(player, organState(organ, STATE_ROOT), player.level().getGameTime());
  }

  private boolean isPlayerMoist(Player player, OrganState state, long gameTime) {
    long lastWet = state.getLong(LAST_WET_TICK_KEY, gameTime);
    if (player.isInWaterRainOrBubble()) {
      return true;
    }
    return gameTime - lastWet <= WET_BUFFER_TICKS;
  }

  private void grantProgress(
      Player player, ChestCavityInstance cc, ItemStack organ, OrganState state, int amount) {
    if (player == null || cc == null || organ == null || organ.isEmpty() || amount <= 0) {
      return;
    }
    int previous = Mth.clamp(state.getInt(PROGRESS_KEY, 0), 0, FISH_ARMOR_MAX_PROGRESS);
    int updated = Mth.clamp(previous + amount, 0, FISH_ARMOR_MAX_PROGRESS);
    if (updated != previous) {
      state.setInt(PROGRESS_KEY, updated);
      if (updated >= SHARK_ARMOR_THRESHOLD) {
        state.setBoolean(HAS_FISH_ARMOR_KEY, true);
      }
      NetworkUtil.sendOrganSlotUpdate(cc, organ);
    }
  }

  private void handleWaterHeal(
      Player player,
      ChestCavityInstance cc,
      ItemStack organ,
      OrganState state,
      boolean hasSharkArmor,
      long gameTime) {
    if (!player.isInWaterOrBubble()) {
      return;
    }
    float health = player.getHealth();
    float maxHealth = player.getMaxHealth();
    if (health >= maxHealth * 0.3f) {
      return;
    }
    MultiCooldown cooldown = MultiCooldown.builder(state).withSync(cc, organ).build();
    MultiCooldown.Entry entry =
        cooldown
            .entry(WATER_HEAL_READY_AT_KEY)
            .withClamp(value -> Math.max(0L, value))
            .withDefault(0L);
    int cooldownTicks = hasSharkArmor ? WATER_HEAL_COOLDOWN_FINAL_TICKS : WATER_HEAL_COOLDOWN_TICKS;
    if (entry.isReady(gameTime)) {
      float healAmount = hasSharkArmor ? 3.0f : 2.0f;
      player.heal(healAmount);
      entry.setReadyAt(gameTime + cooldownTicks);
      NetworkUtil.sendOrganSlotUpdate(cc, organ);
      if (player.level() instanceof ServerLevel serverLevel) {
        serverLevel.playSound(
            null,
            player.blockPosition(),
            SoundEvents.DOLPHIN_SPLASH,
            SoundSource.PLAYERS,
            0.6f,
            1.0f);
      }
    }
  }

  private void applyArmorBuffs(Player player, boolean hasSharkArmor) {
    int graceDuration = 60;
    int graceAmplifier = hasSharkArmor ? 1 : 0;
    player.addEffect(
        new MobEffectInstance(
            MobEffects.DOLPHINS_GRACE, graceDuration, graceAmplifier, true, false));
    player.addEffect(
        new MobEffectInstance(MobEffects.WATER_BREATHING, graceDuration, 0, true, false));
    if (hasSharkArmor) {
      player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 0, true, false));
    }
  }

  private void drainHunger(Player player, int amount) {
    if (amount <= 0) {
      return;
    }
    FoodData stats = player.getFoodData();
    stats.setFoodLevel(Math.max(0, stats.getFoodLevel() - amount));
  }

  private void tickSummons(ServerLevel level, Player owner, long gameTime) {
    if (level == null || owner == null) {
      return;
    }
    synchronized (owner) {
      ItemStack organ = findYuLinGuOrgan(owner);
      if (organ.isEmpty()) {
        return;
      }
      OrganState state = organState(organ, STATE_ROOT);
      List<OwnedSharkEntity> summons = loadSummonsFromState(state);
      if (summons.isEmpty()) {
        return;
      }
      List<OwnedSharkEntity> updated = new ArrayList<>(summons.size());
      for (OwnedSharkEntity summon : summons) {
        if (summon == null) {
          continue;
        }
        if (!summon.tick(level, owner, gameTime)) {
          continue;
        }
        updated.add(summon);
      }
      if (updated.size() != summons.size()) {
        saveSummonsToState(state, updated);
        ChestCavityInstance cc =
            ChestCavityEntity.of(owner).map(ChestCavityEntity::getChestCavityInstance).orElse(null);
        if (cc != null) {
          NetworkUtil.sendOrganSlotUpdate(cc, organ);
        }
      }
    }
  }

  public int countSummons(Player owner) {
    if (owner == null) {
      return 0;
    }
    ItemStack organ = findYuLinGuOrgan(owner);
    if (organ.isEmpty()) {
      return 0;
    }
    OrganState state = organState(organ, STATE_ROOT);
    return loadSummonsFromState(state).size();
  }

  public void pruneToLimit(Player owner) {
    if (owner == null) {
      return;
    }
    synchronized (owner) {
      ItemStack organ = findYuLinGuOrgan(owner);
      if (organ.isEmpty()) {
        return;
      }
      OrganState state = organState(organ, STATE_ROOT);
      List<OwnedSharkEntity> summons = loadSummonsFromState(state);
      if (summons.size() <= MAX_SUMMONS) {
        return;
      }
      List<OwnedSharkEntity> sorted = new ArrayList<>(summons);
      sorted.sort((a, b) -> Long.compare(a.createdAt(), b.createdAt()));
      while (sorted.size() > MAX_SUMMONS) {
        OwnedSharkEntity removed = sorted.remove(0);
        removed.discard(owner.level());
      }
      saveSummonsToState(state, sorted);

      ChestCavityInstance cc =
          ChestCavityEntity.of(owner).map(ChestCavityEntity::getChestCavityInstance).orElse(null);
      if (cc != null) {
        NetworkUtil.sendOrganSlotUpdate(cc, organ);
      }
    }
  }

  public boolean hasOrgan(ChestCavityInstance cc, ResourceLocation organId) {
    if (cc == null || organId == null || cc.inventory == null) {
      return false;
    }
    int size = cc.inventory.getContainerSize();
    for (int i = 0; i < size; i++) {
      ItemStack stack = cc.inventory.getItem(i);
      if (stack.isEmpty()) {
        continue;
      }
      Item item = stack.getItem();
      ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
      if (organId.equals(id)) {
        return true;
      }
    }
    return false;
  }

  public boolean hasTailSynergy(ChestCavityInstance cc) {
    return hasOrgan(cc, JIAO_WEI_GU_ID);
  }

  public boolean hasWaterArmorSynergy(ChestCavityInstance cc) {
    return hasOrgan(cc, SHUI_JIA_GU_ID);
  }

  /** 从器官状态中读取召唤物列表。 */
  private List<OwnedSharkEntity> loadSummonsFromState(OrganState state) {
    if (state == null) {
      return new ArrayList<>();
    }
    ListTag listTag = state.getList(ACTIVE_SUMMONS_KEY, Tag.TAG_COMPOUND);
    List<OwnedSharkEntity> summons = new ArrayList<>(listTag.size());
    for (int i = 0; i < listTag.size(); i++) {
      CompoundTag summonTag = listTag.getCompound(i);
      try {
        UUID entityId = summonTag.getUUID("EntityId");
        UUID ownerId = summonTag.getUUID("OwnerId");
        int tier = summonTag.getInt("Tier");
        long createdAt = summonTag.getLong("CreatedAt");
        long expiresAt = summonTag.getLong("ExpiresAt");
        summons.add(new OwnedSharkEntity(entityId, ownerId, tier, createdAt, expiresAt));
      } catch (Exception e) {
        LOGGER.warn("Failed to load summon from state", e);
      }
    }
    return summons;
  }

  /** 将召唤物列表保存到器官状态中。 */
  private void saveSummonsToState(OrganState state, List<OwnedSharkEntity> summons) {
    if (state == null) {
      return;
    }
    ListTag listTag = new ListTag();
    for (OwnedSharkEntity summon : summons) {
      CompoundTag summonTag = new CompoundTag();
      summonTag.putUUID("EntityId", summon.entityId());
      summonTag.putUUID("OwnerId", summon.ownerId());
      summonTag.putInt("Tier", summon.tier());
      summonTag.putLong("CreatedAt", summon.createdAt());
      summonTag.putLong("ExpiresAt", summon.expiresAt());
      listTag.add(summonTag);
    }
    state.setList(ACTIVE_SUMMONS_KEY, listTag);
  }

  /** 查找玩家的鱼鳞蛊器官。 */
  private ItemStack findYuLinGuOrgan(Player player) {
    if (player == null) {
      return ItemStack.EMPTY;
    }
    ChestCavityInstance cc =
        ChestCavityEntity.of(player).map(ChestCavityEntity::getChestCavityInstance).orElse(null);
    if (cc == null || cc.inventory == null) {
      return ItemStack.EMPTY;
    }
    int size = cc.inventory.getContainerSize();
    for (int i = 0; i < size; i++) {
      ItemStack stack = cc.inventory.getItem(i);
      if (matchesOrgan(stack, ORGAN_ID)) {
        return stack;
      }
    }
    return ItemStack.EMPTY;
  }
}
