package net.tigereye.chestcavity.compat.guzhenren.item.mu_dao.behavior;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.IntUnaryOperator;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.AbstractGuzhenrenOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge.ResourceHandle;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.listeners.OrganIncomingDamageListener;
import net.tigereye.chestcavity.listeners.OrganRemovalListener;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.skill.ActiveSkillRegistry;
import net.tigereye.chestcavity.util.AbsorptionHelper;
import net.tigereye.chestcavity.util.NetworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 草裙蛊（木道）完整行为实现。
 *
 * <p>功能总览：
 *
 * <ul>
 *   <li>支持 12 个阶段（2-I → 5-III）基于「草裙纹点（SP）」的成长体系，完全依赖战斗/治疗 事件推进；
 *   <li>提供四个主动技（护幕、回春、替身、庇佑）与四个被动（静养、森风、木纹、青藤），会 随阶段强化；
 *   <li>管理真元/精力/魂魄/念头/饱食/生命等资源消耗，并处理护幕吸收、减伤和友方治疗；
 *   <li>内建详细注释，便于后续扩展。
 * </ul>
 */
public final class CaoQunGuOrganBehavior extends AbstractGuzhenrenOrganBehavior
    implements OrganSlowTickListener, OrganIncomingDamageListener, OrganRemovalListener {

  public static final CaoQunGuOrganBehavior INSTANCE = new CaoQunGuOrganBehavior();

  private static final Logger LOGGER = LoggerFactory.getLogger(CaoQunGuOrganBehavior.class);

  private static final String MOD_ID = "guzhenren";
  private static final ResourceLocation ORGAN_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "caoqungu");

  public static final ResourceLocation ABILITY_A1_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "caoqungu_a1");
  public static final ResourceLocation ABILITY_A2_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "caoqungu_a2");
  public static final ResourceLocation ABILITY_A3_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "caoqungu_a3");
  public static final ResourceLocation ABILITY_A4_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "caoqungu_a4");

  private static final ResourceLocation ABSORPTION_MODIFIER_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "caoqungu_absorption");

  private static final double BASE_A1_ABSORPTION_FLAT = 40.0D;
  private static final double BASE_A1_ABSORPTION_MAX_HEALTH_RATIO = 0.08D;
  private static final int BASE_A1_DURATION_TICKS = 10 * 20;
  private static final int BASE_A1_COOLDOWN_TICKS = 16 * 20;
  private static final double A1_REFLECT_DAMAGE = 10.0D;
  private static final int A1_REFLECT_INTERVAL_TICKS = 10; // 0.5s

  private static final double BASE_A2_HEAL_FLAT = 6.0D;
  private static final double BASE_A2_HEAL_RATIO = 0.02D;
  private static final int BASE_A2_TICK_INTERVAL = 40; // 2s
  private static final int BASE_A2_DURATION_TICKS = 8 * 20;
  private static final int BASE_A2_TARGETS = 3;
  private static final int BASE_A2_RADIUS = 6;
  private static final int BASE_A2_COOLDOWN_TICKS = 18 * 20;

  private static final double BASE_A3_DAMAGE_REDUCTION = 0.60D;
  private static final double BASE_A3_HEAL_RATIO = 0.50D;
  private static final double BASE_A3_HEAL_CAP_RATIO = 0.20D;
  private static final int BASE_A3_DURATION_TICKS = 5 * 20;
  private static final int BASE_A3_COOLDOWN_TICKS = 24 * 20;

  private static final double BASE_A4_DAMAGE_REDUCTION = 0.35D;
  private static final double BASE_A4_FRONT_BLOCK = 0.30D;
  private static final int BASE_A4_DURATION_TICKS = 3 * 20;
  private static final int BASE_A4_REGEN_DURATION_TICKS = 2 * 20;
  private static final int BASE_A4_COOLDOWN_TICKS = 22 * 20;
  private static final double A4_REGEN_PER_SECOND = 2.0D;

  private static final double P1_HEAL_PER_TICK = 2.0D;
  private static final int P1_STILL_THRESHOLD_TICKS = 40;
  private static final int P1_HEAL_INTERVAL_TICKS = 3 * 20;
  private static final int P1_COST_INTERVAL_TICKS = 6 * 20;
  private static final double P1_BASE_ZHENYUAN_COST = 10.0D;
  private static final double P1_BASE_JINGLI_COST = 1.0D;
  private static final double P1_BASE_HUNPO_COST = 0.0D;

  private static final int P2_TICK_INTERVAL_TICKS = 5 * 20;
  private static final double P2_ZHENYUAN_RESTORE = 1.0D;
  private static final double P2_JINGLI_RESTORE = 0.5D;

  private static final int P3_TICK_INTERVAL_TICKS = 2 * 20;
  private static final double P3_HEAL_PER_TICK = 1.0D;

  private static final int P4_COOLDOWN_TICKS = 120 * 20;
  private static final double P4_HEAL_FLAT = 10.0D;
  private static final double P4_HEAL_RATIO = 0.05D;
  private static final int P4_REGEN_DURATION_TICKS = 4 * 20;

  private static final double MIN_HORIZONTAL_DOT_FOR_FRONT_BLOCK = 0.35D;

  private static final double EPSILON = 1.0E-4D;

  private static final String STATE_ROOT = "CaoQunGu";

  private static final String KEY_STAGE = "Stage";
  private static final String KEY_STAGE_TOTAL = "StageTotal";
  private static final String KEY_STAGE_GENERAL = "StageGeneral";
  private static final String KEY_STAGE_DEFENSE = "StageDefense";
  private static final String KEY_STAGE_HEALING = "StageHealing";
  private static final String KEY_STAGE_DEFENSE_STEADY = "StageDefenseSteady";
  private static final String KEY_STAGE_BLOCK = "StageBlock";
  private static final String KEY_STAGE_COOP = "StageCoop";

  private static final String KEY_PRE_TOTAL = "NextStagePreTotal";
  private static final String KEY_PRE_GENERAL = "NextStagePreGeneral";
  private static final String KEY_PRE_DEFENSE = "NextStagePreDefense";
  private static final String KEY_PRE_HEALING = "NextStagePreHealing";
  private static final String KEY_PRE_DEFENSE_STEADY = "NextStagePreDefenseSteady";
  private static final String KEY_PRE_BLOCK = "NextStagePreBlock";
  private static final String KEY_PRE_COOP = "NextStagePreCoop";

  private static final String KEY_LAST_POS_X = "LastPosX";
  private static final String KEY_LAST_POS_Y = "LastPosY";
  private static final String KEY_LAST_POS_Z = "LastPosZ";
  private static final String KEY_STILL_TICKS = "StillTicks";
  private static final String KEY_P1_HEAL_ACCUM = "P1HealAccum";
  private static final String KEY_P1_COST_ACCUM = "P1CostAccum";

  private static final String KEY_P2_TIMER = "P2Timer";
  private static final String KEY_P3_TIMER = "P3Timer";
  private static final String KEY_P4_READY_AT = "P4ReadyAt";

  private static final String KEY_A1_ACTIVE_UNTIL = "A1ActiveUntil";
  private static final String KEY_A1_SHIELD_VALUE = "A1ShieldValue";
  private static final String KEY_A1_REFLECT_STACKS = "A1ReflectStacks";

  private static final String KEY_A2_ACTIVE_UNTIL = "A2ActiveUntil";
  private static final String KEY_A2_LAST_HEAL_TICK = "A2LastHealTick";

  private static final String KEY_A3_ACTIVE_UNTIL = "A3ActiveUntil";
  private static final String KEY_A3_ABSORBED = "A3Absorbed";
  private static final String KEY_A3_HEAL_PENDING = "A3HealPending";

  private static final String KEY_A4_ACTIVE_UNTIL = "A4ActiveUntil";
  private static final String KEY_A4_REGEN_UNTIL = "A4RegenUntil";

  private static final String KEY_COOLDOWN_READY_A1 = "CooldownA1";
  private static final String KEY_COOLDOWN_READY_A2 = "CooldownA2";
  private static final String KEY_COOLDOWN_READY_A3 = "CooldownA3";
  private static final String KEY_COOLDOWN_READY_A4 = "CooldownA4";
  private static final String KEY_COOLDOWN_REFLECT_PREFIX = "Reflect:";

  private static final double HUNGER_RESERVE_EPSILON = 0.5D;

  private static final Set<TagKey<Biome>> WOODLAND_BIOME_TAGS =
      Set.of(BiomeTags.IS_FOREST, BiomeTags.IS_TAIGA, BiomeTags.HAS_VILLAGE_PLAINS);

  private static final Map<StageData, Component> STAGE_MESSAGES = buildStageMessages();

  static {
    OrganActivationListeners.register(ABILITY_A1_ID, CaoQunGuOrganBehavior::activateA1);
    OrganActivationListeners.register(ABILITY_A2_ID, CaoQunGuOrganBehavior::activateA2);
    OrganActivationListeners.register(ABILITY_A3_ID, CaoQunGuOrganBehavior::activateA3);
    OrganActivationListeners.register(ABILITY_A4_ID, CaoQunGuOrganBehavior::activateA4);
  }

  private CaoQunGuOrganBehavior() {}

  @Override
  public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
    if (!(entity instanceof Player player) || cc == null || organ == null || organ.isEmpty()) {
      return;
    }
    Level level = player.level();
    if (!(level instanceof ServerLevel serverLevel) || level.isClientSide()) {
      return;
    }
    if (!matchesOrgan(organ, ORGAN_ID)) {
      return;
    }

    OrganState state = organState(organ, STATE_ROOT);
    MultiCooldown cooldown = cooldown(cc, organ, state);
    long now = serverLevel.getGameTime();

    maintainStageProgress(player, cc, organ, state, now);
    maintainPassiveOne(player, cc, organ, state, now);
    maintainPassiveTwo(player, cc, organ, state, now);
    maintainPassiveThree(player, cc, organ, state, now);
    maintainPassiveFour(player, cc, organ, state, now);

    maintainAbilityOne(player, cc, organ, state, cooldown, now);
    maintainAbilityTwo(player, cc, organ, state, now);
    maintainAbilityThree(player, cc, organ, state, now);
    maintainAbilityFour(player, cc, organ, state, now);
  }

  @Override
  public float onIncomingDamage(
      DamageSource source,
      LivingEntity victim,
      ChestCavityInstance cc,
      ItemStack organ,
      float damage) {
    if (!(victim instanceof Player player) || cc == null || organ == null || organ.isEmpty()) {
      return damage;
    }
    Level level = player.level();
    if (!(level instanceof ServerLevel serverLevel) || level.isClientSide()) {
      return damage;
    }
    if (!matchesOrgan(organ, ORGAN_ID)) {
      return damage;
    }

    OrganState state = organState(organ, STATE_ROOT);
    long now = serverLevel.getGameTime();

    handleAbilityOneThorns(cc, organ, player, state, source, now);

    double adjusted = damage;
    adjusted = handleAbilityThreeAbsorption(player, state, damage, adjusted, now);
    adjusted = handleAbilityFourMitigation(cc, organ, player, state, source, damage, adjusted, now);
    return (float) adjusted;
  }

  @Override
  public void onRemoved(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
    if (entity == null || entity.level().isClientSide()) {
      return;
    }
    if (cc == null || organ == null || organ.isEmpty() || !matchesOrgan(organ, ORGAN_ID)) {
      return;
    }

    AbsorptionHelper.clearAbsorptionCapacity(entity, ABSORPTION_MODIFIER_ID);

    OrganState state = organState(organ, STATE_ROOT);
    state.setLong(KEY_A1_ACTIVE_UNTIL, 0L, value -> Math.max(0L, value), 0L);
    state.setDouble(KEY_A1_SHIELD_VALUE, 0.0D, value -> Math.max(0.0D, value), 0.0D);
    state.setInt(KEY_A1_REFLECT_STACKS, 0, clampNonNegative(), 0);
  }

  private static void activateA1(LivingEntity entity, ChestCavityInstance cc) {
    if (!(entity instanceof ServerPlayer player) || cc == null || entity.level().isClientSide()) {
      return;
    }
    ItemStack organ = findOrgan(cc);
    if (organ.isEmpty()) {
      return;
    }
    ServerLevel level = player.serverLevel();
    OrganState state = INSTANCE.organState(organ, STATE_ROOT);
    MultiCooldown cooldown = INSTANCE.cooldown(cc, organ, state);
    long now = level.getGameTime();
    MultiCooldown.Entry entry = cooldown.entry(KEY_COOLDOWN_READY_A1);
    if (!entry.isReady(now)) {
      return;
    }
    if (!payAbilityCost(player, 50.0D, 8.0D, 2.0D, 2.0D, 2.0D, 1.0D)) {
      return;
    }

    double absorption = computeA1ShieldValue(player, state);
    AbsorptionHelper.applyAbsorption(player, absorption, ABSORPTION_MODIFIER_ID, false);
    state.setLong(
        KEY_A1_ACTIVE_UNTIL, now + adjustedA1Duration(state), value -> Math.max(0L, value), 0L);
    state.setDouble(KEY_A1_SHIELD_VALUE, absorption, value -> Math.max(0.0D, value), 0.0D);
    state.setInt(KEY_A1_REFLECT_STACKS, 0, clampNonNegative(), 0);
    entry.setReadyAt(now + adjustedCooldown(state, BASE_A1_COOLDOWN_TICKS));
    ActiveSkillRegistry.scheduleReadyToast(player, ABILITY_A1_ID, entry.getReadyTick(), now);

    spawnA1ActivationEffects(level, player);
    grantSp(player, cc, organ, state, SpCategory.GENERAL, 4, StageTier.TIER_TWO_I);
  }

  private static void activateA2(LivingEntity entity, ChestCavityInstance cc) {
    if (!(entity instanceof ServerPlayer player) || cc == null || entity.level().isClientSide()) {
      return;
    }
    ItemStack organ = findOrgan(cc);
    if (organ.isEmpty()) {
      return;
    }
    ServerLevel level = player.serverLevel();
    OrganState state = INSTANCE.organState(organ, STATE_ROOT);
    MultiCooldown cooldown = INSTANCE.cooldown(cc, organ, state);
    long now = level.getGameTime();
    MultiCooldown.Entry entry = cooldown.entry(KEY_COOLDOWN_READY_A2);
    if (!entry.isReady(now)) {
      return;
    }
    if (!isStageUnlocked(state, StageTier.TIER_TWO_III)) {
      return;
    }
    if (!payAbilityCost(player, 80.0D, 10.0D, 3.0D, 3.0D, 3.0D, 1.0D)) {
      return;
    }

    int radius = adjustedA2Radius(state);
    int targets = adjustedA2Targets(state);
    double healPerTick = computeA2HealPerTick(player, state);
    applyA2Healing(level, player, radius, targets, healPerTick, BASE_A2_DURATION_TICKS, true, true);

    state.setLong(
        KEY_A2_ACTIVE_UNTIL, now + BASE_A2_DURATION_TICKS, value -> Math.max(0L, value), 0L);
    state.setLong(KEY_A2_LAST_HEAL_TICK, now, value -> Math.max(0L, value), 0L);
    entry.setReadyAt(now + adjustedCooldown(state, BASE_A2_COOLDOWN_TICKS));
    ActiveSkillRegistry.scheduleReadyToast(player, ABILITY_A2_ID, entry.getReadyTick(), now);

    grantSp(player, cc, organ, state, SpCategory.HEALING, 6, StageTier.TIER_TWO_III);
  }

  private static void activateA3(LivingEntity entity, ChestCavityInstance cc) {
    if (!(entity instanceof ServerPlayer player) || cc == null || entity.level().isClientSide()) {
      return;
    }
    ItemStack organ = findOrgan(cc);
    if (organ.isEmpty()) {
      return;
    }
    ServerLevel level = player.serverLevel();
    OrganState state = INSTANCE.organState(organ, STATE_ROOT);
    MultiCooldown cooldown = INSTANCE.cooldown(cc, organ, state);
    long now = level.getGameTime();
    if (!isStageUnlocked(state, StageTier.TIER_THREE_III)) {
      return;
    }
    MultiCooldown.Entry entry = cooldown.entry(KEY_COOLDOWN_READY_A3);
    if (!entry.isReady(now)) {
      return;
    }
    if (!payAbilityCost(player, 120.0D, 12.0D, 4.0D, 4.0D, 5.0D, 2.0D)) {
      return;
    }

    state.setLong(
        KEY_A3_ACTIVE_UNTIL, now + BASE_A3_DURATION_TICKS, value -> Math.max(0L, value), 0L);
    state.setDouble(KEY_A3_ABSORBED, 0.0D, value -> Math.max(0.0D, value), 0.0D);
    state.setBoolean(KEY_A3_HEAL_PENDING, false, false);

    entry.setReadyAt(now + adjustedCooldown(state, BASE_A3_COOLDOWN_TICKS));
    ActiveSkillRegistry.scheduleReadyToast(player, ABILITY_A3_ID, entry.getReadyTick(), now);

    player
        .level()
        .playSound(
            null,
            player.blockPosition(),
            SoundEvents.SHIELD_BLOCK,
            SoundSource.PLAYERS,
            0.6F,
            0.9F);
    grantSp(player, cc, organ, state, SpCategory.DEFENSE_STEADY, 5, StageTier.TIER_THREE_III);
  }

  private static void activateA4(LivingEntity entity, ChestCavityInstance cc) {
    if (!(entity instanceof ServerPlayer player) || cc == null || entity.level().isClientSide()) {
      return;
    }
    ItemStack organ = findOrgan(cc);
    if (organ.isEmpty()) {
      return;
    }
    ServerLevel level = player.serverLevel();
    OrganState state = INSTANCE.organState(organ, STATE_ROOT);
    MultiCooldown cooldown = INSTANCE.cooldown(cc, organ, state);
    long now = level.getGameTime();
    if (!isStageUnlocked(state, StageTier.TIER_FOUR_III)) {
      return;
    }
    MultiCooldown.Entry entry = cooldown.entry(KEY_COOLDOWN_READY_A4);
    if (!entry.isReady(now)) {
      return;
    }
    if (!payAbilityCost(player, 150.0D, 15.0D, 5.0D, 5.0D, 4.0D, 2.0D)) {
      return;
    }

    int duration = adjustedA4Duration(state);
    state.setLong(KEY_A4_ACTIVE_UNTIL, now + duration, value -> Math.max(0L, value), 0L);
    state.setLong(KEY_A4_REGEN_UNTIL, 0L, value -> Math.max(0L, value), 0L);
    entry.setReadyAt(now + adjustedCooldown(state, BASE_A4_COOLDOWN_TICKS));
    ActiveSkillRegistry.scheduleReadyToast(player, ABILITY_A4_ID, entry.getReadyTick(), now);

    player
        .level()
        .playSound(
            null,
            player.blockPosition(),
            SoundEvents.SHIELD_BLOCK,
            SoundSource.PLAYERS,
            0.6F,
            1.0F);
    grantSp(player, cc, organ, state, SpCategory.DEFENSE, 5, StageTier.TIER_FOUR_III);
  }

  private void maintainStageProgress(
      Player player, ChestCavityInstance cc, ItemStack organ, OrganState state, long now) {
    int stageIndex = currentStage(state);
    StageData stage = StageData.byIndex(stageIndex);
    if (stage == null) {
      return;
    }
    if (isStageComplete(state, stage)) {
      advanceStage(player, cc, organ, state, stageIndex + 1);
    }
    if (stageIndex >= StageData.values().length - 1) {
      return;
    }
    // 自动结转预存点数：当当前阶段刚完成时，advanceStage 已处理
  }

  private void maintainPassiveOne(
      Player player, ChestCavityInstance cc, ItemStack organ, OrganState state, long now) {
    Vec3 currentPos = player.position();
    double lastX = state.getDouble(KEY_LAST_POS_X, currentPos.x);
    double lastY = state.getDouble(KEY_LAST_POS_Y, currentPos.y);
    double lastZ = state.getDouble(KEY_LAST_POS_Z, currentPos.z);
    double distanceSq =
        currentPos.subtract(lastX, lastY, lastZ).horizontalDistanceSqr()
            + Math.pow(currentPos.y - lastY, 2);

    int stillTicks = state.getInt(KEY_STILL_TICKS, 0);
    if (distanceSq < 0.0025D && player.getDeltaMovement().lengthSqr() < 0.01D) {
      stillTicks += 20;
    } else {
      stillTicks = 0;
      state.setDouble(KEY_LAST_POS_X, currentPos.x, value -> value, currentPos.x);
      state.setDouble(KEY_LAST_POS_Y, currentPos.y, value -> value, currentPos.y);
      state.setDouble(KEY_LAST_POS_Z, currentPos.z, value -> value, currentPos.z);
    }
    state.setInt(KEY_STILL_TICKS, stillTicks, clampNonNegative(), 0);

    int healAccum = state.getInt(KEY_P1_HEAL_ACCUM, 0);
    int costAccum = state.getInt(KEY_P1_COST_ACCUM, 0);

    if (stillTicks >= P1_STILL_THRESHOLD_TICKS) {
      healAccum += 20;
      costAccum += 20;
      if (healAccum >= P1_HEAL_INTERVAL_TICKS) {
        double healAmount = adjustedPassiveHeal(state);
        player.heal((float) healAmount);
        healAccum -= P1_HEAL_INTERVAL_TICKS;
        grantSp(player, cc, organ, state, SpCategory.HEALING, 1, StageTier.TIER_TWO_I);
      }
      if (costAccum >= P1_COST_INTERVAL_TICKS && player instanceof ServerPlayer serverPlayer) {
        payPassiveCost(serverPlayer, state, P1_BASE_ZHENYUAN_COST, P1_BASE_JINGLI_COST);
        costAccum -= P1_COST_INTERVAL_TICKS;
      }
    } else {
      healAccum = Math.min(healAccum, P1_HEAL_INTERVAL_TICKS);
      costAccum = Math.min(costAccum, P1_COST_INTERVAL_TICKS);
    }
    state.setInt(KEY_P1_HEAL_ACCUM, healAccum, clampNonNegative(), 0);
    state.setInt(KEY_P1_COST_ACCUM, costAccum, clampNonNegative(), 0);
  }

  private void maintainPassiveTwo(
      Player player, ChestCavityInstance cc, ItemStack organ, OrganState state, long now) {
    int timer = state.getInt(KEY_P2_TIMER, 0) + 20;
    if (timer < P2_TICK_INTERVAL_TICKS) {
      state.setInt(KEY_P2_TIMER, timer, clampNonNegative(), 0);
      return;
    }
    timer -= P2_TICK_INTERVAL_TICKS;
    state.setInt(KEY_P2_TIMER, timer, clampNonNegative(), 0);

    if (!isPlayerInWoodland(player)) {
      return;
    }
    Optional<ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
    handleOpt.ifPresent(
        handle -> {
          ResourceOps.tryReplenishScaledZhenyuan(handle, P2_ZHENYUAN_RESTORE, true);
          ResourceOps.tryAdjustJingli(handle, P2_JINGLI_RESTORE, true);
        });
  }

  private void maintainPassiveThree(
      Player player, ChestCavityInstance cc, ItemStack organ, OrganState state, long now) {
    int timer = state.getInt(KEY_P3_TIMER, 0) + 20;
    if (timer < P3_TICK_INTERVAL_TICKS) {
      state.setInt(KEY_P3_TIMER, timer, clampNonNegative(), 0);
      return;
    }
    timer -= P3_TICK_INTERVAL_TICKS;
    state.setInt(KEY_P3_TIMER, timer, clampNonNegative(), 0);

    if (player.getAbsorptionAmount() <= 0.0F) {
      return;
    }
    double heal = P3_HEAL_PER_TICK;
    player.heal((float) heal);
    grantSp(player, cc, organ, state, SpCategory.HEALING, 1, StageTier.TIER_FOUR_I);
  }

  private void maintainPassiveFour(
      Player player, ChestCavityInstance cc, ItemStack organ, OrganState state, long now) {
    if (!isStageUnlocked(state, StageTier.TIER_FOUR_III)) {
      return;
    }
    long readyAt = Math.max(0L, state.getLong(KEY_P4_READY_AT, 0L));
    if (readyAt > now) {
      return;
    }
    if (player.getHealth() > player.getMaxHealth() * 0.30F) {
      return;
    }
    double heal = P4_HEAL_FLAT + player.getMaxHealth() * P4_HEAL_RATIO;
    player.heal((float) heal);
    player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, P4_REGEN_DURATION_TICKS, 0));
    state.setLong(KEY_P4_READY_AT, now + P4_COOLDOWN_TICKS, value -> Math.max(0L, value), 0L);
    grantSp(player, cc, organ, state, SpCategory.DEFENSE, 6, StageTier.TIER_FOUR_III);
  }

  private void maintainAbilityOne(
      Player player,
      ChestCavityInstance cc,
      ItemStack organ,
      OrganState state,
      MultiCooldown cooldown,
      long now) {
    long activeUntil = Math.max(0L, state.getLong(KEY_A1_ACTIVE_UNTIL, 0L));
    if (activeUntil <= now) {
      AbsorptionHelper.clearAbsorptionCapacity(player, ABSORPTION_MODIFIER_ID);
      state.setDouble(KEY_A1_SHIELD_VALUE, 0.0D, value -> Math.max(0.0D, value), 0.0D);
      return;
    }
    if (now % 20L == 0L) {
      spawnA1AmbientParticles(player);
    }
  }

  /** 定时执行草裙蛊第二主动的持续治疗逻辑，确保 2 秒一次的脉冲治疗与阶段 SP 统计。 */
  private void maintainAbilityTwo(
      Player player, ChestCavityInstance cc, ItemStack organ, OrganState state, long now) {
    if (!(player instanceof ServerPlayer serverPlayer)) {
      return;
    }
    long activeUntil = Math.max(0L, state.getLong(KEY_A2_ACTIVE_UNTIL, 0L));
    if (activeUntil <= now) {
      return;
    }

    long lastHeal = Math.max(0L, state.getLong(KEY_A2_LAST_HEAL_TICK, 0L));
    if (now - lastHeal < BASE_A2_TICK_INTERVAL) {
      return;
    }

    int radius = adjustedA2Radius(state);
    int targets = adjustedA2Targets(state);
    double healPerTick = computeA2HealPerTick(player, state);
    int regenDuration =
        (int) Math.max(20L, Math.min((long) BASE_A2_DURATION_TICKS, activeUntil - now));

    applyA2Healing(
        serverPlayer.serverLevel(),
        serverPlayer,
        radius,
        targets,
        healPerTick,
        regenDuration,
        true,
        false);

    state.setLong(KEY_A2_LAST_HEAL_TICK, now, value -> Math.max(0L, value), 0L);
    grantSp(player, cc, organ, state, SpCategory.HEALING, 2, StageTier.TIER_TWO_III);
  }

  private void maintainAbilityThree(
      Player player, ChestCavityInstance cc, ItemStack organ, OrganState state, long now) {
    long activeUntil = Math.max(0L, state.getLong(KEY_A3_ACTIVE_UNTIL, 0L));
    if (activeUntil > now) {
      return;
    }
    boolean healPending = state.getBoolean(KEY_A3_HEAL_PENDING, false);
    if (!healPending) {
      return;
    }
    double absorbed = Math.max(0.0D, state.getDouble(KEY_A3_ABSORBED, 0.0D));
    if (absorbed <= EPSILON) {
      state.setBoolean(KEY_A3_HEAL_PENDING, false, false);
      return;
    }
    double healRatio = BASE_A3_HEAL_RATIO;
    double capRatio = adjustedA3HealCap(state);
    double healAmount = Math.min(player.getMaxHealth() * capRatio, absorbed * healRatio);
    if (healAmount > 0.0D) {
      player.heal((float) healAmount);
    }
    state.setBoolean(KEY_A3_HEAL_PENDING, false, false);
    state.setDouble(KEY_A3_ABSORBED, 0.0D, value -> Math.max(0.0D, value), 0.0D);
    grantSp(player, cc, organ, state, SpCategory.DEFENSE_STEADY, 4, StageTier.TIER_THREE_III);
  }

  /** 在姿态结束后安排与执行叶影庇佑的再生阶段，覆盖五转后的强化数值。 */
  private void maintainAbilityFour(
      Player player, ChestCavityInstance cc, ItemStack organ, OrganState state, long now) {
    long activeUntil = Math.max(0L, state.getLong(KEY_A4_ACTIVE_UNTIL, 0L));
    long regenUntil = Math.max(0L, state.getLong(KEY_A4_REGEN_UNTIL, 0L));

    if (activeUntil > now) {
      return;
    }

    if (activeUntil > 0L && regenUntil <= now) {
      long newUntil = now + adjustedA4RegenDuration(state);
      state.setLong(KEY_A4_REGEN_UNTIL, newUntil, value -> Math.max(0L, value), 0L);
      regenUntil = newUntil;
    }

    if (regenUntil <= now || now % 20L != 0L) {
      return;
    }

    double heal = adjustedA4RegenPerSecond(state);
    player.heal((float) heal);
    grantSp(player, cc, organ, state, SpCategory.HEALING, 1, StageTier.TIER_FOUR_III);
  }

  private void handleAbilityOneThorns(
      ChestCavityInstance cc,
      ItemStack organ,
      Player player,
      OrganState state,
      DamageSource source,
      long now) {
    long activeUntil = Math.max(0L, state.getLong(KEY_A1_ACTIVE_UNTIL, 0L));
    if (activeUntil <= now) {
      return;
    }
    Entity attacker = source.getEntity();
    if (!(attacker instanceof LivingEntity living)) {
      return;
    }
    if (player.distanceTo(living) > 4.0F) {
      return;
    }
    MultiCooldown cooldown = cooldown(cc, organ, state);
    String cooldownKey = KEY_COOLDOWN_REFLECT_PREFIX + living.getUUID();
    MultiCooldown.Entry entry = cooldown.entry(cooldownKey);
    if (!entry.isReady(now)) {
      return;
    }
    int currentStacks = Math.max(0, state.getInt(KEY_A1_REFLECT_STACKS, 0));
    int maxStacks = adjustedA1ReflectStacks(state);
    if (currentStacks >= maxStacks) {
      return;
    }
    living.hurt(player.damageSources().thorns(player), (float) A1_REFLECT_DAMAGE);
    entry.setReadyAt(now + A1_REFLECT_INTERVAL_TICKS);
    state.setInt(KEY_A1_REFLECT_STACKS, currentStacks + 1, clampNonNegative(), 0);
  }

  private double handleAbilityThreeAbsorption(
      Player player, OrganState state, double originalAmount, double currentAmount, long now) {
    long activeUntil = Math.max(0L, state.getLong(KEY_A3_ACTIVE_UNTIL, 0L));
    if (activeUntil <= now) {
      return currentAmount;
    }
    if (originalAmount <= 0.0D) {
      return currentAmount;
    }
    double reduction = BASE_A3_DAMAGE_REDUCTION;
    double reduced = originalAmount * (1.0D - reduction);
    double result = Math.min(currentAmount, reduced);
    double mitigated = Math.max(0.0D, originalAmount - result);

    double accumulated = Math.max(0.0D, state.getDouble(KEY_A3_ABSORBED, 0.0D));
    state.setDouble(KEY_A3_ABSORBED, accumulated + mitigated, value -> Math.max(0.0D, value), 0.0D);
    state.setBoolean(KEY_A3_HEAL_PENDING, true, false);
    return result;
  }

  private double handleAbilityFourMitigation(
      ChestCavityInstance cc,
      ItemStack organ,
      Player player,
      OrganState state,
      DamageSource source,
      double originalAmount,
      double currentAmount,
      long now) {
    long activeUntil = Math.max(0L, state.getLong(KEY_A4_ACTIVE_UNTIL, 0L));
    if (activeUntil <= now) {
      return currentAmount;
    }
    if (currentAmount <= 0.0D) {
      return currentAmount;
    }
    double multiplier = 1.0D - BASE_A4_DAMAGE_REDUCTION;
    if (isFrontBlock(player, source)) {
      multiplier *= 1.0D - BASE_A4_FRONT_BLOCK;
      grantSp(player, cc, organ, state, SpCategory.BLOCK, 1, StageTier.TIER_FOUR_III);
    }
    double result = currentAmount * multiplier;
    long abilityEnd = Math.max(now, activeUntil);
    long regenDuration = adjustedA4RegenDuration(state);
    long currentRegenUntil = Math.max(0L, state.getLong(KEY_A4_REGEN_UNTIL, 0L));
    long scheduledEnd = Math.max(currentRegenUntil, abilityEnd + regenDuration);
    state.setLong(KEY_A4_REGEN_UNTIL, scheduledEnd, value -> Math.max(0L, value), 0L);
    return result;
  }

  private static boolean isFrontBlock(Player player, DamageSource source) {
    Entity attacker = source.getEntity();
    if (!(attacker instanceof LivingEntity living)) {
      return false;
    }
    Vec3 toAttacker = living.position().subtract(player.position());
    Vec3 look = player.getLookAngle();
    double dot = toAttacker.normalize().dot(look.normalize());
    return dot > MIN_HORIZONTAL_DOT_FOR_FRONT_BLOCK;
  }

  private static int adjustedCooldown(OrganState state, int baseTicks) {
    return baseTicks;
  }

  private static int adjustedA1Duration(OrganState state) {
    double multiplier = 1.0D;
    if (currentStage(state) >= StageTier.TIER_FIVE_II.ordinal()) {
      multiplier += 0.20D;
    }
    return (int) Math.round(baseDurationWithMultiplier(BASE_A1_DURATION_TICKS, multiplier));
  }

  private static double computeA1ShieldValue(Player player, OrganState state) {
    double base =
        BASE_A1_ABSORPTION_FLAT + player.getMaxHealth() * BASE_A1_ABSORPTION_MAX_HEALTH_RATIO;
    double multiplier = 1.0D;
    if (currentStage(state) >= StageTier.TIER_TWO_II.ordinal()) {
      multiplier += 0.20D;
    }
    if (currentStage(state) >= StageTier.TIER_FIVE_I.ordinal()) {
      multiplier += 0.25D;
    }
    return base * multiplier;
  }

  private static int adjustedA1ReflectStacks(OrganState state) {
    int stacks = 1;
    if (currentStage(state) >= StageTier.TIER_THREE_II.ordinal()) {
      stacks += 1;
    }
    return stacks;
  }

  private static int adjustedA2Targets(OrganState state) {
    int targets = BASE_A2_TARGETS;
    if (currentStage(state) >= StageTier.TIER_FOUR_II.ordinal()) {
      targets += 1;
    }
    return targets;
  }

  private static int adjustedA2Radius(OrganState state) {
    int radius = BASE_A2_RADIUS;
    if (currentStage(state) >= StageTier.TIER_FOUR_II.ordinal()) {
      radius += 1;
    }
    if (currentStage(state) >= StageTier.TIER_FIVE_I.ordinal()) {
      radius += 2;
    }
    return radius;
  }

  private static double computeA2HealPerTick(Player player, OrganState state) {
    double heal = BASE_A2_HEAL_FLAT + player.getMaxHealth() * BASE_A2_HEAL_RATIO;
    if (currentStage(state) >= StageTier.TIER_THREE_II.ordinal()) {
      heal *= 1.15D;
    }
    if (currentStage(state) >= StageTier.TIER_FIVE_III.ordinal()) {
      heal *= 1.10D;
    }
    return heal;
  }

  private static int adjustedA4Duration(OrganState state) {
    double multiplier = 1.0D;
    if (currentStage(state) >= StageTier.TIER_FIVE_II.ordinal()) {
      multiplier += 0.20D;
    }
    return (int) Math.round(baseDurationWithMultiplier(BASE_A4_DURATION_TICKS, multiplier));
  }

  /** 五转阶段延长再生持续时间。 */
  private static long adjustedA4RegenDuration(OrganState state) {
    long duration = BASE_A4_REGEN_DURATION_TICKS;
    if (currentStage(state) >= StageTier.TIER_FIVE_II.ordinal()) {
      duration += 20L;
    }
    if (currentStage(state) >= StageTier.TIER_FIVE_III.ordinal()) {
      duration += 20L;
    }
    return duration;
  }

  /** 五转阶段提升再生强度。 */
  private static double adjustedA4RegenPerSecond(OrganState state) {
    double heal = A4_REGEN_PER_SECOND;
    if (currentStage(state) >= StageTier.TIER_FIVE_I.ordinal()) {
      heal *= 1.15D;
    }
    if (currentStage(state) >= StageTier.TIER_FIVE_III.ordinal()) {
      heal *= 1.10D;
    }
    return heal;
  }

  private static double adjustedA3HealCap(OrganState state) {
    double cap = BASE_A3_HEAL_CAP_RATIO;
    if (currentStage(state) >= StageTier.TIER_FIVE_II.ordinal()) {
      cap += 0.10D;
    }
    return cap;
  }

  private static double adjustedPassiveHeal(OrganState state) {
    double heal = P1_HEAL_PER_TICK;
    if (currentStage(state) >= StageTier.TIER_FIVE_III.ordinal()) {
      heal *= 1.10D;
    }
    return heal;
  }

  private static int baseDurationWithMultiplier(int base, double multiplier) {
    return Mth.clamp((int) Math.round(base * multiplier), 1, Integer.MAX_VALUE);
  }

  private static boolean payAbilityCost(
      ServerPlayer player,
      double zhenyuan,
      double jingli,
      double hunpo,
      double niantou,
      double satiety,
      double health) {
    Objects.requireNonNull(player, "player");
    Optional<ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
    if (handleOpt.isEmpty()) {
      return false;
    }
    ResourceHandle handle = handleOpt.get();

    double currentHunpo = handle.read("hunpo").orElse(0.0D);
    if (currentHunpo + EPSILON < hunpo) {
      return false;
    }
    double currentNiantou = handle.read("niantou").orElse(0.0D);
    if (currentNiantou + EPSILON < niantou) {
      return false;
    }
    if (!hasEnoughHunger(player, satiety)) {
      return false;
    }
    if (!hasEnoughHealth(player, health)) {
      return false;
    }

    GuzhenrenResourceBridge.ResourceHandle finalHandle = handle;
    var payment = ResourceOps.consumeStrict(player, zhenyuan, jingli);
    if (!payment.succeeded()) {
      return false;
    }

    boolean success = true;
    if (hunpo > 0.0D) {
      success &= finalHandle.adjustDouble("hunpo", -hunpo, true, "zuida_hunpo").isPresent();
    }
    if (success && niantou > 0.0D) {
      success &= finalHandle.adjustDouble("niantou", -niantou, true, "niantou_zuida").isPresent();
    }
    double[] hungerSnapshot = snapshotHunger(player);
    if (success && satiety > 0.0D) {
      success &= drainHunger(player, satiety);
    }
    if (success && health > 0.0D) {
      success &= ResourceOps.drainHealth(player, (float) health, (float) (health / 4.0D));
    }
    if (!success) {
      ResourceOps.refund(player, payment);
      if (hunpo > 0.0D) {
        finalHandle.adjustDouble("hunpo", hunpo, true, "zuida_hunpo");
      }
      if (niantou > 0.0D) {
        finalHandle.adjustDouble("niantou", niantou, true, "niantou_zuida");
      }
      restoreHunger(player, hungerSnapshot);
    }
    return success;
  }

  private static void payPassiveCost(
      ServerPlayer player, OrganState state, double zhenyuan, double jingli) {
    ResourceOps.consumeStrict(player, zhenyuan, jingli);
  }

  private static boolean isPlayerInWoodland(Player player) {
    if (!(player.level() instanceof ServerLevel serverLevel)) {
      return false;
    }
    Holder<Biome> holder = serverLevel.getBiome(player.blockPosition());
    return WOODLAND_BIOME_TAGS.stream().anyMatch(holder::is);
  }

  private static double[] snapshotHunger(Player player) {
    return new double[] {
      player.getFoodData().getFoodLevel(), player.getFoodData().getSaturationLevel()
    };
  }

  private static void restoreHunger(Player player, double[] snapshot) {
    if (snapshot == null || snapshot.length < 2) {
      return;
    }
    player.getFoodData().setFoodLevel((int) snapshot[0]);
    player.getFoodData().setSaturation((float) snapshot[1]);
  }

  private static boolean drainHunger(Player player, double amount) {
    if (amount <= 0.0D) {
      return true;
    }
    if (!hasEnoughHunger(player, amount)) {
      return false;
    }
    var data = player.getFoodData();
    double remaining = amount;
    float saturation = data.getSaturationLevel();
    float newSaturation = (float) Math.max(0.0D, saturation - remaining);
    remaining -= Math.max(0.0D, saturation - newSaturation);
    data.setSaturation(newSaturation);
    if (remaining > 0.0D) {
      int foodLevel = data.getFoodLevel();
      int newFoodLevel = (int) Math.max(0.0D, foodLevel - Math.ceil(remaining));
      data.setFoodLevel(newFoodLevel);
    }
    return true;
  }

  private static boolean hasEnoughHunger(Player player, double amount) {
    if (amount <= 0.0D) {
      return true;
    }
    var data = player.getFoodData();
    double total = data.getFoodLevel() + data.getSaturationLevel();
    return total + EPSILON >= amount + HUNGER_RESERVE_EPSILON;
  }

  private static boolean hasEnoughHealth(Player player, double amount) {
    if (amount <= 0.0D) {
      return true;
    }
    return player.getHealth() - amount > 1.0F;
  }

  private static void spawnA1ActivationEffects(ServerLevel level, Player player) {
    level.playSound(
        null,
        player.blockPosition(),
        SoundEvents.AMETHYST_BLOCK_CHIME,
        SoundSource.PLAYERS,
        0.8F,
        1.2F);
    spawnA1AmbientParticles(player);
  }

  private static void spawnA1AmbientParticles(Player player) {
    Level level = player.level();
    if (!(level instanceof ServerLevel serverLevel)) {
      return;
    }
    Vec3 pos = player.position();
    serverLevel.sendParticles(
        ParticleTypes.HAPPY_VILLAGER, pos.x, pos.y + 0.8D, pos.z, 6, 0.3D, 0.4D, 0.3D, 0.01D);
  }

  private static void applyA2Healing(
      ServerLevel level,
      ServerPlayer player,
      int radius,
      int targets,
      double heal,
      int regenDurationTicks,
      boolean applyRegen,
      boolean playSound) {
    List<LivingEntity> recipients = collectA2Recipients(level, player, radius, targets);
    for (LivingEntity recipient : recipients) {
      if (applyRegen) {
        recipient.addEffect(
            new MobEffectInstance(
                MobEffects.REGENERATION, regenDurationTicks, 0, false, true, true));
      }
      recipient.heal((float) heal);
    }
    if (playSound) {
      level.playSound(
          null,
          player.blockPosition(),
          SoundEvents.AMETHYST_BLOCK_RESONATE,
          SoundSource.PLAYERS,
          0.9F,
          1.1F);
    }
  }

  /** 收集当前脉冲要治疗的友方单位，优先包含自身并限制在阶段允许的目标数量内。 */
  private static List<LivingEntity> collectA2Recipients(
      ServerLevel level, ServerPlayer player, int radius, int targets) {
    AABB area = new AABB(player.blockPosition()).inflate(radius);
    List<LivingEntity> recipients = new ArrayList<>();
    recipients.add(player);
    for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area)) {
      if (entity == player) {
        continue;
      }
      if (!player.isAlliedTo(entity)) {
        continue;
      }
      recipients.add(entity);
      if (recipients.size() >= targets) {
        break;
      }
    }
    return recipients;
  }

  private static int currentStage(OrganState state) {
    return Math.max(0, state.getInt(KEY_STAGE, 0));
  }

  private static boolean isStageUnlocked(OrganState state, StageTier tier) {
    return currentStage(state) >= tier.ordinal();
  }

  private static boolean isStageComplete(OrganState state, StageData stage) {
    int total = state.getInt(KEY_STAGE_TOTAL, 0);
    if (total < stage.requiredSp()) {
      return false;
    }
    for (Map.Entry<SpCategory, Integer> entry : stage.categoryMinimums().entrySet()) {
      int current = categoryValue(state, entry.getKey());
      if (current < entry.getValue()) {
        return false;
      }
    }
    return true;
  }

  private static void advanceStage(
      Player player, ChestCavityInstance cc, ItemStack organ, OrganState state, int nextStage) {
    if (nextStage >= StageData.values().length) {
      state.setInt(KEY_STAGE, StageData.values().length - 1, clampNonNegative(), 0);
      return;
    }
    state.setInt(KEY_STAGE, nextStage, clampNonNegative(), 0);
    resetStageCounters(state);
    applyPrestoredProgress(state, nextStage);
    Component message = STAGE_MESSAGES.get(StageData.byIndex(nextStage));
    if (message != null) {
      player.displayClientMessage(message, false);
    }
    NetworkUtil.sendOrganSlotUpdate(cc, organ);
  }

  private static void resetStageCounters(OrganState state) {
    state.setInt(KEY_STAGE_TOTAL, 0, clampNonNegative(), 0);
    state.setInt(KEY_STAGE_GENERAL, 0, clampNonNegative(), 0);
    state.setInt(KEY_STAGE_DEFENSE, 0, clampNonNegative(), 0);
    state.setInt(KEY_STAGE_HEALING, 0, clampNonNegative(), 0);
    state.setInt(KEY_STAGE_DEFENSE_STEADY, 0, clampNonNegative(), 0);
    state.setInt(KEY_STAGE_BLOCK, 0, clampNonNegative(), 0);
    state.setInt(KEY_STAGE_COOP, 0, clampNonNegative(), 0);
  }

  private static void applyPrestoredProgress(OrganState state, int stageIndex) {
    int total = state.getInt(KEY_PRE_TOTAL, 0);
    int general = state.getInt(KEY_PRE_GENERAL, 0);
    int defense = state.getInt(KEY_PRE_DEFENSE, 0);
    int healing = state.getInt(KEY_PRE_HEALING, 0);
    int defenseSteady = state.getInt(KEY_PRE_DEFENSE_STEADY, 0);
    int block = state.getInt(KEY_PRE_BLOCK, 0);
    int coop = state.getInt(KEY_PRE_COOP, 0);
    resetPrestore(state);
    state.setInt(KEY_STAGE_TOTAL, total, clampNonNegative(), 0);
    state.setInt(KEY_STAGE_GENERAL, general, clampNonNegative(), 0);
    state.setInt(KEY_STAGE_DEFENSE, defense, clampNonNegative(), 0);
    state.setInt(KEY_STAGE_HEALING, healing, clampNonNegative(), 0);
    state.setInt(KEY_STAGE_DEFENSE_STEADY, defenseSteady, clampNonNegative(), 0);
    state.setInt(KEY_STAGE_BLOCK, block, clampNonNegative(), 0);
    state.setInt(KEY_STAGE_COOP, coop, clampNonNegative(), 0);
  }

  private static void resetPrestore(OrganState state) {
    state.setInt(KEY_PRE_TOTAL, 0, clampNonNegative(), 0);
    state.setInt(KEY_PRE_GENERAL, 0, clampNonNegative(), 0);
    state.setInt(KEY_PRE_DEFENSE, 0, clampNonNegative(), 0);
    state.setInt(KEY_PRE_HEALING, 0, clampNonNegative(), 0);
    state.setInt(KEY_PRE_DEFENSE_STEADY, 0, clampNonNegative(), 0);
    state.setInt(KEY_PRE_BLOCK, 0, clampNonNegative(), 0);
    state.setInt(KEY_PRE_COOP, 0, clampNonNegative(), 0);
  }

  private static void grantSp(
      Player player,
      ChestCavityInstance cc,
      ItemStack organ,
      OrganState state,
      SpCategory category,
      int amount,
      StageTier tier) {
    if (amount <= 0 || category == null) {
      return;
    }
    int currentStage = currentStage(state);
    if (tier.ordinal() > currentStage) {
      prestoreSp(state, category, amount, currentStage + 1);
      return;
    }
    for (int i = 0; i < amount; i++) {
      int stageIndex = currentStage(state);
      StageData stage = StageData.byIndex(stageIndex);
      incrementCategory(state, category, 1);
      int total = state.getInt(KEY_STAGE_TOTAL, 0) + 1;
      state.setInt(KEY_STAGE_TOTAL, total, clampNonNegative(), 0);
      if (isStageComplete(state, stage)) {
        advanceStage(player, cc, organ, state, stageIndex + 1);
      }
    }
  }

  private static void prestoreSp(
      OrganState state, SpCategory category, int amount, int nextStageIndex) {
    StageData nextStage = StageData.byIndex(nextStageIndex);
    if (nextStage == null) {
      return;
    }
    int limit = (int) Math.floor(nextStage.requiredSp() * 0.4D);
    int currentTotal = state.getInt(KEY_PRE_TOTAL, 0);
    int allowed = Math.min(amount, Math.max(0, limit - currentTotal));
    if (allowed <= 0) {
      return;
    }
    incrementPrestore(state, category, allowed);
    state.setInt(KEY_PRE_TOTAL, currentTotal + allowed, clampNonNegative(), 0);
  }

  private static void incrementCategory(OrganState state, SpCategory category, int delta) {
    String key = categoryKey(category);
    state.setInt(key, state.getInt(key, 0) + delta, clampNonNegative(), 0);
  }

  private static void incrementPrestore(OrganState state, SpCategory category, int delta) {
    String key = prestoreKey(category);
    state.setInt(key, state.getInt(key, 0) + delta, clampNonNegative(), 0);
  }

  private static int categoryValue(OrganState state, SpCategory category) {
    return state.getInt(categoryKey(category), 0);
  }

  private static String categoryKey(SpCategory category) {
    return switch (category) {
      case GENERAL -> KEY_STAGE_GENERAL;
      case DEFENSE -> KEY_STAGE_DEFENSE;
      case HEALING -> KEY_STAGE_HEALING;
      case DEFENSE_STEADY -> KEY_STAGE_DEFENSE_STEADY;
      case BLOCK -> KEY_STAGE_BLOCK;
      case COOP -> KEY_STAGE_COOP;
    };
  }

  private static String prestoreKey(SpCategory category) {
    return switch (category) {
      case GENERAL -> KEY_PRE_GENERAL;
      case DEFENSE -> KEY_PRE_DEFENSE;
      case HEALING -> KEY_PRE_HEALING;
      case DEFENSE_STEADY -> KEY_PRE_DEFENSE_STEADY;
      case BLOCK -> KEY_PRE_BLOCK;
      case COOP -> KEY_PRE_COOP;
    };
  }

  private MultiCooldown cooldown(ChestCavityInstance cc, ItemStack organ, OrganState state) {
    return MultiCooldown.builder(state)
        .withSync(cc, organ)
        .withLongClamp(value -> Math.max(0L, value), 0L)
        .build();
  }

  private static ItemStack findOrgan(ChestCavityInstance cc) {
    if (cc == null || cc.inventory == null) {
      return ItemStack.EMPTY;
    }
    for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
      ItemStack candidate = cc.inventory.getItem(i);
      if (!candidate.isEmpty() && INSTANCE.matchesOrgan(candidate, ORGAN_ID)) {
        return candidate;
      }
    }
    return ItemStack.EMPTY;
  }

  private static Map<StageData, Component> buildStageMessages() {
    Map<StageData, Component> map = Maps.newEnumMap(StageData.class);
    map.put(StageData.TWO_I, Component.literal("草裙蛊·护幕成形：解锁藤裙护幕与织草生息。"));
    map.put(StageData.TWO_II, Component.literal("草裙蛊·护幕精进：护幕吸收强化。"));
    map.put(StageData.TWO_III, Component.literal("草裙蛊·露润回春：解锁持续治疗。"));
    map.put(StageData.THREE_I, Component.literal("草裙蛊·森风回灵：林地恢复奏效。"));
    map.put(StageData.THREE_II, Component.literal("草裙蛊·护幕反击：护幕反伤与治疗提升。"));
    map.put(StageData.THREE_III, Component.literal("草裙蛊·木心替身：解锁伤害转化护身。"));
    map.put(StageData.FOUR_I, Component.literal("草裙蛊·木纹护体：护盾共鸣并赋予器官分值。"));
    map.put(StageData.FOUR_II, Component.literal("草裙蛊·叶幕同纱：回春范围强化。"));
    map.put(StageData.FOUR_III, Component.literal("草裙蛊·叶影庇佑：解锁防御姿态与续命。"));
    map.put(StageData.FIVE_I, Component.literal("草裙蛊·庇护宏成：护幕与回春全面强化。"));
    map.put(StageData.FIVE_II, Component.literal("草裙蛊·守御长存：防御型主动持续延长。"));
    map.put(StageData.FIVE_III, Component.literal("草裙蛊·青藤长生：最终减伤与再生提升。"));
    return ImmutableMap.copyOf(map);
  }

  private static IntUnaryOperator clampNonNegative() {
    return value -> Math.max(0, value);
  }

  private enum SpCategory {
    GENERAL,
    DEFENSE,
    HEALING,
    DEFENSE_STEADY,
    BLOCK,
    COOP
  }

  private enum StageTier {
    TIER_TWO_I,
    TIER_TWO_II,
    TIER_TWO_III,
    TIER_THREE_I,
    TIER_THREE_II,
    TIER_THREE_III,
    TIER_FOUR_I,
    TIER_FOUR_II,
    TIER_FOUR_III,
    TIER_FIVE_I,
    TIER_FIVE_II,
    TIER_FIVE_III
  }

  private enum StageData {
    TWO_I(40, ImmutableMap.of(SpCategory.GENERAL, 20)),
    TWO_II(60, ImmutableMap.of(SpCategory.DEFENSE, 20)),
    TWO_III(80, ImmutableMap.of(SpCategory.HEALING, 30)),
    THREE_I(100, ImmutableMap.of(SpCategory.GENERAL, 30, SpCategory.HEALING, 30)),
    THREE_II(140, ImmutableMap.of(SpCategory.DEFENSE, 40)),
    THREE_III(180, ImmutableMap.of(SpCategory.DEFENSE_STEADY, 40)),
    FOUR_I(240, ImmutableMap.of(SpCategory.GENERAL, 60, SpCategory.DEFENSE, 60)),
    FOUR_II(300, ImmutableMap.of(SpCategory.DEFENSE, 40)),
    FOUR_III(360, ImmutableMap.of(SpCategory.BLOCK, 20, SpCategory.HEALING, 80)),
    FIVE_I(450, ImmutableMap.of()),
    FIVE_II(550, ImmutableMap.of(SpCategory.DEFENSE, 60)),
    FIVE_III(700, ImmutableMap.of(SpCategory.DEFENSE, 100, SpCategory.HEALING, 100));

    private final int requiredSp;
    private final Map<SpCategory, Integer> categoryMinimums;

    StageData(int requiredSp, Map<SpCategory, Integer> categoryMinimums) {
      this.requiredSp = requiredSp;
      this.categoryMinimums = categoryMinimums;
    }

    public int requiredSp() {
      return requiredSp;
    }

    public Map<SpCategory, Integer> categoryMinimums() {
      return categoryMinimums;
    }

    public static StageData byIndex(int index) {
      StageData[] values = values();
      if (index < 0 || index >= values.length) {
        return values[values.length - 1];
      }
      return values[index];
    }
  }
}
