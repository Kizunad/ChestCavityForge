package net.tigereye.chestcavity.compat.guzhenren.item.li_dao.behavior;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.item.li_dao.AbstractLiDaoOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.registry.GRDamageSources;
import net.tigereye.chestcavity.compat.guzhenren.util.CombatEntityUtil;
import net.tigereye.chestcavity.compat.guzhenren.util.DamagePipeline;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.BehaviorConfigAccess;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge.ResourceHandle;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.listeners.OrganIncomingDamageListener;
import net.tigereye.chestcavity.listeners.OrganOnHitListener;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.skill.ActiveSkillRegistry;
import net.tigereye.chestcavity.util.NetworkUtil;
import org.slf4j.Logger;

/**
 * 花豕蛊（力道·肌肉）
 *
 * <p>实现要点：
 *
 * <ul>
 *   <li>提供“冲撞—震退—追打”三段主动技，配合《牛劲回息》《力魄余威》被动维持节奏。
 *   <li>使用 {@link MultiCooldown} 统一管理冷却、蓄势窗口以及点数采样，避免散乱的 NBT 标记。
 *   <li>通过 {@link OrganState} 记录阶段、点数、资源采样与协同开关，在装备/卸载时可被完整重建。
 *   <li>所有资源开销均走 {@link ResourceOps} / {@link ResourceHandle}，保证真元缩放与精力结算一致。
 * </ul>
 */
public final class HuaShiGuOrganBehavior extends AbstractLiDaoOrganBehavior
    implements OrganSlowTickListener, OrganOnHitListener, OrganIncomingDamageListener {

  public static final HuaShiGuOrganBehavior INSTANCE = new HuaShiGuOrganBehavior();

  private static final Logger LOGGER = LogUtils.getLogger();

  private static final String MOD_ID = "guzhenren";
  private static final ResourceLocation ORGAN_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "hua_shi_gu");
  public static final ResourceLocation CHARGE_ABILITY_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "hua_shi_gu/charge");
  public static final ResourceLocation HOOFQUAKE_ABILITY_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "hua_shi_gu/hoofquake");
  public static final ResourceLocation OVERLOAD_ABILITY_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "hua_shi_gu/overload_burst");

  private static final ResourceLocation HAN_YUE_GU_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "han_yue_gu");
  private static final ResourceLocation TIE_SHOU_QIN_NA_GU_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "tie_shou_qin_na_gu");
  private static final ResourceLocation LIE_ZHUA_GU_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "lie_zhua_gu");
  private static final ResourceLocation QING_NIU_LAO_LI_GU_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "qing_niu_lao_li_gu");
  private static final ResourceLocation JING_LI_GU_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "jing_li_gu");

  private static final String STATE_ROOT = "HuaShiGu";

  // Passive & cooldown tracking --------------------------------------------------------------
  private static final String KEY_STAGE = "Stage";
  private static final String KEY_P1 = "P1";
  private static final String KEY_P2 = "P2";
  private static final String KEY_P3 = "P3";
  private static final String KEY_P4 = "P4";
  private static final String KEY_LAST_POS_X = "LastPosX";
  private static final String KEY_LAST_POS_Z = "LastPosZ";
  private static final String KEY_PASSIVE_READY_TICK = "PassiveReady";
  private static final String KEY_LAST_DAMAGE_TICK = "LastDamageTick";
  private static final String KEY_COMBO_COUNT = "ComboCount";
  private static final String KEY_COMBO_WINDOW_TICK = "ComboWindow";
  private static final String KEY_P4_WINDOW_TICK = "P4Window";
  private static final String KEY_P4_COUNT = "P4Count";

  // Ability cooldown keys -------------------------------------------------------------------
  private static final String KEY_CHARGE_READY_TICK = "ChargeReady";
  private static final String KEY_CHAIN_READY_TICK = "ChargeChainWindow";
  private static final String KEY_HOOFQUAKE_READY_TICK = "HoofquakeReady";
  private static final String KEY_OVERLOAD_READY_TICK = "OverloadReady";
  private static final String KEY_OVERLOAD_WINDOW_TICK = "OverloadWindow";
  private static final String KEY_OVERLOAD_STACKS = "OverloadStacks";
  private static final String KEY_OVERLOAD_PENDING = "OverloadPending";
  private static final String KEY_SYNERGY_HEAVY_READY = "SynergyHeavyReady";
  private static final String KEY_GRAB_TARGET_ID = "GrabTargetId";
  private static final String KEY_GRAB_WINDOW_TICK = "GrabWindowTick";

  // Deduplication helpers (per-player) -------------------------------------------------------
  private static final Object2ObjectMap<UUID, Object2LongMap<UUID>> P1_HIT_DEDUP =
      new Object2ObjectOpenHashMap<>();
  private static final Object2IntMap<UUID> P4_DAMAGE_FLAGS = new Object2IntOpenHashMap<>();

  // Configurable constants ------------------------------------------------------------------
  private static final int PASSIVE_INTERVAL_TICKS = 5 * 20;
  private static final double PASSIVE_BASE_ZHENYUAN_COST =
      BehaviorConfigAccess.getFloat(
          HuaShiGuOrganBehavior.class, "PASSIVE_BASE_ZHENYUAN_COST", 200.0f);
  private static final double PASSIVE_JINGLI_GAIN =
      BehaviorConfigAccess.getFloat(HuaShiGuOrganBehavior.class, "PASSIVE_JINGLI_GAIN", 3.0f);

  private static final double OVERDRIVE_BASE_COST =
      BehaviorConfigAccess.getFloat(HuaShiGuOrganBehavior.class, "OVERDRIVE_BASE_COST", 300.0f);
  private static final int OVERDRIVE_DURATION_TICKS = 10 * 20;
  private static final int OVERDRIVE_STRENGTH_LEVEL = 3;

  private static final double CHARGE_BASE_ZHENYUAN_COST =
      BehaviorConfigAccess.getFloat(
          HuaShiGuOrganBehavior.class, "CHARGE_BASE_ZHENYUAN_COST", 80.0f);
  private static final double CHARGE_JINGLI_COST =
      BehaviorConfigAccess.getFloat(HuaShiGuOrganBehavior.class, "CHARGE_JINGLI_COST", 6.0f);
  private static final int CHARGE_COOLDOWN_TICKS = 8 * 20;
  private static final double CHARGE_DISTANCE =
      BehaviorConfigAccess.getFloat(HuaShiGuOrganBehavior.class, "CHARGE_DISTANCE", 4.5f);
  private static final double CHARGE_KNOCKBACK =
      BehaviorConfigAccess.getFloat(HuaShiGuOrganBehavior.class, "CHARGE_KNOCKBACK", 1.2f);
  private static final int CHARGE_STUN_TICKS = 3;
  private static final int CHARGE_CHAIN_WINDOW_TICKS = 24; // 1.2 秒
  private static final double CHAIN_DISTANCE_FACTOR = 0.8;
  private static final double CHAIN_DAMAGE_FACTOR = 1.2;
  // 铁手擒拿蛊协同：0.8s内副手短按可定身小型单位，以下常量分别控制窗口与定身强度。
  private static final int GRAB_WINDOW_TICKS = 16;
  private static final int GRAB_LOCK_TICKS = 8;
  private static final int GRAB_LOCK_LEVEL = 6;

  private static final double HOOFQUAKE_BASE_ZHENYUAN_COST =
      BehaviorConfigAccess.getFloat(
          HuaShiGuOrganBehavior.class, "HOOFQUAKE_BASE_ZHENYUAN_COST", 120.0f);
  private static final double HOOFQUAKE_JINGLI_COST =
      BehaviorConfigAccess.getFloat(HuaShiGuOrganBehavior.class, "HOOFQUAKE_JINGLI_COST", 8.0f);
  private static final int HOOFQUAKE_COOLDOWN_TICKS = 12 * 20;
  private static final double HOOFQUAKE_RADIUS =
      BehaviorConfigAccess.getFloat(HuaShiGuOrganBehavior.class, "HOOFQUAKE_RADIUS", 3.0f);
  private static final double HOOFQUAKE_KNOCKBACK =
      BehaviorConfigAccess.getFloat(HuaShiGuOrganBehavior.class, "HOOFQUAKE_KNOCKBACK", 0.6f);
  private static final int HOOFQUAKE_SLOW_DURATION_TICKS = 40;
  private static final int HOOFQUAKE_SLOW_AMPLIFIER = 0;
  private static final int HOOFQUAKE_STAGGER_EXTRA_TICKS = 8; // 0.4s for stage 4

  private static final double OVERLOAD_BASE_ZHENYUAN_COST =
      BehaviorConfigAccess.getFloat(
          HuaShiGuOrganBehavior.class, "OVERLOAD_BASE_ZHENYUAN_COST", 150.0f);
  private static final double OVERLOAD_JINGLI_COST =
      BehaviorConfigAccess.getFloat(HuaShiGuOrganBehavior.class, "OVERLOAD_JINGLI_COST", 10.0f);
  private static final int OVERLOAD_COOLDOWN_TICKS = 18 * 20;
  private static final int OVERLOAD_CHARGE_TICKS = 4 * 20;
  private static final int OVERLOAD_BASE_STACK_MAX = 6;
  private static final int OVERLOAD_STAGE3_BONUS = 2;
  private static final double OVERLOAD_DAMAGE_PER_STACK = 0.10;
  private static final double OVERLOAD_KB_PER_STACK = 0.10;
  private static final int OVERLOAD_WEAKNESS_TICKS = 40;
  private static final int OVERLOAD_WEAKNESS_LEVEL = 0;

  private static final double KNOCKBACK_SAMPLE_TO_P3 = 12.0;
  private static final double RUN_DISTANCE_PER_POINT = 80.0;
  private static final int COMBO_WINDOW_TICKS = 3 * 20;
  private static final int CLEAN_TRASH_WINDOW_TICKS = 8 * 20;
  private static final int CLEAN_TRASH_RESET_TICKS = 30 * 20;

  private static final int P1_THRESHOLD = 200;
  private static final int P2_THRESHOLD = 260;
  private static final int P3_THRESHOLD = 320;
  private static final int P4_THRESHOLD = 380;

  private static final double SMALL_MOB_BOUND = 1.1D;

  private static final Map<Integer, StageModifier> STAGE_MODIFIERS = buildStageModifiers();

  private static final String TOAST_CHARGE_READY = "【花豕蛊】野冲冷却完毕";
  private static final String TOAST_HOOFQUAKE_READY = "【花豕蛊】蹄震冷却完毕";
  private static final String TOAST_OVERLOAD_READY = "【花豕蛊】负重爆发冷却完毕";

  static {
    OrganActivationListeners.register(CHARGE_ABILITY_ID, HuaShiGuOrganBehavior::activateCharge);
    OrganActivationListeners.register(
        HOOFQUAKE_ABILITY_ID, HuaShiGuOrganBehavior::activateHoofquake);
    OrganActivationListeners.register(OVERLOAD_ABILITY_ID, HuaShiGuOrganBehavior::activateOverload);
  }

  private HuaShiGuOrganBehavior() {}

  // -----------------------------------------------------------------------------------------
  // Listener implementation
  // -----------------------------------------------------------------------------------------

  @Override
  public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
    if (!(entity instanceof Player player) || entity.level().isClientSide()) {
      return;
    }
    if (!matchesOrgan(organ, ORGAN_ID) || !isPrimaryOrgan(cc, organ)) {
      return;
    }

    Level level = entity.level();
    OrganState state = organState(organ, STATE_ROOT);
    MultiCooldown cooldown = createCooldown(cc, organ);
    long gameTime = level.getGameTime();

    // 被动资源循环（牛劲回息）
    tickPassive(player, state, cooldown, gameTime);
    // 位移采样用于阶段点数判定
    sampleMovement(player, state);
    // 清理连击与无伤窗口
    decayCombo(state, cooldown, gameTime);
    decayP4Window(player, state, cooldown, gameTime);
    // 评估阶段晋升
    updateStageIfNeeded(player, cc, organ, state);
  }

  @Override
  public float onHit(
      DamageSource source,
      LivingEntity attacker,
      LivingEntity target,
      ChestCavityInstance cc,
      ItemStack organ,
      float damage) {
    if (!(attacker instanceof Player player)
        || attacker.level().isClientSide()
        || !matchesOrgan(organ, ORGAN_ID)
        || !isPrimaryOrgan(cc, organ)) {
      return damage;
    }
    if (!CombatEntityUtil.isMeleeHit(source) || !CombatEntityUtil.areEnemies(attacker, target)) {
      return damage;
    }

    OrganState state = organState(organ, STATE_ROOT);
    MultiCooldown cooldown = createCooldown(cc, organ);
    long now = attacker.level().getGameTime();

    recordP1Melee(player, target, state, now);
    recordCombo(player, state, cooldown, now);
    handleOverloadOnHit(player, target, cc, state, cooldown, now, damage);
    checkExecution(player, target, state);

    if (state.getBoolean(KEY_SYNERGY_HEAVY_READY, false)) {
      Vec3 direction = target.position().subtract(player.position()).normalize();
      target.push(direction.x * 0.2D, 0.05D, direction.z * 0.2D);
      state.setBoolean(KEY_SYNERGY_HEAVY_READY, false);
    }

    return damage;
  }

  @Override
  public float onIncomingDamage(
      DamageSource source,
      LivingEntity victim,
      ChestCavityInstance cc,
      ItemStack organ,
      float damage) {
    if (!(victim instanceof Player player)
        || victim.level().isClientSide()
        || !matchesOrgan(organ, ORGAN_ID)
        || !isPrimaryOrgan(cc, organ)) {
      return damage;
    }

    OrganState state = organState(organ, STATE_ROOT);
    long now = victim.level().getGameTime();
    state.setLong(KEY_LAST_DAMAGE_TICK, now, value -> Math.max(0L, value), 0L);
    P4_DAMAGE_FLAGS.put(player.getUUID(), 1);

    sampleKnockbackResistance(player, state);
    resetP4CleanseIfNeeded(state, now);
    return damage;
  }

  // -----------------------------------------------------------------------------------------
  // Passive + stage tracking helpers
  // -----------------------------------------------------------------------------------------

  private void tickPassive(Player player, OrganState state, MultiCooldown cooldown, long now) {
    MultiCooldown.Entry passiveEntry =
        cooldown
            .entry(KEY_PASSIVE_READY_TICK)
            .withDefault(now)
            .withClamp(value -> Math.max(0L, value));
    if (!passiveEntry.isReady(now)) {
      return;
    }
    passiveEntry.setReadyAt(now + PASSIVE_INTERVAL_TICKS);

    // 资源结算：先按阶段缩放真元，成功后扣减精力
    Optional<ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
    if (handleOpt.isEmpty()) {
      return;
    }
    ResourceHandle handle = handleOpt.get();
    OptionalDouble consumed =
        ResourceOps.tryConsumeScaledZhenyuan(handle, PASSIVE_BASE_ZHENYUAN_COST);
    if (consumed.isEmpty()) {
      return;
    }
    OptionalDouble adjusted = handle.adjustJingli(PASSIVE_JINGLI_GAIN, true);
    if (adjusted.isEmpty()) {
      ResourceOps.tryReplenishScaledZhenyuan(handle, consumed.getAsDouble(), false);
      return;
    }

    state.setInt(KEY_P4, state.getInt(KEY_P4, 0) + 1, value -> Math.max(0, value), 0);
    updateStageIfNeeded(player, null, null, state);
  }

  private void sampleMovement(Player player, OrganState state) {
    Vec3 pos = player.position();
    double lastX = state.getDouble(KEY_LAST_POS_X, pos.x);
    double lastZ = state.getDouble(KEY_LAST_POS_Z, pos.z);
    double dx = pos.x - lastX;
    double dz = pos.z - lastZ;
    double horizontal = Math.sqrt(dx * dx + dz * dz);
    state.setDouble(KEY_LAST_POS_X, pos.x, value -> value, pos.x);
    state.setDouble(KEY_LAST_POS_Z, pos.z, value -> value, pos.z);
    if (horizontal <= 0.02D) {
      return;
    }

    if (player.isSprinting() || player.isPassenger()) {
      double accumulated = state.getDouble("RunAccum", 0.0D) + horizontal;
      if (accumulated >= RUN_DISTANCE_PER_POINT) {
        int extra = (int) (accumulated / RUN_DISTANCE_PER_POINT);
        state.setInt(KEY_P1, state.getInt(KEY_P1, 0) + extra, value -> Math.max(0, value), 0);
        accumulated -= extra * RUN_DISTANCE_PER_POINT;
        updateStageIfNeeded(player, null, null, state);
      }
      state.setDouble("RunAccum", accumulated, value -> Math.max(0.0D, value), 0.0D);
    }
  }

  private void decayCombo(OrganState state, MultiCooldown cooldown, long now) {
    MultiCooldown.Entry comboWindow = cooldown.entry(KEY_COMBO_WINDOW_TICK).withDefault(0L);
    if (!comboWindow.isReady(now)) {
      return;
    }
    if (state.getInt(KEY_COMBO_COUNT, 0) > 0) {
      state.setInt(KEY_COMBO_COUNT, 0, value -> Math.max(0, value), 0);
    }
  }

  private void decayP4Window(Player player, OrganState state, MultiCooldown cooldown, long now) {
    MultiCooldown.Entry window = cooldown.entry(KEY_P4_WINDOW_TICK).withDefault(0L);
    if (!window.isReady(now)) {
      return;
    }
    if (state.getInt(KEY_P4_COUNT, 0) > 0) {
      state.setInt(KEY_P4_COUNT, 0, value -> Math.max(0, value), 0);
    }
    if (P4_DAMAGE_FLAGS.containsKey(player.getUUID())) {
      P4_DAMAGE_FLAGS.removeInt(player.getUUID());
    }
  }

  private void recordP1Melee(Player player, LivingEntity target, OrganState state, long now) {
    Object2LongMap<UUID> inner =
        P1_HIT_DEDUP.computeIfAbsent(player.getUUID(), uuid -> new Object2LongOpenHashMap<>());
    long nextAllowed = inner.getOrDefault(target.getUUID(), 0L);
    if (now < nextAllowed) {
      return;
    }
    inner.put(target.getUUID(), now + 20L);
    state.setInt(KEY_P1, state.getInt(KEY_P1, 0) + 1, value -> Math.max(0, value), 0);
    updateStageIfNeeded(player, null, null, state);
  }

  private void recordCombo(Player player, OrganState state, MultiCooldown cooldown, long now) {
    int current = state.getInt(KEY_COMBO_COUNT, 0) + 1;
    state.setInt(KEY_COMBO_COUNT, current, value -> Math.max(0, value), 0);
    cooldown.entry(KEY_COMBO_WINDOW_TICK).setReadyAt(now + COMBO_WINDOW_TICKS);
    if (current >= 5) {
      state.setInt(KEY_P4, state.getInt(KEY_P4, 0) + 2, value -> Math.max(0, value), 0);
      state.setInt(KEY_COMBO_COUNT, 0, value -> Math.max(0, value), 0);
      updateStageIfNeeded(player, null, null, state);
    }
  }

  private void recordChargeHit(Player player, OrganState state) {
    state.setInt(KEY_P1, state.getInt(KEY_P1, 0) + 2, value -> Math.max(0, value), 0);
    updateStageIfNeeded(player, null, null, state);
  }

  private void recordHoofquakeMulti(Player player, OrganState state) {
    state.setInt(KEY_P2, state.getInt(KEY_P2, 0) + 3, value -> Math.max(0, value), 0);
    updateStageIfNeeded(player, null, null, state);
  }

  private void recordFullEnergyHit(Player player, OrganState state) {
    state.setInt(KEY_P2, state.getInt(KEY_P2, 0) + 1, value -> Math.max(0, value), 0);
    updateStageIfNeeded(player, null, null, state);
  }

  private void recordKnockbackContribution(Player player, OrganState state, double distance) {
    if (distance <= 0.0D) {
      return;
    }
    double accum = state.getDouble("P3KnockbackAccum", 0.0D) + distance;
    while (accum >= KNOCKBACK_SAMPLE_TO_P3) {
      accum -= KNOCKBACK_SAMPLE_TO_P3;
      state.setInt(KEY_P3, state.getInt(KEY_P3, 0) + 1, value -> Math.max(0, value), 0);
    }
    state.setDouble("P3KnockbackAccum", accum, value -> Math.max(0.0D, value), 0.0D);
    updateStageIfNeeded(player, null, null, state);
  }

  private void recordP3Overload(Player player, OrganState state) {
    state.setInt(KEY_P3, state.getInt(KEY_P3, 0) + 2, value -> Math.max(0, value), 0);
    updateStageIfNeeded(player, null, null, state);
  }

  private void recordExecution(Player player, OrganState state) {
    state.setInt(KEY_P3, state.getInt(KEY_P3, 0) + 2, value -> Math.max(0, value), 0);
    updateStageIfNeeded(player, null, null, state);
  }

  private void recordCleanTrash(Player player, OrganState state, MultiCooldown cooldown, long now) {
    if (P4_DAMAGE_FLAGS.getOrDefault(player.getUUID(), 0) != 0) {
      return;
    }
    MultiCooldown.Entry window = cooldown.entry(KEY_P4_WINDOW_TICK).withDefault(0L);
    if (!window.isReady(now)) {
      int count = state.getInt(KEY_P4_COUNT, 0) + 1;
      state.setInt(KEY_P4_COUNT, count, value -> Math.max(0, value), 0);
      if (count >= 4) {
        state.setInt(KEY_P4, state.getInt(KEY_P4, 0) + 3, value -> Math.max(0, value), 0);
        state.setInt(KEY_P4_COUNT, 0, value -> Math.max(0, value), 0);
        window.setReadyAt(now + CLEAN_TRASH_RESET_TICKS);
        updateStageIfNeeded(player, null, null, state);
      }
      return;
    }

    state.setInt(KEY_P4_COUNT, 1, value -> Math.max(0, value), 0);
    window.setReadyAt(now + CLEAN_TRASH_WINDOW_TICKS);
  }

  private void sampleKnockbackResistance(Player player, OrganState state) {
    Vec3 motion = player.getDeltaMovement();
    double horizontal = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
    if (horizontal < 0.1D) {
      state.setInt(KEY_P2, state.getInt(KEY_P2, 0) + 1, value -> Math.max(0, value), 0);
      updateStageIfNeeded(player, null, null, state);
    }
  }

  private void resetP4CleanseIfNeeded(OrganState state, long now) {
    state.setInt(KEY_P4_COUNT, 0, value -> Math.max(0, value), 0);
    state.setLong(KEY_P4_WINDOW_TICK, now, value -> Math.max(0L, value), 0L);
  }

  private void checkExecution(Player player, LivingEntity target, OrganState state) {
    if (target == null || !target.isAlive()) {
      return;
    }
    float maxHealth = target.getMaxHealth();
    if (maxHealth <= 0.0f) {
      return;
    }
    if (target.getHealth() / maxHealth <= 0.15f && !(target instanceof Player)) {
      recordExecution(player, state);
    }
  }

  private void updateStageIfNeeded(
      Player player, ChestCavityInstance cc, ItemStack organ, OrganState state) {
    int stage = Mth.clamp(state.getInt(KEY_STAGE, 1), 1, 5);
    int p1 = state.getInt(KEY_P1, 0);
    int p2 = state.getInt(KEY_P2, 0);
    int p3 = state.getInt(KEY_P3, 0);
    int p4 = state.getInt(KEY_P4, 0);
    int newStage = stage;
    if (stage == 1 && p1 >= P1_THRESHOLD) {
      newStage = 2;
    } else if (stage == 2 && p2 >= P2_THRESHOLD) {
      newStage = 3;
    } else if (stage == 3 && p3 >= P3_THRESHOLD) {
      newStage = 4;
    } else if (stage == 4 && p4 >= P4_THRESHOLD) {
      newStage = 5;
    }

    if (newStage != stage) {
      state.setInt(KEY_STAGE, newStage, value -> Mth.clamp(value, 1, 5), 1);
      if (player instanceof ServerPlayer serverPlayer) {
        NetworkUtil.sendOrganSlotUpdate(cc, organ == null ? findOrgan(cc) : organ);
        serverPlayer.displayClientMessage(
            net.minecraft.network.chat.Component.literal("花豕蛊蜕变至" + newStage + "转"), true);
      }
      LOGGER.debug(
          "{} advanced 花豕蛊 stage from {} to {} (P1={}, P2={}, P3={}, P4={})",
          player.getName().getString(),
          stage,
          newStage,
          p1,
          p2,
          p3,
          p4);
    }
  }

  private static Map<Integer, StageModifier> buildStageModifiers() {
    Map<Integer, StageModifier> map = new HashMap<>();
    map.put(1, new StageModifier(0.0, 0.0, 0));
    map.put(2, new StageModifier(0.0, 0.5, 0));
    map.put(3, new StageModifier(0.0, 0.5, OVERLOAD_STAGE3_BONUS));
    map.put(4, new StageModifier(0.5, 0.5, OVERLOAD_STAGE3_BONUS));
    map.put(5, new StageModifier(0.5, 0.5, OVERLOAD_STAGE3_BONUS));
    return map;
  }

  private StageModifier modifier(OrganState state) {
    int stage = Mth.clamp(state.getInt(KEY_STAGE, 1), 1, 5);
    return STAGE_MODIFIERS.getOrDefault(stage, STAGE_MODIFIERS.get(1));
  }

  // -----------------------------------------------------------------------------------------
  // Ability implementations
  // -----------------------------------------------------------------------------------------

  private static void activateCharge(LivingEntity entity, ChestCavityInstance cc) {
    if (!(entity instanceof Player player) || player.level().isClientSide()) {
      return;
    }
    ItemStack organ = findOrgan(cc);
    if (organ.isEmpty()) {
      return;
    }
    OrganState state = INSTANCE.organState(organ, STATE_ROOT);
    MultiCooldown cooldown = INSTANCE.createCooldown(cc, organ);
    long now = player.level().getGameTime();
    MultiCooldown.Entry ready = cooldown.entry(KEY_CHARGE_READY_TICK).withDefault(0L);
    StageModifier modifier = INSTANCE.modifier(state);
    double distance = CHARGE_DISTANCE + modifier.chargeRangeBonus();
    boolean allowChain = INSTANCE.isStageFive(state);
    boolean hasGrab = INSTANCE.hasOrgan(cc, TIE_SHOU_QIN_NA_GU_ID);

    boolean usingChain = false;
    if (allowChain) {
      MultiCooldown.Entry chainWindow = cooldown.entry(KEY_CHAIN_READY_TICK).withDefault(0L);
      if (chainWindow.isReady(now)) {
        chainWindow.setReadyAt(now + CHARGE_CHAIN_WINDOW_TICKS);
      } else if (chainWindow.getReadyTick() > now) {
        usingChain = true;
        distance *= CHAIN_DISTANCE_FACTOR;
        chainWindow.setReadyAt(now);
      }
    }

    if (!usingChain && !ready.isReady(now)) {
      return;
    }

    Optional<ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
    if (handleOpt.isEmpty()) {
      return;
    }
    ResourceHandle handle = handleOpt.get();
    OptionalDouble consumed =
        ResourceOps.tryConsumeScaledZhenyuan(handle, CHARGE_BASE_ZHENYUAN_COST);
    if (consumed.isEmpty()) {
      return;
    }
    OptionalDouble jingli = handle.adjustJingli(-CHARGE_JINGLI_COST, true);
    if (jingli.isEmpty()) {
      ResourceOps.tryReplenishScaledZhenyuan(handle, consumed.getAsDouble(), false);
      return;
    }

    Vec3 look = player.getLookAngle();
    Vec3 horizontal = new Vec3(look.x, 0.0D, look.z);
    if (horizontal.lengthSqr() < 1.0E-4D) {
      horizontal = new Vec3(player.getLookAngle().x, 0.0D, player.getLookAngle().z);
    }
    Vec3 direction = horizontal.normalize();
    Vec3 targetPos = player.position().add(direction.scale(distance));
    Vec3 clipped = clipToSolid(player.level(), player.position(), targetPos, player);

    Vec3 velocity = clipped.subtract(player.position());
    if (velocity.lengthSqr() > 0.0001D) {
      player.setDeltaMovement(velocity.normalize().scale(0.9D).add(0.0D, 0.1D, 0.0D));
      player.hurtMarked = true;
    }

    LivingEntity hit = findChargeHit(player, clipped);
    if (hit != null) {
      performChargeImpact(player, hit, usingChain, hasGrab, state, now);
      INSTANCE.recordChargeHit(player, state);
      INSTANCE.recordKnockbackContribution(player, state, CHARGE_KNOCKBACK);
      INSTANCE.recordCleanTrashIfSmall(player, state, cooldown, now, hit);
    }

    if (!usingChain) {
      long readyAt = now + CHARGE_COOLDOWN_TICKS;
      ready.setReadyAt(readyAt);
      if (player instanceof ServerPlayer serverPlayer) {
        ActiveSkillRegistry.scheduleReadyToast(serverPlayer, CHARGE_ABILITY_ID, readyAt, now);
      }
    }

    INSTANCE.triggerOverdrive(player, cc, state, handle, now);
    player.swing(InteractionHand.MAIN_HAND, true);
    player.playSound(SoundEvents.WARDEN_ATTACK_IMPACT, 0.9f, 0.75f);
  }

  private static void activateHoofquake(LivingEntity entity, ChestCavityInstance cc) {
    if (!(entity instanceof Player player) || player.level().isClientSide()) {
      return;
    }
    ItemStack organ = findOrgan(cc);
    if (organ.isEmpty()) {
      return;
    }
    OrganState state = INSTANCE.organState(organ, STATE_ROOT);
    MultiCooldown cooldown = INSTANCE.createCooldown(cc, organ);
    long now = player.level().getGameTime();
    MultiCooldown.Entry ready = cooldown.entry(KEY_HOOFQUAKE_READY_TICK).withDefault(0L);
    if (!ready.isReady(now)) {
      return;
    }

    Optional<ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
    if (handleOpt.isEmpty()) {
      return;
    }
    ResourceHandle handle = handleOpt.get();
    OptionalDouble consumed =
        ResourceOps.tryConsumeScaledZhenyuan(handle, HOOFQUAKE_BASE_ZHENYUAN_COST);
    if (consumed.isEmpty()) {
      return;
    }
    OptionalDouble jingli = handle.adjustJingli(-HOOFQUAKE_JINGLI_COST, true);
    if (jingli.isEmpty()) {
      ResourceOps.tryReplenishScaledZhenyuan(handle, consumed.getAsDouble(), false);
      return;
    }

    StageModifier modifier = INSTANCE.modifier(state);
    double radius = HOOFQUAKE_RADIUS + modifier.hoofquakeRadiusBonus();
    boolean hasHanYue = INSTANCE.hasOrgan(cc, HAN_YUE_GU_ID);
    if (hasHanYue) {
      radius += 1.0D;
    }
    int stage = state.getInt(KEY_STAGE, 1);
    boolean stage4Plus = stage >= 4;

    List<LivingEntity> targets = collectTargets(player, radius);
    if (!targets.isEmpty() && targets.size() >= 3) {
      INSTANCE.recordHoofquakeMulti(player, state);
    }

    for (LivingEntity target : targets) {
      if (!CombatEntityUtil.areEnemies(player, target)) {
        continue;
      }
      applyHoofquakeImpact(player, target, radius, stage4Plus, hasHanYue);
      INSTANCE.recordKnockbackContribution(player, state, HOOFQUAKE_KNOCKBACK);
      INSTANCE.recordCleanTrashIfSmall(player, state, cooldown, now, target);
    }

    long readyAt = now + HOOFQUAKE_COOLDOWN_TICKS;
    ready.setReadyAt(readyAt);
    if (player instanceof ServerPlayer serverPlayer) {
      ActiveSkillRegistry.scheduleReadyToast(serverPlayer, HOOFQUAKE_ABILITY_ID, readyAt, now);
    }

    INSTANCE.triggerOverdrive(player, cc, state, handle, now);
    spawnHoofquakeParticles(player, radius);
    player.playSound(SoundEvents.IRON_GOLEM_ATTACK, 1.0f, 0.8f);
  }

  private static void activateOverload(LivingEntity entity, ChestCavityInstance cc) {
    if (!(entity instanceof Player player) || player.level().isClientSide()) {
      return;
    }
    ItemStack organ = findOrgan(cc);
    if (organ.isEmpty()) {
      return;
    }
    OrganState state = INSTANCE.organState(organ, STATE_ROOT);
    MultiCooldown cooldown = INSTANCE.createCooldown(cc, organ);
    long now = player.level().getGameTime();
    MultiCooldown.Entry ready = cooldown.entry(KEY_OVERLOAD_READY_TICK).withDefault(0L);
    if (!ready.isReady(now)) {
      return;
    }

    Optional<ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
    if (handleOpt.isEmpty()) {
      return;
    }
    ResourceHandle handle = handleOpt.get();
    OptionalDouble consumed =
        ResourceOps.tryConsumeScaledZhenyuan(handle, OVERLOAD_BASE_ZHENYUAN_COST);
    if (consumed.isEmpty()) {
      return;
    }
    OptionalDouble jingli = handle.adjustJingli(-OVERLOAD_JINGLI_COST, true);
    if (jingli.isEmpty()) {
      ResourceOps.tryReplenishScaledZhenyuan(handle, consumed.getAsDouble(), false);
      return;
    }

    StageModifier modifier = INSTANCE.modifier(state);
    int maxStacks = OVERLOAD_BASE_STACK_MAX + Math.max(0, modifier.extraOverloadStacks());
    state.setInt(KEY_OVERLOAD_STACKS, 0, value -> Mth.clamp(value, 0, maxStacks), 0);
    state.setBoolean(KEY_OVERLOAD_PENDING, true);
    cooldown.entry(KEY_OVERLOAD_WINDOW_TICK).setReadyAt(now + OVERLOAD_CHARGE_TICKS);
    long readyAt = now + OVERLOAD_COOLDOWN_TICKS;
    ready.setReadyAt(readyAt);
    if (player instanceof ServerPlayer serverPlayer) {
      ActiveSkillRegistry.scheduleReadyToast(serverPlayer, OVERLOAD_ABILITY_ID, readyAt, now);
    }

    INSTANCE.triggerOverdrive(player, cc, state, handle, now);
    player.playSound(SoundEvents.ANVIL_PLACE, 0.9f, 0.7f);
  }

  private void handleOverloadOnHit(
      Player player,
      LivingEntity target,
      ChestCavityInstance cc,
      OrganState state,
      MultiCooldown cooldown,
      long now,
      float baseDamage) {
    if (!state.getBoolean(KEY_OVERLOAD_PENDING, false)) {
      return;
    }

    MultiCooldown.Entry window = cooldown.entry(KEY_OVERLOAD_WINDOW_TICK).withDefault(0L);
    StageModifier stageModifier = modifier(state);
    int maxStacks = OVERLOAD_BASE_STACK_MAX + Math.max(0, stageModifier.extraOverloadStacks());

    if (!window.isReady(now)) {
      int stacks = Mth.clamp(state.getInt(KEY_OVERLOAD_STACKS, 0) + 1, 0, maxStacks);
      state.setInt(KEY_OVERLOAD_STACKS, stacks, value -> Mth.clamp(value, 0, maxStacks), 0);
      return;
    }

    int stacks = Mth.clamp(state.getInt(KEY_OVERLOAD_STACKS, 0), 0, maxStacks);
    if (stacks <= 0) {
      state.setBoolean(KEY_OVERLOAD_PENDING, false);
      return;
    }

    double multiplier = 1.0 + stacks * OVERLOAD_DAMAGE_PER_STACK;
    float extraDamage = (float) (baseDamage * (multiplier - 1.0));
    if (extraDamage > 0.0f) {
      if (DamagePipeline.active()) {
        DamagePipeline.addBonus(extraDamage);
      } else if (target.level() instanceof ServerLevel serverLevel) {
        serverLevel
            .getServer()
            .execute(
                () -> {
                  if (target.isAlive()) {
                    target.hurt(
                        GRDamageSources.organInternal(serverLevel),
                        extraDamage);
                  }
                });
      }
    }
    double knockback = stacks * OVERLOAD_KB_PER_STACK;
    Vec3 direction = target.position().subtract(player.position()).normalize();
    target.push(direction.x * knockback, 0.35D, direction.z * knockback);
    if (stacks >= 3) {
      target.addEffect(
          new MobEffectInstance(
              MobEffects.WEAKNESS,
              OVERLOAD_WEAKNESS_TICKS,
              OVERLOAD_WEAKNESS_LEVEL,
              false,
              true,
              true));
    }
    if (hasOrgan(cc, LIE_ZHUA_GU_ID)) {
      target.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 0, false, true, true));
    }
    recordKnockbackContribution(player, state, knockback);
    recordP3Overload(player, state);
    recordCleanTrashIfSmall(player, state, cooldown, now, target);

    if (hasOrgan(cc, QING_NIU_LAO_LI_GU_ID)) {
      player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 120, 0, false, true, true));
    }

    state.setBoolean(KEY_OVERLOAD_PENDING, false);
    state.setInt(KEY_OVERLOAD_STACKS, 0, value -> 0, 0);
  }

  private void recordCleanTrashIfSmall(
      Player player, OrganState state, MultiCooldown cooldown, long now, LivingEntity target) {
    if (!(target instanceof Mob mob) || mob.getBbWidth() > SMALL_MOB_BOUND) {
      return;
    }
    recordCleanTrash(player, state, cooldown, now);
  }

  private void triggerOverdrive(
      Player player, ChestCavityInstance cc, OrganState state, ResourceHandle handle, long now) {
    OptionalDouble consumed = ResourceOps.tryConsumeScaledZhenyuan(handle, OVERDRIVE_BASE_COST);
    if (consumed.isEmpty()) {
      return;
    }
    player.addEffect(
        new MobEffectInstance(
            MobEffects.DAMAGE_BOOST,
            OVERDRIVE_DURATION_TICKS,
            Math.max(0, OVERDRIVE_STRENGTH_LEVEL - 1),
            false,
            true,
            true));
    if (hasOrgan(cc, JING_LI_GU_ID)) {
      state.setBoolean(KEY_SYNERGY_HEAVY_READY, true);
    }
  }

  private static void clearGrabWindow(OrganState state) {
    state.setInt(KEY_GRAB_TARGET_ID, -1, value -> value, -1);
    state.setLong(KEY_GRAB_WINDOW_TICK, 0L, value -> Math.max(0L, value), 0L);
  }

  public boolean tryTriggerGrabFollowUp(
      Player player, ChestCavityInstance cc, InteractionHand hand) {
    if (player == null
        || cc == null
        || hand != InteractionHand.OFF_HAND
        || player.level().isClientSide()) {
      return false;
    }

    Level level = player.level();
    if (!(level instanceof ServerLevel serverLevel)) {
      return false;
    }

    ItemStack organ = findOrgan(cc);
    if (organ.isEmpty()) {
      return false;
    }

    OrganState state = organState(organ, STATE_ROOT);
    long now = level.getGameTime();
    long expireTick = state.getLong(KEY_GRAB_WINDOW_TICK, 0L);
    if (expireTick <= now) {
      clearGrabWindow(state);
      return false;
    }

    int targetId = state.getInt(KEY_GRAB_TARGET_ID, -1);
    if (targetId <= 0) {
      clearGrabWindow(state);
      return false;
    }

    var targetEntity = serverLevel.getEntity(targetId);
    if (!(targetEntity instanceof LivingEntity target) || !target.isAlive()) {
      clearGrabWindow(state);
      return false;
    }

    if (target instanceof Player || target.getBbWidth() > SMALL_MOB_BOUND) {
      clearGrabWindow(state);
      return false;
    }

    // 定身时略微衰减移动向量并轻微上挑，突出“擒拿”反馈。
    target.setDeltaMovement(target.getDeltaMovement().multiply(0.2D, 0.2D, 0.2D));
    target.push(0.0D, 0.05D, 0.0D);
    target.addEffect(
        new MobEffectInstance(
            MobEffects.MOVEMENT_SLOWDOWN, GRAB_LOCK_TICKS, GRAB_LOCK_LEVEL, false, true, true));
    clearGrabWindow(state);

    player.swing(hand, true);
    player.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 0.7f, 1.4f);
    return true;
  }

  private static Vec3 clipToSolid(Level level, Vec3 start, Vec3 end, Player player) {
    HitResult result =
        level.clip(
            new ClipContext(
                start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
    if (result.getType() == HitResult.Type.BLOCK) {
      return result.getLocation();
    }
    return end;
  }

  private static LivingEntity findChargeHit(Player player, Vec3 destination) {
    AABB box =
        player
            .getBoundingBox()
            .expandTowards(destination.subtract(player.position()))
            .inflate(0.5D);
    List<LivingEntity> list =
        player
            .level()
            .getEntitiesOfClass(
                LivingEntity.class,
                box,
                entity ->
                    entity != player
                        && entity.isAlive()
                        && CombatEntityUtil.areEnemies(player, entity));
    if (list.isEmpty()) {
      return null;
    }
    return list.stream()
        .min(Comparator.comparingDouble(entity -> entity.distanceToSqr(player)))
        .orElse(null);
  }

  private static double resolveAttackDamage(Player player) {
    if (player == null) {
      return 1.0D;
    }
    var attribute = player.getAttribute(Attributes.ATTACK_DAMAGE);
    return attribute == null ? 1.0D : Math.max(1.0D, attribute.getValue());
  }

  private static void performChargeImpact(
      Player player,
      LivingEntity target,
      boolean empowered,
      boolean hasGrabSynergy,
      OrganState state,
      long now) {
    double knockback = CHARGE_KNOCKBACK * (empowered ? CHAIN_DAMAGE_FACTOR : 1.0D);
    Vec3 direction = target.position().subtract(player.position()).normalize();
    target.push(direction.x * knockback, 0.3D, direction.z * knockback);
    double attackDamage = resolveAttackDamage(player);
    target.hurt(
        player.damageSources().playerAttack(player),
        (float) (attackDamage * (empowered ? CHAIN_DAMAGE_FACTOR : 1.0D)));
    target.addEffect(
        new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, CHARGE_STUN_TICKS, 1, false, true));
    if (hasGrabSynergy && target.getBbWidth() <= SMALL_MOB_BOUND && !(target instanceof Player)) {
      // 记录潜在抓取目标供 0.8s 内的副手短按回调使用。
      state.setInt(KEY_GRAB_TARGET_ID, target.getId(), value -> value, -1);
      state.setLong(
          KEY_GRAB_WINDOW_TICK, now + GRAB_WINDOW_TICKS, value -> Math.max(0L, value), 0L);
    } else {
      clearGrabWindow(state);
    }
    if (player instanceof ServerPlayer serverPlayer) {
      serverPlayer.playNotifySound(
          SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 1.0f, 0.8f);
    }
  }

  private static List<LivingEntity> collectTargets(Player player, double radius) {
    AABB box = player.getBoundingBox().inflate(radius, radius, radius);
    return new ArrayList<>(
        player
            .level()
            .getEntitiesOfClass(
                LivingEntity.class,
                box,
                entity ->
                    entity != player
                        && entity.isAlive()
                        && CombatEntityUtil.areEnemies(player, entity)));
  }

  private static void applyHoofquakeImpact(
      Player player, LivingEntity target, double radius, boolean stage4Plus, boolean hasHanYue) {
    double distance = Math.max(0.2D, player.distanceTo(target));
    double scale = 1.0D - Math.min(distance / radius, 1.0D);
    Vec3 direction = target.position().subtract(player.position()).normalize();
    double knockback = HOOFQUAKE_KNOCKBACK * (0.5D + scale);
    target.push(direction.x * knockback, 0.25D, direction.z * knockback);
    double attackDamage = resolveAttackDamage(player);
    target.hurt(
        player.damageSources().playerAttack(player),
        (float) (attackDamage * (0.8F + scale * 0.4F)));
    int slowTicks = HOOFQUAKE_SLOW_DURATION_TICKS;
    if (stage4Plus) {
      slowTicks += HOOFQUAKE_STAGGER_EXTRA_TICKS;
    }
    target.addEffect(
        new MobEffectInstance(
            MobEffects.MOVEMENT_SLOWDOWN, slowTicks, HOOFQUAKE_SLOW_AMPLIFIER, false, true));
    if (hasHanYue) {
      target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 8, 1, false, true));
    }
  }

  private static void spawnHoofquakeParticles(Player player, double radius) {
    if (!(player.level() instanceof ServerLevel serverLevel)) {
      return;
    }
    BlockPos pos = player.blockPosition();
    for (int i = 0; i < 20; i++) {
      double angle = (Math.PI * 2 * i) / 20.0;
      double x = pos.getX() + 0.5 + Math.cos(angle) * radius;
      double z = pos.getZ() + 0.5 + Math.sin(angle) * radius;
      serverLevel.sendParticles(ParticleTypes.POOF, x, pos.getY(), z, 1, 0.05, 0.0, 0.05, 0.01);
    }
  }

  // -----------------------------------------------------------------------------------------
  // Utility helpers
  // -----------------------------------------------------------------------------------------

  private MultiCooldown createCooldown(ChestCavityInstance cc, ItemStack organ) {
    MultiCooldown.Builder builder =
        MultiCooldown.builder(OrganState.of(organ, STATE_ROOT))
            .withLongClamp(value -> Math.max(0L, value), 0L)
            .withIntClamp(value -> Math.max(0, value), 0);
    if (cc != null) {
      builder.withSync(cc, organ);
    } else {
      builder.withOrgan(organ);
    }
    return builder.build();
  }

  private static ItemStack findOrgan(ChestCavityInstance cc) {
    if (cc == null || cc.inventory == null) {
      return ItemStack.EMPTY;
    }
    int size = cc.inventory.getContainerSize();
    for (int i = 0; i < size; i++) {
      ItemStack stack = cc.inventory.getItem(i);
      if (INSTANCE.matchesOrgan(stack, ORGAN_ID)) {
        return stack;
      }
    }
    return ItemStack.EMPTY;
  }

  private boolean isPrimaryOrgan(ChestCavityInstance cc, ItemStack organ) {
    if (cc == null || cc.inventory == null || organ == null || organ.isEmpty()) {
      return false;
    }
    int size = cc.inventory.getContainerSize();
    for (int i = 0; i < size; i++) {
      ItemStack slotStack = cc.inventory.getItem(i);
      if (slotStack == null || slotStack.isEmpty()) {
        continue;
      }
      if (!matchesOrgan(slotStack, ORGAN_ID)) {
        continue;
      }
      return slotStack == organ;
    }
    return false;
  }

  private boolean isStageFive(OrganState state) {
    return state.getInt(KEY_STAGE, 1) >= 5;
  }

  private boolean hasOrgan(ChestCavityInstance cc, ResourceLocation id) {
    if (cc == null || cc.inventory == null) {
      return false;
    }
    int size = cc.inventory.getContainerSize();
    for (int i = 0; i < size; i++) {
      ItemStack stack = cc.inventory.getItem(i);
      if (matchesOrgan(stack, id)) {
        return true;
      }
    }
    return false;
  }

  private static ChestCavityInstance ccFromPlayer(Player player) {
    if (player instanceof net.tigereye.chestcavity.interfaces.ChestCavityEntity ccEntity) {
      return ccEntity.getChestCavityInstance();
    }
    return null;
  }

  private record StageModifier(
      double chargeRangeBonus, double hoofquakeRadiusBonus, int extraOverloadStacks) {}
}
