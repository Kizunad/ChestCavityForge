package net.tigereye.chestcavity.compat.guzhenren.item.jin_dao.behavior;

import com.mojang.logging.LogUtils;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.AbstractGuzhenrenOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.network.packets.TiePiProgressPayload;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.AttributeOps;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.FxOps;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.LedgerOps;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.OrganStateOps;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge.ResourceHandle;
import net.tigereye.chestcavity.linkage.LinkageChannel;
import net.tigereye.chestcavity.linkage.policy.ClampPolicy;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.listeners.OrganIncomingDamageListener;
import net.tigereye.chestcavity.listeners.OrganOnHitListener;
import net.tigereye.chestcavity.listeners.OrganRemovalListener;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.skill.ActiveSkillRegistry;
import org.slf4j.Logger;

/**
 * 铁皮蛊阶段/资源/联动骨架：
 *
 * <ul>
 *   <li>以 linkage channel {@code guzhenren:linkage/tiepi_sp} 存储阶段经验，并在 60 秒窗口内限制
 *       60 点增量；
 *   <li>管理硬化/铁壁/沉击/重拳沉坠四个主动技的冷却、成本和状态；
 *   <li>暴露基础同步数据到 {@link TiePiProgressPayload} 供客户端 HUD 使用。
 * </ul>
 */
public enum TiePiGuBehavior
    implements OrganSlowTickListener,
        OrganOnHitListener,
        OrganIncomingDamageListener,
        OrganRemovalListener {
  INSTANCE;

  private static final Logger LOGGER = LogUtils.getLogger();

  private static final String MOD_ID = "guzhenren";
  private static final ResourceLocation ORGAN_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "t_tie_pi_gu");

  public static final ResourceLocation HARDENING_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "tiepi/hardening");
  public static final ResourceLocation IRONWALL_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "tiepi/ironwall");
  public static final ResourceLocation HEAVY_BLOW_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "tiepi/heavy_blow");
  public static final ResourceLocation SLAM_FIST_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "tiepi/slam_fist");

  private static final ResourceLocation TIEPI_SP_CHANNEL =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/tiepi_sp");
  private static final ResourceLocation JIN_DAO_INCREASE_EFFECT =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/jin_dao_increase_effect");

  private static final ClampPolicy SP_CLAMP = new ClampPolicy(0.0, 4000.0);
  private static final ClampPolicy NON_NEGATIVE = new ClampPolicy(0.0, Double.MAX_VALUE);

  private static final String STATE_ROOT = "TiePiGu";
  private static final String KEY_PHASE = "Phase";
  private static final String KEY_UPGRADE_READY = "UpgradeReady";
  private static final String KEY_WINDOW_GAIN = "WindowGain";
  private static final String KEY_WINDOW_RESET = "WindowReset";
  private static final String KEY_ACTIVITY_TICK = "RecentActivityTick";
  private static final String KEY_HUNGER_LOCKED = "HungerLocked";
  private static final String KEY_FORCE_SYNC = "ForceSync";
  private static final String KEY_SYNERGY_COPPER = "SynergyCopper";
  private static final String KEY_SYNERGY_BONE = "SynergyBone";
  private static final String KEY_SYNERGY_BELL = "SynergyBell";
  private static final String KEY_SYNERGY_SWORD = "SynergySword";
  private static final String KEY_SYNERGY_LEI = "SynergyLei";
  private static final String KEY_HARDENING_ACTIVE = "HardeningActive";
  private static final String KEY_HEAVY_READY = "HeavyBlowReady";

  private static final String ENTRY_CD_HARDENING = "TiePi/CdHardening";
  private static final String ENTRY_CD_IRONWALL = "TiePi/CdIronwall";
  private static final String ENTRY_CD_HEAVY =
      "TiePi/CdHeavy"; // countdown entry for next activation
  private static final String ENTRY_CD_SLAM = "TiePi/CdSlam";
  private static final String ENTRY_HARDENING_END = "TiePi/HardeningEnd";
  private static final String ENTRY_HARDENING_TICK = "TiePi/HardeningTick";
  private static final String ENTRY_HARDENING_STAMINA = "TiePi/HardeningFood";
  private static final String ENTRY_HARDENING_SP = "TiePi/HardeningSp";
  private static final String ENTRY_IRONWALL_END = "TiePi/IronwallEnd";
  private static final String ENTRY_IRONWALL_ACCUM = "TiePi/IronwallAccum";
  private static final String ENTRY_HEAVY_TIMEOUT = "TiePi/HeavyTimeout";
  private static final String ENTRY_SYNC_TICK = "TiePi/NextSync";
  private static final String ENTRY_HUNGER_TICK = "TiePi/HungerDrain";
  private static final String ENTRY_BLOCK_MINE = "TiePi/BlockMine";

  private static final int MAX_PHASE = 5;
  private static final int[] PHASE_THRESHOLDS = {0, 120, 360, 900, 2000};
  private static final long WINDOW_INTERVAL_TICKS = 1200L;
  private static final double WINDOW_CAP = 60.0;
  private static final long SYNC_INTERVAL_TICKS = 40L;
  private static final long HUNGER_INTERVAL_TICKS = 200L;
  private static final long HARDENING_TICK_INTERVAL = 20L;
  private static final long HARDENING_FOOD_INTERVAL = 100L;
  private static final int HARDENING_MAX_SP_PER_CAST = 12;
  private static final long ACTIVITY_TIMEOUT_TICKS = 100L;
  private static final double IRONWALL_SP_PER_DAMAGE = 2.0 / 20.0; // +2 per 20 吸收

  private static final double HARDENING_BASE_ZHENYUAN = 60.0;
  private static final double IRONWALL_BASE_ZHENYUAN = 30.0;
  private static final double HEAVY_BLOW_BASE_ZHENYUAN = 25.0;
  private static final double SLAM_FIST_BASE_ZHENYUAN = 35.0;

  private static final ResourceLocation[] COPPER_SYNERGY_IDS =
      new ResourceLocation[] {
        ResourceLocation.fromNamespaceAndPath(MOD_ID, "tong_pi_gu"),
        ResourceLocation.fromNamespaceAndPath(MOD_ID, "tong_pi_gu_2"),
        ResourceLocation.fromNamespaceAndPath(MOD_ID, "tong_pi_gu_san_zhuan"),
        ResourceLocation.fromNamespaceAndPath(MOD_ID, "qingtonggu"),
        ResourceLocation.fromNamespaceAndPath(MOD_ID, "gutongpigu")
      };
  private static final ResourceLocation[] BONE_SYNERGY_IDS =
      new ResourceLocation[] {
        ResourceLocation.fromNamespaceAndPath(MOD_ID, "tie_gu_gu"),
        ResourceLocation.fromNamespaceAndPath(MOD_ID, "jingtiegugu")
      };
  private static final ResourceLocation[] BELL_SYNERGY_IDS =
      new ResourceLocation[] {ResourceLocation.fromNamespaceAndPath(MOD_ID, "jinzhonggu")};
  private static final ResourceLocation[] LEI_SYNERGY_IDS =
      new ResourceLocation[] {ResourceLocation.fromNamespaceAndPath(MOD_ID, "leidungu")};
  private static final ResourceLocation[] SWORD_SYNERGY_IDS =
      new ResourceLocation[] {
        ResourceLocation.fromNamespaceAndPath(MOD_ID, "jianfenggu"),
        ResourceLocation.fromNamespaceAndPath(MOD_ID, "jian_feng_gu_5"),
        ResourceLocation.fromNamespaceAndPath(MOD_ID, "jianzhigu"),
        ResourceLocation.fromNamespaceAndPath(MOD_ID, "jian_zhi_gu_3")
      };
  private static final ResourceLocation ATTACK_SPEED_MOD_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "tiepi/attack_speed");

  static {
    for (Ability ability : Ability.values()) {
      OrganActivationListeners.register(
          ability.abilityId, (entity, cc) -> activateAbility(entity, cc, ability));
    }
  }

  private TiePiGuBehavior() {}

  public void ensureAttached(ChestCavityInstance cc) {
    LedgerOps.ensureChannel(cc, TIEPI_SP_CHANNEL, SP_CLAMP);
    LedgerOps.ensureChannel(cc, JIN_DAO_INCREASE_EFFECT, NON_NEGATIVE);
  }

  @Override
  public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
    if (!(entity instanceof Player player)
        || cc == null
        || organ == null
        || organ.isEmpty()
        || player.level().isClientSide()) {
      return;
    }

    Level level = player.level();
    if (!(level instanceof ServerLevel serverLevel)) {
      return;
    }

    OrganState state = organState(organ, STATE_ROOT);
    MultiCooldown cooldown = cooldown(cc, organ, state);
    long now = serverLevel.getGameTime();

    refreshWindow(state, cc, organ, now);
    tickHunger(player, cc, organ, state, cooldown, now);
    tickHardening(player, cc, organ, state, cooldown, now);
    tickIronwall(player, cc, organ, state, cooldown, now);
    tickHeavyBlow(player, cc, organ, state, cooldown, now);
    trackSynergies(cc, organ, state);
    if (player instanceof ServerPlayer serverPlayer) {
      checkPhaseProgression(serverPlayer, cc, organ, state, now);
    }

    boolean forceSync = state.getBoolean(KEY_FORCE_SYNC, false);
    MultiCooldown.Entry syncEntry = cooldown.entry(ENTRY_SYNC_TICK);
    if (forceSync || now >= syncEntry.getReadyTick()) {
      syncEntry.setReadyAt(now + SYNC_INTERVAL_TICKS);
      OrganStateOps.setBoolean(state, cc, organ, KEY_FORCE_SYNC, false, false);
      syncProgress((ServerPlayer) player, cc, organ, state, cooldown, now);
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
    if (cc == null || organ == null || organ.isEmpty()) {
      return damage;
    }
    if (!(attacker instanceof ServerPlayer player) || attacker != cc.owner) {
      return damage;
    }
    OrganState state = organState(organ, STATE_ROOT);
    Level level = player.level();
    long now = level.getGameTime();
    markActivity(cc, organ, state, now);
    MultiCooldown cooldown = cooldown(cc, organ, state);

    if (state.getBoolean(KEY_HEAVY_READY, false)) {
      int phase = currentPhase(state);
      double multiplier = phase >= 3 ? 1.4 : 1.3;
      damage *= multiplier;
      MultiCooldown.Entry heavyTimeout = cooldown.entry(ENTRY_HEAVY_TIMEOUT);
      heavyTimeout.setReadyAt(0L);
      OrganStateOps.setBoolean(state, cc, organ, KEY_HEAVY_READY, false, false);
      awardSp(cc, organ, state, 4.0, now);
      cooldown.entry(ENTRY_CD_HEAVY).setReadyAt(now + heavyCooldownTicks());
      ActiveSkillRegistry.scheduleReadyToast(player, HEAVY_BLOW_ID, now + heavyCooldownTicks(), now);
      emitHeavyImpactFx(player, target);
    }

    if (state.getBoolean(KEY_HARDENING_ACTIVE, false) && hasSwordSynergy(cc)) {
      applyAttackSpeedPulse(player);
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
    if (cc == null || organ == null || organ.isEmpty()) {
      return damage;
    }
    if (!(victim instanceof Player player) || victim != cc.owner) {
      return damage;
    }
    OrganState state = organState(organ, STATE_ROOT);
    Level level = player.level();
    long now = level.getGameTime();
    markActivity(cc, organ, state, now);
    MultiCooldown cooldown = cooldown(cc, organ, state);

    if (state.getBoolean(KEY_HARDENING_ACTIVE, false)) {
      damage = applyHardeningReduction(damage, currentPhase(state));
    }

    if (isIronwallActive(state, cooldown, now)) {
      float reduced =
          (float) Mth.clamp(damage * (1.0 - ironwallReduction(currentPhase(state))), 0.0, damage);
      double absorbed = damage - reduced;
      damage = reduced;
      double accumulator =
          state.getDouble(ENTRY_IRONWALL_ACCUM, 0.0)
              + absorbed; // 临时积累以便每 20 点换 SP
      if (accumulator >= 20.0 - 1.0E-3) {
        int bundles = (int) (accumulator / 20.0);
        accumulator -= (double) bundles * 20.0;
        awardSp(cc, organ, state, bundles * 2.0, now);
      }
      OrganStateOps.setDouble(
          state, cc, organ, ENTRY_IRONWALL_ACCUM, accumulator, value -> Math.max(0.0, value), 0.0);
    }

    if (source != null && source.is(DamageTypeTags.IS_FALL) && damage > 0.0f) {
      double mitigated = damage * 0.2; // 钢筋抗冲基线减伤比
      if (mitigated > 0.0) {
        double spGain = Math.min(6.0, mitigated / 3.0);
        awardSp(cc, organ, state, spGain, now);
      }
    }

    return damage;
  }

  @Override
  public void onRemoved(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
    if (cc == null || organ == null || organ.isEmpty()) {
      return;
    }
    OrganState state = organState(organ, STATE_ROOT);
    OrganStateOps.setBoolean(state, cc, organ, KEY_HARDENING_ACTIVE, false, false);
    OrganStateOps.setBoolean(state, cc, organ, KEY_HEAVY_READY, false, false);
  }

  private static void activateAbility(
      LivingEntity entity, ChestCavityInstance cc, Ability ability) {
    if (!(entity instanceof ServerPlayer player) || cc == null) {
      return;
    }
    ItemStack organ = findOrgan(cc);
    if (organ.isEmpty()) {
      return;
    }
    OrganState state = INSTANCE.organState(organ, STATE_ROOT);
    MultiCooldown cooldown = INSTANCE.cooldown(cc, organ, state);
    long now = player.serverLevel().getGameTime();
    switch (ability) {
      case HARDENING -> INSTANCE.startHardening(player, cc, organ, state, cooldown, now);
      case IRONWALL -> INSTANCE.startIronwall(player, cc, organ, state, cooldown, now);
      case HEAVY_BLOW -> INSTANCE.startHeavyBlow(player, cc, organ, state, cooldown, now);
      case SLAM_FIST -> INSTANCE.castSlamFist(player, cc, organ, state, cooldown, now);
    }
  }

  private void startHardening(
      ServerPlayer player,
      ChestCavityInstance cc,
      ItemStack organ,
      OrganState state,
      MultiCooldown cooldown,
      long now) {
    MultiCooldown.Entry cdEntry = cooldown.entry(ENTRY_CD_HARDENING);
    if (!cdEntry.isReady(now)) {
      return;
    }
    if (state.getBoolean(KEY_HUNGER_LOCKED, false)) {
      warn(player, "饱食不足，硬化无法维持");
      return;
    }
    Optional<ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
    if (handleOpt.isEmpty()) {
      return;
    }
    ResourceHandle handle = handleOpt.get();
    double cost = HARDENING_BASE_ZHENYUAN * costMultiplier(currentPhase(state));
    OptionalDouble result = handle.consumeScaledZhenyuan(cost);
    if (result.isEmpty()) {
      warn(player, "真元不足，硬化失败");
      return;
    }
    OrganStateOps.setBoolean(state, cc, organ, KEY_HARDENING_ACTIVE, true, false);
    cooldown.entry(ENTRY_HARDENING_END).setReadyAt(now + hardeningDuration(currentPhase(state)));
    cooldown.entry(ENTRY_HARDENING_TICK).setReadyAt(now + HARDENING_TICK_INTERVAL);
    cooldown.entry(ENTRY_HARDENING_STAMINA).setReadyAt(now + HARDENING_FOOD_INTERVAL);
    OrganStateOps.setDouble(state, cc, organ, ENTRY_IRONWALL_ACCUM, 0.0, value -> 0.0, 0.0);
    OrganStateOps.setInt(state, cc, organ, ENTRY_HARDENING_SP, 0, value -> Math.max(0, value), 0);
    cdEntry.setReadyAt(now + hardeningCooldown(currentPhase(state)));
    ActiveSkillRegistry.scheduleReadyToast(player, HARDENING_ID, cdEntry.getReadyTick(), now);
    emitHardeningStartFx(player);
    markActivity(cc, organ, state, now);
  }

  private void startIronwall(
      ServerPlayer player,
      ChestCavityInstance cc,
      ItemStack organ,
      OrganState state,
      MultiCooldown cooldown,
      long now) {
    MultiCooldown.Entry cdEntry = cooldown.entry(ENTRY_CD_IRONWALL);
    if (!cdEntry.isReady(now)) {
      return;
    }
    Optional<ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
    if (handleOpt.isEmpty()) {
      return;
    }
    ResourceHandle handle = handleOpt.get();
    double cost = IRONWALL_BASE_ZHENYUAN * costMultiplier(currentPhase(state));
    OptionalDouble result = handle.consumeScaledZhenyuan(cost);
    if (result.isEmpty()) {
      warn(player, "真元不足，铁壁失败");
      return;
    }
    ResourceOps.tryAdjustJingli(player, -8.0, true);
    cooldown.entry(ENTRY_IRONWALL_END).setReadyAt(now + ironwallDuration(currentPhase(state)));
    cdEntry.setReadyAt(now + ironwallCooldown(currentPhase(state)));
    ActiveSkillRegistry.scheduleReadyToast(player, IRONWALL_ID, cdEntry.getReadyTick(), now);
    OrganStateOps.setDouble(state, cc, organ, ENTRY_IRONWALL_ACCUM, 0.0, value -> 0.0, 0.0);
    OrganStateOps.setBoolean(state, cc, organ, KEY_FORCE_SYNC, true, false);
    emitIronwallFx(player);
    markActivity(cc, organ, state, now);
  }

  private void startHeavyBlow(
      ServerPlayer player,
      ChestCavityInstance cc,
      ItemStack organ,
      OrganState state,
      MultiCooldown cooldown,
      long now) {
    MultiCooldown.Entry cdEntry = cooldown.entry(ENTRY_CD_HEAVY);
    if (!cdEntry.isReady(now)) {
      return;
    }
    Optional<ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
    if (handleOpt.isEmpty()) {
      return;
    }
    ResourceHandle handle = handleOpt.get();
    double cost = HEAVY_BLOW_BASE_ZHENYUAN * costMultiplier(currentPhase(state));
    OptionalDouble result = handle.consumeScaledZhenyuan(cost);
    if (result.isEmpty()) {
      warn(player, "真元不足，沉击失败");
      return;
    }
    OrganStateOps.setBoolean(state, cc, organ, KEY_HEAVY_READY, true, false);
    cooldown.entry(ENTRY_HEAVY_TIMEOUT).setReadyAt(now + heavyChargeDuration());
    cdEntry.setReadyAt(now + heavyCooldownTicks());
    ActiveSkillRegistry.scheduleReadyToast(player, HEAVY_BLOW_ID, cdEntry.getReadyTick(), now);
    emitHeavyBlowChargeFx(player);
    markActivity(cc, organ, state, now);
  }

  private void castSlamFist(
      ServerPlayer player,
      ChestCavityInstance cc,
      ItemStack organ,
      OrganState state,
      MultiCooldown cooldown,
      long now) {
    if (!hasBoneSynergy(cc)) {
      warn(player, "需要铁骨/精铁骨蛊联动");
      return;
    }
    MultiCooldown.Entry cdEntry = cooldown.entry(ENTRY_CD_SLAM);
    if (!cdEntry.isReady(now)) {
      return;
    }
    Optional<ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
    if (handleOpt.isEmpty()) {
      return;
    }
    ResourceHandle handle = handleOpt.get();
    double cost = SLAM_FIST_BASE_ZHENYUAN * costMultiplier(currentPhase(state));
    OptionalDouble result = handle.consumeScaledZhenyuan(cost);
    if (result.isEmpty()) {
      warn(player, "真元不足，重拳沉坠失败");
      return;
    }
    cdEntry.setReadyAt(now + slamCooldownTicks());
    ActiveSkillRegistry.scheduleReadyToast(player, SLAM_FIST_ID, cdEntry.getReadyTick(), now);
    emitSlamFistFx(player);
    markActivity(cc, organ, state, now);
  }

  private void tickHunger(
      Player player,
      ChestCavityInstance cc,
      ItemStack organ,
      OrganState state,
      MultiCooldown cooldown,
      long now) {
    MultiCooldown.Entry hungerEntry = cooldown.entry(ENTRY_HUNGER_TICK);
    if (now >= hungerEntry.getReadyTick()) {
      hungerEntry.setReadyAt(now + HUNGER_INTERVAL_TICKS);
      int food = player.getFoodData().getFoodLevel();
      if (food > 0) {
        player.getFoodData().setFoodLevel(Math.max(0, food - 1));
      }
    }
    boolean locked = player.getFoodData().getFoodLevel() <= 3;
    OrganStateOps.setBoolean(state, cc, organ, KEY_HUNGER_LOCKED, locked, false);
  }

  private void tickHardening(
      Player player,
      ChestCavityInstance cc,
      ItemStack organ,
      OrganState state,
      MultiCooldown cooldown,
      long now) {
    if (!state.getBoolean(KEY_HARDENING_ACTIVE, false)) {
      return;
    }
    MultiCooldown.Entry endEntry = cooldown.entry(ENTRY_HARDENING_END);
    if (now >= endEntry.getReadyTick()) {
      finishHardening(cc, organ, state);
      return;
    }

    MultiCooldown.Entry foodEntry = cooldown.entry(ENTRY_HARDENING_STAMINA);
    if (now >= foodEntry.getReadyTick()) {
      foodEntry.setReadyAt(now + HARDENING_FOOD_INTERVAL);
      ResourceOps.tryAdjustJingli(player, -2.0, true);
      int food = player.getFoodData().getFoodLevel();
      if (food > 0) {
        player.getFoodData().setFoodLevel(Math.max(0, food - 1));
      } else {
        finishHardening(cc, organ, state);
        warn(player, "饱食耗尽，硬化中止");
        return;
      }
    }

    MultiCooldown.Entry tickEntry = cooldown.entry(ENTRY_HARDENING_TICK);
    if (now >= tickEntry.getReadyTick()) {
      tickEntry.setReadyAt(now + HARDENING_TICK_INTERVAL);
      if (isActivityRecent(state, now)) {
        int used = state.getInt(ENTRY_HARDENING_SP, 0);
        if (used < HARDENING_MAX_SP_PER_CAST) {
          if (awardSp(cc, organ, state, 1.0, now)) {
            OrganStateOps.setInt(
                state, cc, organ, ENTRY_HARDENING_SP, used + 1, value -> Math.max(0, value), 0);
          }
        }
      }
    }
  }

  private void tickIronwall(
      Player player,
      ChestCavityInstance cc,
      ItemStack organ,
      OrganState state,
      MultiCooldown cooldown,
      long now) {
    MultiCooldown.Entry entry = cooldown.entry(ENTRY_IRONWALL_END);
    if (entry.getReadyTick() > 0L && now >= entry.getReadyTick()) {
      entry.setReadyAt(0L);
      OrganStateOps.setDouble(state, cc, organ, ENTRY_IRONWALL_ACCUM, 0.0, value -> 0.0, 0.0);
      OrganStateOps.setBoolean(state, cc, organ, KEY_FORCE_SYNC, true, false);
    }
  }

  private void tickHeavyBlow(
      Player player,
      ChestCavityInstance cc,
      ItemStack organ,
      OrganState state,
      MultiCooldown cooldown,
      long now) {
    if (!state.getBoolean(KEY_HEAVY_READY, false)) {
      return;
    }
    MultiCooldown.Entry entry = cooldown.entry(ENTRY_HEAVY_TIMEOUT);
    if (now >= entry.getReadyTick()) {
      entry.setReadyAt(0L);
      OrganStateOps.setBoolean(state, cc, organ, KEY_HEAVY_READY, false, false);
    }
  }

  private void finishHardening(ChestCavityInstance cc, ItemStack organ, OrganState state) {
    OrganStateOps.setBoolean(state, cc, organ, KEY_HARDENING_ACTIVE, false, false);
  }

  private void checkPhaseProgression(
      ServerPlayer player,
      ChestCavityInstance cc,
      ItemStack organ,
      OrganState state,
      long now) {
    int phase = currentPhase(state);
    if (phase >= MAX_PHASE) {
      return;
    }
    LinkageChannel channel = tiepiChannel(cc);
    if (channel == null) {
      return;
    }
    double sp = channel.get();
    int nextThreshold = PHASE_THRESHOLDS[phase];
    if (sp + 1.0E-3 >= nextThreshold) {
      OrganStateOps.setBoolean(state, cc, organ, KEY_UPGRADE_READY, true, false);
      OrganStateOps.setBoolean(state, cc, organ, KEY_FORCE_SYNC, true, false);
    }
  }

  private void syncProgress(
      ServerPlayer player,
      ChestCavityInstance cc,
      ItemStack organ,
      OrganState state,
      MultiCooldown cooldown,
      long now) {
    LinkageChannel channel = tiepiChannel(cc);
    if (channel == null) {
      return;
    }
    double storedSp = channel.get();
    int phase = currentPhase(state);
    int nextThreshold = phase < MAX_PHASE ? PHASE_THRESHOLDS[phase] : PHASE_THRESHOLDS[MAX_PHASE - 1];
    double windowGain = Math.max(0.0, state.getDouble(KEY_WINDOW_GAIN, 0.0));
    boolean hungerLocked = state.getBoolean(KEY_HUNGER_LOCKED, false);
    boolean hardeningActive = state.getBoolean(KEY_HARDENING_ACTIVE, false);
    boolean ironwallActive = isIronwallActive(state, cooldown, now);
    boolean heavyArmed = state.getBoolean(KEY_HEAVY_READY, false);
    boolean slamUnlocked = hasBoneSynergy(cc);

    TiePiProgressPayload payload =
        new TiePiProgressPayload(
            player.getId(),
            phase,
            storedSp,
            windowGain,
            WINDOW_CAP,
            nextThreshold,
            now,
            hungerLocked,
            hardeningActive,
            ironwallActive,
            heavyArmed,
            slamUnlocked,
            cooldown.entry(ENTRY_CD_HARDENING).getReadyTick(),
            cooldown.entry(ENTRY_CD_IRONWALL).getReadyTick(),
            cooldown.entry(ENTRY_CD_HEAVY).getReadyTick(),
            cooldown.entry(ENTRY_CD_SLAM).getReadyTick(),
            cooldown.entry(ENTRY_HARDENING_END).getReadyTick(),
            cooldown.entry(ENTRY_IRONWALL_END).getReadyTick(),
            cooldown.entry(ENTRY_HEAVY_TIMEOUT).getReadyTick(),
            hasCopperSynergy(cc),
            hasBoneSynergy(cc),
            hasBellSynergy(cc),
            hasSwordSynergy(cc),
            hasLeiSynergy(cc));
    player.connection.send(payload);
  }

  private boolean awardSp(
      ChestCavityInstance cc, ItemStack organ, OrganState state, double amount, long now) {
    if (cc == null || organ == null || organ.isEmpty() || amount <= 0.0) {
      return false;
    }
    LinkageChannel channel = tiepiChannel(cc);
    if (channel == null) {
      return false;
    }
    double allowed = clampWindow(state, cc, organ, amount, now);
    if (allowed <= 0.0) {
      return false;
    }
    channel.adjust(allowed);
    OrganStateOps.setBoolean(state, cc, organ, KEY_FORCE_SYNC, true, false);
    return true;
  }

  private void refreshWindow(OrganState state, ChestCavityInstance cc, ItemStack organ, long now) {
    long resetTick = Math.max(0L, state.getLong(KEY_WINDOW_RESET, 0L));
    if (resetTick == 0L || now >= resetTick) {
      OrganStateOps.setDouble(state, cc, organ, KEY_WINDOW_GAIN, 0.0, value -> 0.0, 0.0);
      OrganStateOps.setLong(
          state,
          cc,
          organ,
          KEY_WINDOW_RESET,
          now + WINDOW_INTERVAL_TICKS,
          value -> Math.max(now, value),
          now + WINDOW_INTERVAL_TICKS);
    }
  }

  private double clampWindow(
      OrganState state, ChestCavityInstance cc, ItemStack organ, double requested, long now) {
    double gain = Math.max(0.0, state.getDouble(KEY_WINDOW_GAIN, 0.0));
    long resetTick = Math.max(0L, state.getLong(KEY_WINDOW_RESET, 0L));
    if (resetTick == 0L || now >= resetTick) {
      gain = 0.0;
      resetTick = now + WINDOW_INTERVAL_TICKS;
      OrganStateOps.setLong(
          state,
          cc,
          organ,
          KEY_WINDOW_RESET,
          resetTick,
          value -> Math.max(now, value),
          resetTick);
    }
    double remaining = Math.max(0.0, WINDOW_CAP - gain);
    double granted = Math.min(requested, remaining);
    if (granted > 0.0) {
      OrganStateOps.setDouble(
          state,
          cc,
          organ,
          KEY_WINDOW_GAIN,
          gain + granted,
          value -> Math.max(0.0, value),
          0.0);
    }
    return granted;
  }

  private boolean isIronwallActive(OrganState state, MultiCooldown cooldown, long now) {
    long end = cooldown.entry(ENTRY_IRONWALL_END).getReadyTick();
    return end > 0L && now < end;
  }

  private void markActivity(ChestCavityInstance cc, ItemStack organ, OrganState state, long now) {
    OrganStateOps.setLong(
        state, cc, organ, KEY_ACTIVITY_TICK, now, value -> Math.max(0L, value), 0L);
  }

  private boolean isActivityRecent(OrganState state, long now) {
    long tick = Math.max(0L, state.getLong(KEY_ACTIVITY_TICK, 0L));
    return tick > 0L && now - tick <= ACTIVITY_TIMEOUT_TICKS;
  }

  private void trackSynergies(ChestCavityInstance cc, ItemStack organ, OrganState state) {
    boolean copper = hasCopperSynergy(cc);
    boolean bone = hasBoneSynergy(cc);
    boolean bell = hasBellSynergy(cc);
    boolean sword = hasSwordSynergy(cc);
    boolean lei = hasLeiSynergy(cc);
    boolean changed =
        updateSynergyFlag(state, cc, organ, KEY_SYNERGY_COPPER, copper)
            | updateSynergyFlag(state, cc, organ, KEY_SYNERGY_BONE, bone)
            | updateSynergyFlag(state, cc, organ, KEY_SYNERGY_BELL, bell)
            | updateSynergyFlag(state, cc, organ, KEY_SYNERGY_SWORD, sword)
            | updateSynergyFlag(state, cc, organ, KEY_SYNERGY_LEI, lei);
    if (changed) {
      OrganStateOps.setBoolean(state, cc, organ, KEY_FORCE_SYNC, true, false);
    }
  }

  private boolean updateSynergyFlag(
      OrganState state,
      ChestCavityInstance cc,
      ItemStack organ,
      String key,
      boolean value) {
    boolean previous = state.getBoolean(key, false);
    if (previous == value) {
      return false;
    }
    OrganStateOps.setBoolean(state, cc, organ, key, value, false);
    return true;
  }

  private int currentPhase(OrganState state) {
    return Mth.clamp(state.getInt(KEY_PHASE, 1), 1, MAX_PHASE);
  }

  private double costMultiplier(int phase) {
    return 1.0 + 0.1 * (Math.max(1, phase) - 1);
  }

  private int hardeningDuration(int phase) {
    int duration = 15 * 20;
    if (phase >= 5) {
      duration += 3 * 20;
    }
    return duration;
  }

  private int hardeningCooldown(int phase) {
    int cooldown = 30 * 20;
    if (phase >= 5) {
      cooldown -= 5 * 20;
    }
    return Math.max(5 * 20, cooldown);
  }

  private float applyHardeningReduction(float damage, int phase) {
    double reduction = 0.10;
    if (phase >= 2) {
      reduction = 0.12;
    }
    return (float) Math.max(0.0, damage * (1.0 - reduction));
  }

  private void emitHardeningStartFx(ServerPlayer player) {
    if (player == null) {
      return;
    }
    ServerLevel server = player.serverLevel();
    Vec3 center = bodyCenter(player, 0.6);
    float pitch = 0.82f + player.getRandom().nextFloat() * 0.12f;
    FxOps.playSound(server, center, SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.9f, pitch);
    FxOps.playSound(server, center, SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.4f, 0.65f);
    FxOps.particles(server, ParticleTypes.CRIT, center, 20, 0.35, 0.45, 0.35, 0.3);
    FxOps.particles(server, ParticleTypes.END_ROD, center, 12, 0.2, 0.3, 0.2, 0.02);
  }

  private void emitIronwallFx(ServerPlayer player) {
    if (player == null) {
      return;
    }
    ServerLevel server = player.serverLevel();
    Vec3 center = bodyCenter(player, 0.55);
    FxOps.playSound(server, center, SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.9f, 0.8f);
    FxOps.playSound(server, center, SoundEvents.ANVIL_PLACE, SoundSource.PLAYERS, 0.6f, 0.7f);
    FxOps.particles(server, ParticleTypes.CLOUD, center, 18, 0.4, 0.3, 0.4, 0.05);
    FxOps.particles(server, ParticleTypes.CRIT, center, 10, 0.25, 0.3, 0.25, 0.15);
  }

  private void emitHeavyBlowChargeFx(ServerPlayer player) {
    if (player == null) {
      return;
    }
    ServerLevel server = player.serverLevel();
    Vec3 center = bodyCenter(player, 0.65);
    FxOps.playSound(server, center, SoundEvents.ANVIL_HIT, SoundSource.PLAYERS, 0.8f, 0.75f);
    FxOps.particles(server, ParticleTypes.SWEEP_ATTACK, center, 4, 0.0, 0.1, 0.0, 0.0);
    FxOps.particles(server, ParticleTypes.CRIT, center, 12, 0.3, 0.35, 0.3, 0.25);
  }

  private void emitHeavyImpactFx(ServerPlayer player, LivingEntity target) {
    if (player == null) {
      return;
    }
    ServerLevel server = player.serverLevel();
    Vec3 center =
        target != null
            ? bodyCenter(target, 0.5)
            : player.position().add(0.0, player.getBbHeight() * 0.4, 0.0);
    FxOps.playSound(
        server, center, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.65f, 1.2f);
    FxOps.playSound(server, center, SoundEvents.IRON_GOLEM_ATTACK, SoundSource.PLAYERS, 0.7f, 0.9f);
    FxOps.particles(server, ParticleTypes.EXPLOSION, center, 1, 0.0, 0.0, 0.0, 0.0);
    FxOps.particles(server, ParticleTypes.CRIT, center, 16, 0.4, 0.4, 0.4, 0.4);
    FxOps.particles(server, ParticleTypes.SWEEP_ATTACK, center, 3, 0.0, 0.0, 0.0, 0.0);
  }

  private void emitSlamFistFx(ServerPlayer player) {
    if (player == null) {
      return;
    }
    ServerLevel server = player.serverLevel();
    Vec3 center = bodyCenter(player, 0.55);
    FxOps.playSound(server, center, SoundEvents.IRON_GOLEM_ATTACK, SoundSource.PLAYERS, 1.0f, 0.95f);
    FxOps.playSound(
        server, center, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.5f, 0.9f);
    FxOps.particles(server, ParticleTypes.SWEEP_ATTACK, center, 5, 0.0, 0.0, 0.0, 0.0);
    FxOps.particles(server, ParticleTypes.CRIT, center, 14, 0.35, 0.35, 0.35, 0.3);
  }

  private Vec3 bodyCenter(LivingEntity entity, double heightFactor) {
    if (entity == null) {
      return Vec3.ZERO;
    }
    return entity.position().add(0.0, entity.getBbHeight() * heightFactor, 0.0);
  }

  private int ironwallDuration(int phase) {
    int duration = 3 * 20;
    if (phase >= 2) {
      duration += 10;
    }
    return duration;
  }

  private int ironwallCooldown(int phase) {
    int cooldown = 20 * 20;
    if (phase >= 3) {
      cooldown -= 2 * 20;
    }
    return Math.max(5 * 20, cooldown);
  }

  private double ironwallReduction(int phase) {
    double reduction = 0.05;
    if (phase >= 3) {
      reduction += 0.02;
    }
    return reduction;
  }

  private int heavyChargeDuration() {
    return 8 * 20;
  }

  private int heavyCooldownTicks() {
    return 12 * 20;
  }

  private int slamCooldownTicks() {
    return 18 * 20;
  }

  private LinkageChannel tiepiChannel(ChestCavityInstance cc) {
    return LedgerOps.ensureChannel(cc, TIEPI_SP_CHANNEL, SP_CLAMP);
  }

  private MultiCooldown cooldown(ChestCavityInstance cc, ItemStack organ, OrganState state) {
    MultiCooldown.Builder builder =
        MultiCooldown.builder(state).withLongClamp(value -> Math.max(0L, value), 0L);
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
    for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
      ItemStack stack = cc.inventory.getItem(i);
      if (stack == null || stack.isEmpty()) {
        continue;
      }
      ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
      if (Objects.equals(id, ORGAN_ID)) {
        return stack;
      }
    }
    return ItemStack.EMPTY;
  }

  private static boolean hasCopperSynergy(ChestCavityInstance cc) {
    return hasAnyOrgan(cc, COPPER_SYNERGY_IDS);
  }

  private static boolean hasBoneSynergy(ChestCavityInstance cc) {
    return hasAnyOrgan(cc, BONE_SYNERGY_IDS);
  }

  private static boolean hasBellSynergy(ChestCavityInstance cc) {
    return hasAnyOrgan(cc, BELL_SYNERGY_IDS);
  }

  private static boolean hasSwordSynergy(ChestCavityInstance cc) {
    return hasAnyOrgan(cc, SWORD_SYNERGY_IDS);
  }

  private static boolean hasLeiSynergy(ChestCavityInstance cc) {
    return hasAnyOrgan(cc, LEI_SYNERGY_IDS);
  }

  private static boolean hasAnyOrgan(ChestCavityInstance cc, ResourceLocation... ids) {
    if (cc == null || cc.inventory == null || ids == null || ids.length == 0) {
      return false;
    }
    for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
      ItemStack stack = cc.inventory.getItem(i);
      if (stack == null || stack.isEmpty()) {
        continue;
      }
      ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
      for (ResourceLocation candidate : ids) {
        if (candidate != null && candidate.equals(id)) {
          return true;
        }
      }
    }
    return false;
  }

  private void warn(Player player, String message) {
    player.displayClientMessage(Component.literal(message), true);
  }

  private void applyAttackSpeedPulse(Player player) {
    var attribute = player.getAttribute(Attributes.ATTACK_SPEED);
    if (attribute == null) {
      return;
    }
    AttributeOps.replaceTransient(
        attribute,
        ATTACK_SPEED_MOD_ID,
        new net.minecraft.world.entity.ai.attributes.AttributeModifier(
            ATTACK_SPEED_MOD_ID,
            0.05,
            net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
  }

  private OrganState organState(ItemStack stack, String rootKey) {
    return OrganState.of(stack, rootKey);
  }

  private enum Ability {
    HARDENING(HARDENING_ID),
    IRONWALL(IRONWALL_ID),
    HEAVY_BLOW(HEAVY_BLOW_ID),
    SLAM_FIST(SLAM_FIST_ID);

    final ResourceLocation abilityId;

    Ability(ResourceLocation abilityId) {
      this.abilityId = abilityId;
    }
  }
}
