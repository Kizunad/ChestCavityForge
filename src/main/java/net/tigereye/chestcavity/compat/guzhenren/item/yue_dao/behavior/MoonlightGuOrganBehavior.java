package net.tigereye.chestcavity.compat.guzhenren.item.yue_dao.behavior;

import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.AbstractGuzhenrenOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.AttributeOps;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.EffectOps;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.LedgerOps;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.OrganStateOps;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.linkage.ActiveLinkageContext;
import net.tigereye.chestcavity.linkage.LinkageChannel;
import net.tigereye.chestcavity.linkage.LinkageManager;
import net.tigereye.chestcavity.linkage.policy.ClampPolicy;
import net.tigereye.chestcavity.listeners.OrganIncomingDamageListener;
import net.tigereye.chestcavity.listeners.OrganOnHitListener;
import net.tigereye.chestcavity.listeners.OrganRemovalContext;
import net.tigereye.chestcavity.listeners.OrganRemovalListener;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.util.NetworkUtil;

/** 月光蛊 - 夜月增益器官。 */
public final class MoonlightGuOrganBehavior extends AbstractGuzhenrenOrganBehavior
    implements OrganSlowTickListener,
        OrganIncomingDamageListener,
        OrganOnHitListener,
        OrganRemovalListener {

  public static final MoonlightGuOrganBehavior INSTANCE = new MoonlightGuOrganBehavior();

  private static final String MOD_ID = "guzhenren";
  private static final ResourceLocation ORGAN_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "yue_guang_gu");
  private static final ResourceLocation LUNAR_WARD_CHANNEL =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/lunar_ward");
  private static final ResourceLocation HEALTH_MODIFIER_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "modifiers/yue_guang_gu_health");
  private static final ResourceLocation SPEED_MODIFIER_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "modifiers/yue_guang_gu_speed");

  private static final String STATE_ROOT = "GZR_Lunar";
  private static final String KEY_TIER = "tier";
  private static final String KEY_WARD = "ward";
  private static final String KEY_TEMP_WARD = "tempWard";
  private static final String KEY_WARD_CAP = "wardCap";
  private static final String KEY_LAST_DAMAGE_TICK = "lastDamageTick";
  private static final String KEY_LAST_BREAK_TICK = "lastBreakTick";
  private static final String KEY_LAST_SLOW_TICK = "lastSlowTick";
  private static final String KEY_DAMAGE_REDUCTION = "damageReduction";
  private static final String KEY_SPEED_PERCENT = "speedPercent";
  private static final String KEY_JUMP_LEVEL = "jumpLevel";
  private static final String KEY_ACTIVE_FACTOR = "activeFactor";
  private static final String KEY_MOON_PHASE = "moonPhase";
  private static final String KEY_BRIGHTNESS = "brightness";
  private static final String KEY_SKY_VISIBLE = "skyVisible";
  private static final String KEY_TIDE_STACKS = "tideStacks";
  private static final String KEY_TIDE_LAST_GAIN = "tideLastGain";
  private static final String KEY_TIDE_LOCKOUT = "tideLockout";
  private static final String KEY_SURGE_READY = "surgeReady";
  private static final String KEY_SURGE_COOLDOWN = "surgeCooldown";
  private static final String KEY_LXP = "lxp";
  private static final String KEY_LXP_FRACTION = "lxpFraction";
  private static final String KEY_ACTIVE_TICK_ACCUM = "activeTickAccum";
  private static final String KEY_SHIELD_ABSORB_ACCUM = "shieldAbsorbAccum";
  private static final String KEY_LAST_ELITE_KILL_TICK = "lastEliteKillTick";
  private static final String KEY_LAST_BOSS_KILL_TICK = "lastBossKillTick";
  private static final String KEY_LAST_SURGE_LXP_TICK = "lastSurgeLxpTick";
  private static final String KEY_BATTLE_DECAY_UNTIL = "battleDecayUntil";
  private static final String KEY_CURRENT_FULL_MOON_DAY = "currentFullMoonDay";
  private static final String KEY_CURRENT_FULL_MOON_ACTIVE_TICKS = "currentFullMoonActiveTicks";
  private static final String KEY_LAST_FULL_MOON_ACTIVE_TICKS = "lastFullMoonActiveTicks";

  private static final double BASE_COST = 96.0D;
  private static final double INDOOR_BASE_RATIO = 0.50D;
  private static final double INDOOR_L1_RATIO = 0.60D;
  private static final int MAX_TIER = 5;
  private static final int MAX_TIDE_STACKS = 6;
  private static final int TIDE_INTERVAL_TICKS = 8 * 20;
  private static final int TIDE_LOCKOUT_TICKS = 8 * 20;
  private static final double TIDE_TRIGGER_REDUCTION = 0.08D;
  private static final double TIDE_TRIGGER_WARD_BONUS = 2.0D;
  private static final double SURGE_TEMP_WARD = 4.0D;
  private static final int SURGE_COOLDOWN_TICKS = 20 * 20;
  private static final int WARD_REGEN_DELAY_TICKS = 40;
  private static final int WARD_BREAK_DELAY_TICKS = 30;
  private static final int HALF_SECOND_TICKS = 10;
  private static final double BASE_REGEN_PER_HALF_SECOND = 1.0D;
  private static final double L2_REGEN_MULTIPLIER = 0.20D;
  private static final double L3_DR_BONUS = 0.03D;
  private static final double L3_SPEED_BONUS = 0.03D;
  private static final double DR_SOFT_CAP = 0.18D;
  private static final double PVP_MULTIPLIER = 0.75D;
  private static final double EPSILON = 1.0E-4D;
  private static final double SURGE_SLOW_RADIUS = 4.0D;
  private static final int SURGE_SLOW_DURATION = 40;

  private static final int ACTIVE_TICKS_PER_LXP = 30 * 20;
  private static final double SHIELD_ABSORB_PER_LXP = 100.0D;
  private static final int ELITE_KILL_LXP = 3;
  private static final int BOSS_KILL_LXP = 8;
  private static final int KILL_LXP_COOLDOWN_TICKS = 30 * 20;
  private static final int SURGE_EVENT_LXP = 5;
  private static final int SURGE_EVENT_COOLDOWN_TICKS = 30 * 20;
  private static final int LXP_CAP = 10_000;
  private static final int[] LXP_REQUIREMENTS = {0, 0, 120, 240, 360, 480};
  private static final double MAX_LXP_FRACTION = 1.0D;
  private static final double FULL_MOON_REQUIRED_ACTIVE_TICKS = 10 * 60 * 20;
  private static final double LXP_MIN_STEP = 1.0E-6D;
  private static final long FULL_MOON_DAY_SENTINEL = -1L;
  private static final long BATTLE_DECAY_DURATION_TICKS = 24L * 60L * 60L * 20L;
  private static final double BATTLE_DECAY_MULTIPLIER = 0.5D;

  private static final ClampPolicy NON_NEGATIVE = new ClampPolicy(0.0D, Double.MAX_VALUE);

  private static final MoonPhaseStats[] PHASE_STATS =
      new MoonPhaseStats[] {
        new MoonPhaseStats(0.30D, 8.0D, 0.12D, 0.10D, 2), // 0 满月
        new MoonPhaseStats(0.22D, 6.0D, 0.09D, 0.07D, 1), // 1 凸月
        new MoonPhaseStats(0.14D, 4.0D, 0.06D, 0.05D, 1), // 2 上弦
        new MoonPhaseStats(0.08D, 2.0D, 0.04D, 0.03D, 0), // 3 娥眉
        new MoonPhaseStats(-0.05D, 0.0D, 0.00D, 0.00D, 0), // 4 朔月
        new MoonPhaseStats(0.08D, 2.0D, 0.04D, 0.03D, 0), // 5 娥眉
        new MoonPhaseStats(0.14D, 4.0D, 0.06D, 0.05D, 1), // 6 下弦
        new MoonPhaseStats(0.22D, 6.0D, 0.09D, 0.07D, 1) // 7 凸月
      };

  private MoonlightGuOrganBehavior() {}

  @Override
  public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
    if (!(entity instanceof Player player)
        || entity.level().isClientSide()
        || cc == null
        || organ == null
        || organ.isEmpty()) {
      return;
    }
    if (!matchesOrgan(organ, ORGAN_ID)) {
      return;
    }

    Level level = entity.level();
    long gameTime = level.getGameTime();

    OrganState state = organState(organ, STATE_ROOT);
    OrganStateOps.Collector collector = OrganStateOps.collector(cc, organ);

    int storedTier = Mth.clamp(state.getInt(KEY_TIER, 1), 0, MAX_TIER);
    if (storedTier <= 0) {
      storedTier = 1;
      collector.record(
          state.setInt(KEY_TIER, storedTier, value -> Mth.clamp(value, 0, MAX_TIER), 1));
    }
    final int tier = storedTier;

    boolean isNight = level.isNight();
    BlockPos pos = entity.blockPosition();
    boolean skyVisible = level.canSeeSky(pos.above());
    int brightness = level.getMaxLocalRawBrightness(pos);

    double activeFactor = 0.0D;
    if (isNight && skyVisible) {
      Optional<GuzhenrenResourceBridge.ResourceHandle> handleOpt =
          GuzhenrenResourceBridge.open(player);
      if (handleOpt.isPresent()
          && ResourceOps.tryConsumeScaledZhenyuan(handleOpt.get(), BASE_COST).isPresent()) {
        activeFactor = 1.0D;
      }
    }

    int moonPhase = level.getMoonPhase();
    MoonPhaseStats stats = PHASE_STATS[Math.floorMod(moonPhase, PHASE_STATS.length)];

    double healthPercent = stats.healthPercent();
    double wardBase = stats.ward();
    double damageReduction = stats.damageReduction();
    double speedPercent = stats.speedPercent();
    int jumpLevel = stats.jumpLevel();

    if (tier >= 1) {
      wardBase += 1.0D;
      if (moonPhase != 4) {
        jumpLevel = Math.max(jumpLevel, 1);
      }
    }
    if (tier >= 2) {
      wardBase += 1.0D;
    }
    if (tier >= 3) {
      damageReduction += L3_DR_BONUS;
      speedPercent += L3_SPEED_BONUS;
    }
    if (tier >= 4 && moonPhase == 4 && healthPercent < 0.0D) {
      healthPercent = 0.0D;
    }
    if (tier >= 5 && moonPhase == 0) {
      if (healthPercent > 0.0D) {
        healthPercent *= 1.30D;
      }
      wardBase *= 1.30D;
      damageReduction *= 1.30D;
      speedPercent *= 1.30D;
    }

    healthPercent *= activeFactor;
    wardBase *= activeFactor;
    damageReduction *= activeFactor;
    speedPercent *= activeFactor;

    if (isPvpServer(level)) {
      healthPercent *= PVP_MULTIPLIER;
      wardBase *= PVP_MULTIPLIER;
      damageReduction *= PVP_MULTIPLIER;
      speedPercent *= PVP_MULTIPLIER;
    }

    damageReduction = Mth.clamp(damageReduction, 0.0F, (float) DR_SOFT_CAP);

    AttributeInstance maxHealthAttr = entity.getAttribute(Attributes.MAX_HEALTH);
    if (maxHealthAttr != null) {
      if (Math.abs(healthPercent) > EPSILON) {
        AttributeModifier modifier =
            new AttributeModifier(
                HEALTH_MODIFIER_ID,
                healthPercent,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        AttributeOps.replaceTransient(maxHealthAttr, HEALTH_MODIFIER_ID, modifier);
      } else {
        AttributeOps.removeById(maxHealthAttr, HEALTH_MODIFIER_ID);
      }
    }

    AttributeInstance speedAttr = entity.getAttribute(Attributes.MOVEMENT_SPEED);
    if (speedAttr != null) {
      if (Math.abs(speedPercent) > EPSILON) {
        AttributeModifier modifier =
            new AttributeModifier(
                SPEED_MODIFIER_ID, speedPercent, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        AttributeOps.replaceTransient(speedAttr, SPEED_MODIFIER_ID, modifier);
      } else {
        AttributeOps.removeById(speedAttr, SPEED_MODIFIER_ID);
      }
    }

    if (jumpLevel > 0 && activeFactor > 0.0D) {
      EffectOps.ensure(entity, MobEffects.JUMP, 60, Math.max(0, jumpLevel - 1), false, true);
    } else {
      EffectOps.remove(entity, MobEffects.JUMP);
    }

    double wardCap = Math.max(0.0D, wardBase);
    double currentWard = Math.max(0.0D, state.getDouble(KEY_WARD, 0.0D));
    double tempWard = Math.max(0.0D, state.getDouble(KEY_TEMP_WARD, 0.0D));

    if (currentWard > wardCap) {
      currentWard = wardCap;
    }

    long lastSlowTick = state.getLong(KEY_LAST_SLOW_TICK, gameTime);
    long lastDamageTick = state.getLong(KEY_LAST_DAMAGE_TICK, 0L);
    long lastBreakTick = state.getLong(KEY_LAST_BREAK_TICK, 0L);

    long delta = Math.max(0L, gameTime - lastSlowTick);
    boolean canRegen =
        wardCap > 0.0D
            && currentWard < wardCap
            && activeFactor > 0.0D
            && gameTime - lastDamageTick >= WARD_REGEN_DELAY_TICKS
            && gameTime - lastBreakTick >= WARD_BREAK_DELAY_TICKS;
    if (canRegen && delta > 0L) {
      double regenMultiplier = 1.0D + (tier >= 2 ? L2_REGEN_MULTIPLIER : 0.0D);
      double halfSeconds = delta / (double) HALF_SECOND_TICKS;
      if (halfSeconds > 0.0D) {
        double regenerated = halfSeconds * BASE_REGEN_PER_HALF_SECOND * regenMultiplier;
        currentWard = Math.min(wardCap, currentWard + regenerated);
      }
    }

    collector.record(state.setDouble(KEY_WARD, currentWard, value -> Math.max(0.0D, value), 0.0D));
    collector.record(
        state.setDouble(KEY_TEMP_WARD, tempWard, value -> Math.max(0.0D, value), 0.0D));
    collector.record(state.setDouble(KEY_WARD_CAP, wardCap, value -> Math.max(0.0D, value), 0.0D));
    collector.record(
        state.setDouble(
            KEY_DAMAGE_REDUCTION,
            damageReduction,
            value -> Mth.clamp(value, 0.0D, DR_SOFT_CAP),
            0.0D));
    collector.record(state.setDouble(KEY_SPEED_PERCENT, speedPercent, value -> value, 0.0D));
    collector.record(state.setInt(KEY_JUMP_LEVEL, jumpLevel, value -> Mth.clamp(value, 0, 2), 0));
    collector.record(
        state.setDouble(
            KEY_ACTIVE_FACTOR, activeFactor, value -> Mth.clamp(value, 0.0D, 1.0D), 0.0D));
    collector.record(state.setInt(KEY_MOON_PHASE, moonPhase, value -> Math.max(0, value), 0));
    collector.record(state.setInt(KEY_BRIGHTNESS, brightness, value -> Math.max(0, value), 0));
    collector.record(state.setBoolean(KEY_SKY_VISIBLE, skyVisible, false));
    collector.record(state.setLong(KEY_LAST_SLOW_TICK, gameTime, value -> Math.max(0L, value), 0L));

    handleProgression(
        player, cc, organ, state, collector, tier, activeFactor, delta, level, gameTime, moonPhase);

    handleTideStacks(level, state, collector, tier, activeFactor, gameTime);
    handleSurgeReady(state, collector, tier, activeFactor, gameTime);

    collector.commit();

    ActiveLinkageContext context = LinkageManager.getContext(cc);
    if (context != null) {
      LinkageChannel channel = LedgerOps.ensureChannel(context, LUNAR_WARD_CHANNEL, NON_NEGATIVE);
      if (channel != null) {
        channel.set(currentWard + tempWard);
      }
    }
  }

  @Override
  public float onIncomingDamage(
      net.minecraft.world.damagesource.DamageSource source,
      LivingEntity victim,
      ChestCavityInstance cc,
      ItemStack organ,
      float damage) {
    if (!(victim instanceof Player)
        || victim.level().isClientSide()
        || cc == null
        || organ == null
        || organ.isEmpty()) {
      return damage;
    }
    if (!matchesOrgan(organ, ORGAN_ID)) {
      return damage;
    }
    if (damage <= 0.0F) {
      return damage;
    }

    Level level = victim.level();
    long gameTime = level.getGameTime();
    int moonPhase = level.getMoonPhase();

    OrganState state = organState(organ, STATE_ROOT);
    OrganStateOps.Collector collector = OrganStateOps.collector(cc, organ);

    int tier = Mth.clamp(state.getInt(KEY_TIER, 1), 0, MAX_TIER);
    double activeFactor = state.getDouble(KEY_ACTIVE_FACTOR, 0.0D);

    double tempWard = Math.max(0.0D, state.getDouble(KEY_TEMP_WARD, 0.0D));
    double currentWard = Math.max(0.0D, state.getDouble(KEY_WARD, 0.0D));
    double wardCap = Math.max(0.0D, state.getDouble(KEY_WARD_CAP, 0.0D));
    double damageReduction =
        Mth.clamp(state.getDouble(KEY_DAMAGE_REDUCTION, 0.0D), 0.0D, DR_SOFT_CAP);

    double remaining = damage;
    double absorbed = 0.0D;

    if (tier >= 5 && activeFactor > 0.0D && state.getBoolean(KEY_SURGE_READY, false)) {
      tempWard += SURGE_TEMP_WARD;
      collector.record(state.setBoolean(KEY_SURGE_READY, false, false));
      collector.record(
          state.setLong(
              KEY_SURGE_COOLDOWN,
              gameTime + SURGE_COOLDOWN_TICKS,
              value -> Math.max(0L, value),
              0L));
      applySurgeSlow(victim);
    }

    if (tempWard > 0.0D && remaining > 0.0D) {
      double used = Math.min(tempWard, remaining);
      tempWard -= used;
      remaining -= used;
      absorbed += used;
    }

    if (currentWard > 0.0D && remaining > 0.0D) {
      double used = Math.min(currentWard, remaining);
      currentWard -= used;
      remaining -= used;
      absorbed += used;
      if (currentWard <= EPSILON) {
        collector.record(
            state.setLong(KEY_LAST_BREAK_TICK, gameTime, value -> Math.max(0L, value), 0L));
        currentWard = Math.max(0.0D, currentWard);
      }
    }

    if (remaining > 0.0D && damageReduction > 0.0D) {
      remaining = remaining * (1.0D - damageReduction);
    }

    if (tier >= 4 && activeFactor > 0.0D) {
      int stacks = Mth.clamp(state.getInt(KEY_TIDE_STACKS, 0), 0, MAX_TIDE_STACKS);
      long lockout = state.getLong(KEY_TIDE_LOCKOUT, 0L);
      if (stacks >= MAX_TIDE_STACKS && gameTime >= lockout) {
        remaining = remaining * (1.0D - TIDE_TRIGGER_REDUCTION);
        currentWard = Math.min(wardCap, currentWard + TIDE_TRIGGER_WARD_BONUS);
        collector.record(
            state.setInt(KEY_TIDE_STACKS, 0, value -> Mth.clamp(value, 0, MAX_TIDE_STACKS), 0));
        collector.record(
            state.setLong(
                KEY_TIDE_LOCKOUT, gameTime + TIDE_LOCKOUT_TICKS, value -> Math.max(0L, value), 0L));
        collector.record(
            state.setLong(KEY_TIDE_LAST_GAIN, gameTime, value -> Math.max(0L, value), 0L));
        long surgeReadyTick = state.getLong(KEY_LAST_SURGE_LXP_TICK, 0L);
        if (gameTime >= surgeReadyTick) {
          GrantOutcome surgeOutcome =
              grantLxp(
                  (Player) victim,
                  cc,
                  organ,
                  state,
                  collector,
                  SURGE_EVENT_LXP,
                  LxpSource.SURGE,
                  level,
                  moonPhase,
                  gameTime);
          if (surgeOutcome.lxpChanged()) {
            collector.record(
                state.setLong(
                    KEY_LAST_SURGE_LXP_TICK,
                    gameTime + SURGE_EVENT_COOLDOWN_TICKS,
                    value -> Math.max(0L, value),
                    0L));
          }
        }
      }
    }

    if (absorbed > 0.0D || remaining < damage) {
      collector.record(
          state.setLong(KEY_LAST_DAMAGE_TICK, gameTime, value -> Math.max(0L, value), 0L));
    }

    if (absorbed > EPSILON) {
      double absorbAccum = Math.max(0.0D, state.getDouble(KEY_SHIELD_ABSORB_ACCUM, 0.0D));
      absorbAccum += absorbed;
      int points = (int) (absorbAccum / SHIELD_ABSORB_PER_LXP);
      absorbAccum -= points * SHIELD_ABSORB_PER_LXP;
      collector.record(
          state.setDouble(
              KEY_SHIELD_ABSORB_ACCUM, absorbAccum, value -> Math.max(0.0D, value), 0.0D));
      if (points > 0) {
        grantLxp(
            (Player) victim,
            cc,
            organ,
            state,
            collector,
            points,
            LxpSource.SHIELD,
            level,
            moonPhase,
            gameTime);
      }
    }

    collector.record(
        state.setDouble(
            KEY_WARD, Math.max(0.0D, currentWard), value -> Math.max(0.0D, value), 0.0D));
    collector.record(
        state.setDouble(
            KEY_TEMP_WARD, Math.max(0.0D, tempWard), value -> Math.max(0.0D, value), 0.0D));
    collector.commit();

    if (cc != null) {
      ActiveLinkageContext context = LinkageManager.getContext(cc);
      if (context != null) {
        LinkageChannel channel = LedgerOps.ensureChannel(context, LUNAR_WARD_CHANNEL, NON_NEGATIVE);
        if (channel != null) {
          channel.set(currentWard + tempWard);
        }
      }
    }

    return (float) Math.max(0.0D, remaining);
  }

  @Override
  public float onHit(
      net.minecraft.world.damagesource.DamageSource source,
      LivingEntity attacker,
      LivingEntity target,
      ChestCavityInstance cc,
      ItemStack organ,
      float damage) {
    if (!(attacker instanceof Player player) || attacker.level().isClientSide()) {
      return damage;
    }
    if (!matchesOrgan(organ, ORGAN_ID) || cc == null || organ == null || organ.isEmpty()) {
      return damage;
    }

    OrganState state = organState(organ, STATE_ROOT);
    double activeFactor = state.getDouble(KEY_ACTIVE_FACTOR, 0.0D);
    if (activeFactor <= 0.0D) {
      return damage;
    }

    Level level = player.level();
    long gameTime = level.getGameTime();
    int moonPhase = level.getMoonPhase();
    OrganStateOps.Collector collector = OrganStateOps.collector(cc, organ);

    maybeAwardKillLxp(
        player, cc, organ, state, collector, level, gameTime, moonPhase, target, damage);

    int tier = Mth.clamp(state.getInt(KEY_TIER, 1), 0, MAX_TIER);
    if (tier < 4) {
      collector.commit();
      return damage;
    }

    long lockout = state.getLong(KEY_TIDE_LOCKOUT, 0L);
    if (gameTime < lockout) {
      collector.commit();
      return damage;
    }

    int stacks = Mth.clamp(state.getInt(KEY_TIDE_STACKS, 0), 0, MAX_TIDE_STACKS);
    if (stacks >= MAX_TIDE_STACKS) {
      collector.commit();
      return damage;
    }

    collector.record(
        state.setInt(
            KEY_TIDE_STACKS, stacks + 1, value -> Mth.clamp(value, 0, MAX_TIDE_STACKS), 0));
    collector.record(state.setLong(KEY_TIDE_LAST_GAIN, gameTime, value -> Math.max(0L, value), 0L));
    collector.commit();
    return damage;
  }

  @Override
  public void onRemoved(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
    if (!matchesOrgan(organ, ORGAN_ID)) {
      return;
    }

    AttributeOps.removeById(entity.getAttribute(Attributes.MAX_HEALTH), HEALTH_MODIFIER_ID);
    AttributeOps.removeById(entity.getAttribute(Attributes.MOVEMENT_SPEED), SPEED_MODIFIER_ID);
    EffectOps.remove(entity, MobEffects.JUMP);

    OrganState state = organState(organ, STATE_ROOT);
    state.setDouble(KEY_WARD, 0.0D);
    state.setDouble(KEY_TEMP_WARD, 0.0D);
    state.setDouble(KEY_WARD_CAP, 0.0D);
    state.setDouble(KEY_DAMAGE_REDUCTION, 0.0D);
    state.setDouble(KEY_SPEED_PERCENT, 0.0D);
    state.setInt(KEY_JUMP_LEVEL, 0);
    state.setDouble(KEY_ACTIVE_FACTOR, 0.0D);
    state.setBoolean(KEY_SURGE_READY, false);
    state.setLong(KEY_CURRENT_FULL_MOON_DAY, FULL_MOON_DAY_SENTINEL);
    state.setDouble(KEY_CURRENT_FULL_MOON_ACTIVE_TICKS, 0.0D);

    if (cc != null) {
      ActiveLinkageContext context = LinkageManager.getContext(cc);
      if (context != null) {
        LinkageChannel channel = LedgerOps.ensureChannel(context, LUNAR_WARD_CHANNEL, NON_NEGATIVE);
        if (channel != null) {
          channel.set(0.0D);
        }
      }
    }
    NetworkUtil.sendOrganSlotUpdate(cc, organ);
  }

  public void ensureAttached(ChestCavityInstance cc) {
    LedgerOps.ensureChannel(cc, LUNAR_WARD_CHANNEL, NON_NEGATIVE);
  }

  public void onEquip(
      ChestCavityInstance cc, ItemStack organ, List<OrganRemovalContext> staleRemovalContexts) {
    if (cc == null || organ == null || organ.isEmpty() || !matchesOrgan(organ, ORGAN_ID)) {
      return;
    }
    registerRemovalHook(cc, organ, this, staleRemovalContexts);
    OrganState state = organState(organ, STATE_ROOT);
    int tier = Mth.clamp(state.getInt(KEY_TIER, 1), 0, MAX_TIER);
    if (tier <= 0) {
      state.setInt(KEY_TIER, 1);
    }
    NetworkUtil.sendOrganSlotUpdate(cc, organ);
  }

  private void handleTideStacks(
      Level level,
      OrganState state,
      OrganStateOps.Collector collector,
      int tier,
      double activeFactor,
      long gameTime) {
    if (tier < 4 || activeFactor <= 0.0D) {
      return;
    }
    int stacks = Mth.clamp(state.getInt(KEY_TIDE_STACKS, 0), 0, MAX_TIDE_STACKS);
    long lastGain = state.getLong(KEY_TIDE_LAST_GAIN, 0L);
    long lockout = state.getLong(KEY_TIDE_LOCKOUT, 0L);
    if (gameTime < lockout) {
      return;
    }
    if (stacks >= MAX_TIDE_STACKS) {
      return;
    }
    if (gameTime - lastGain >= TIDE_INTERVAL_TICKS) {
      collector.record(
          state.setInt(
              KEY_TIDE_STACKS, stacks + 1, value -> Mth.clamp(value, 0, MAX_TIDE_STACKS), 0));
      collector.record(
          state.setLong(KEY_TIDE_LAST_GAIN, gameTime, value -> Math.max(0L, value), 0L));
    }
  }

  private void handleSurgeReady(
      OrganState state,
      OrganStateOps.Collector collector,
      int tier,
      double activeFactor,
      long gameTime) {
    if (tier < 5) {
      collector.record(state.setBoolean(KEY_SURGE_READY, false, false));
      return;
    }
    long cooldownUntil = state.getLong(KEY_SURGE_COOLDOWN, 0L);
    boolean ready = state.getBoolean(KEY_SURGE_READY, false);
    if (activeFactor <= 0.0D) {
      if (ready) {
        collector.record(state.setBoolean(KEY_SURGE_READY, false, false));
      }
      return;
    }
    if (gameTime >= cooldownUntil && !ready) {
      collector.record(state.setBoolean(KEY_SURGE_READY, true, false));
    }
  }

  private void maybeAwardKillLxp(
      Player player,
      ChestCavityInstance cc,
      ItemStack organ,
      OrganState state,
      OrganStateOps.Collector collector,
      Level level,
      long gameTime,
      int moonPhase,
      LivingEntity target,
      float damage) {
    if (player == null || state == null || collector == null || level == null) {
      return;
    }
    if (!(target instanceof Mob mob)
        || target == player
        || target.isAlliedTo(player)
        || damage <= 0.0F) {
      return;
    }
    if (!level.isNight() || !willKillTarget(target, damage)) {
      return;
    }
    if (isBossEntity(mob)) {
      long lastBoss = state.getLong(KEY_LAST_BOSS_KILL_TICK, 0L);
      if (gameTime - lastBoss < KILL_LXP_COOLDOWN_TICKS) {
        return;
      }
      GrantOutcome outcome =
          grantLxp(
              player,
              cc,
              organ,
              state,
              collector,
              BOSS_KILL_LXP,
              LxpSource.BATTLE_BOSS,
              level,
              moonPhase,
              gameTime);
      if (outcome.lxpChanged()) {
        collector.record(
            state.setLong(KEY_LAST_BOSS_KILL_TICK, gameTime, value -> Math.max(0L, value), 0L));
      }
      return;
    }
    if (isEliteMob(mob)) {
      long lastElite = state.getLong(KEY_LAST_ELITE_KILL_TICK, 0L);
      if (gameTime - lastElite < KILL_LXP_COOLDOWN_TICKS) {
        return;
      }
      GrantOutcome outcome =
          grantLxp(
              player,
              cc,
              organ,
              state,
              collector,
              ELITE_KILL_LXP,
              LxpSource.BATTLE_ELITE,
              level,
              moonPhase,
              gameTime);
      if (outcome.lxpChanged()) {
        collector.record(
            state.setLong(KEY_LAST_ELITE_KILL_TICK, gameTime, value -> Math.max(0L, value), 0L));
      }
    }
  }

  private boolean willKillTarget(LivingEntity target, float damage) {
    if (target == null) {
      return false;
    }
    float effectiveHealth = target.getHealth() + target.getAbsorptionAmount();
    return damage >= effectiveHealth - 1.0E-4F;
  }

  private void handleProgression(
      Player player,
      ChestCavityInstance cc,
      ItemStack organ,
      OrganState state,
      OrganStateOps.Collector collector,
      int tier,
      double activeFactor,
      long deltaTicks,
      Level level,
      long gameTime,
      int moonPhase) {
    if (player == null || state == null || collector == null) {
      return;
    }
    double clampedFactor = Mth.clamp(activeFactor, 0.0D, 1.0D);
    double accum = Math.max(0.0D, state.getDouble(KEY_ACTIVE_TICK_ACCUM, 0.0D));
    if (deltaTicks > 0L && clampedFactor > 0.0D) {
      accum += deltaTicks * clampedFactor;
    }
    int points = (int) (accum / ACTIVE_TICKS_PER_LXP);
    accum -= points * ACTIVE_TICKS_PER_LXP;
    collector.record(
        state.setDouble(KEY_ACTIVE_TICK_ACCUM, accum, value -> Math.max(0.0D, value), 0.0D));
    if (points > 0) {
      grantLxp(
          player,
          cc,
          organ,
          state,
          collector,
          points,
          LxpSource.ACTIVE,
          level,
          moonPhase,
          gameTime);
    }
    handleFullMoonTracking(state, collector, level, clampedFactor, deltaTicks, moonPhase);
  }

  private void handleFullMoonTracking(
      OrganState state,
      OrganStateOps.Collector collector,
      Level level,
      double activeFactor,
      long deltaTicks,
      int moonPhase) {
    if (state == null || collector == null || level == null) {
      return;
    }
    long currentDay = level.getDayTime() / 24000L;
    long trackedDay = state.getLong(KEY_CURRENT_FULL_MOON_DAY, FULL_MOON_DAY_SENTINEL);
    if (moonPhase == 0) {
      if (trackedDay != currentDay) {
        collector.record(
            state.setLong(
                KEY_CURRENT_FULL_MOON_DAY,
                currentDay,
                value -> Math.max(FULL_MOON_DAY_SENTINEL, value),
                FULL_MOON_DAY_SENTINEL));
        collector.record(
            state.setDouble(
                KEY_CURRENT_FULL_MOON_ACTIVE_TICKS, 0.0D, value -> Math.max(0.0D, value), 0.0D));
      }
      if (deltaTicks > 0L && activeFactor > 0.0D) {
        double current = Math.max(0.0D, state.getDouble(KEY_CURRENT_FULL_MOON_ACTIVE_TICKS, 0.0D));
        current += deltaTicks * Math.min(activeFactor, 1.0D);
        collector.record(
            state.setDouble(
                KEY_CURRENT_FULL_MOON_ACTIVE_TICKS, current, value -> Math.max(0.0D, value), 0.0D));
      }
    } else if (trackedDay != FULL_MOON_DAY_SENTINEL) {
      double current = Math.max(0.0D, state.getDouble(KEY_CURRENT_FULL_MOON_ACTIVE_TICKS, 0.0D));
      collector.record(
          state.setDouble(
              KEY_LAST_FULL_MOON_ACTIVE_TICKS, current, value -> Math.max(0.0D, value), 0.0D));
      collector.record(
          state.setLong(
              KEY_CURRENT_FULL_MOON_DAY,
              FULL_MOON_DAY_SENTINEL,
              value -> Math.max(FULL_MOON_DAY_SENTINEL, value),
              FULL_MOON_DAY_SENTINEL));
      collector.record(
          state.setDouble(
              KEY_CURRENT_FULL_MOON_ACTIVE_TICKS, 0.0D, value -> Math.max(0.0D, value), 0.0D));
    }
  }

  private GrantOutcome grantLxp(
      Player player,
      ChestCavityInstance cc,
      ItemStack organ,
      OrganState state,
      OrganStateOps.Collector collector,
      double baseAmount,
      LxpSource source,
      Level level,
      int moonPhase,
      long gameTime) {
    if (state == null || collector == null || baseAmount <= 0.0D) {
      return GrantOutcome.NONE;
    }
    double scaled = baseAmount * computeLxpMultiplier(moonPhase);
    if (source.isBattle()) {
      long decayUntil = state.getLong(KEY_BATTLE_DECAY_UNTIL, 0L);
      if (gameTime < decayUntil) {
        scaled *= BATTLE_DECAY_MULTIPLIER;
      }
    }
    if (scaled <= LXP_MIN_STEP) {
      return GrantOutcome.NONE;
    }
    double fraction = Math.max(0.0D, state.getDouble(KEY_LXP_FRACTION, 0.0D));
    double total = fraction + scaled;
    int gained = (int) Math.floor(total + LXP_MIN_STEP);
    double remainder = total - gained;
    collector.record(
        state.setDouble(
            KEY_LXP_FRACTION, remainder, value -> Mth.clamp(value, 0.0D, MAX_LXP_FRACTION), 0.0D));
    if (gained <= 0) {
      return new GrantOutcome(false, false);
    }
    int current = Math.max(0, state.getInt(KEY_LXP, 0));
    int newValue = Mth.clamp(current + gained, 0, LXP_CAP);
    collector.record(state.setInt(KEY_LXP, newValue, value -> Mth.clamp(value, 0, LXP_CAP), 0));
    boolean lxpChanged = newValue != current;
    boolean tierChanged = false;
    if (lxpChanged) {
      tierChanged =
          maybeHandleTierUp(player, cc, organ, state, collector, level, gameTime, newValue);
    }
    return new GrantOutcome(lxpChanged, tierChanged);
  }

  private boolean maybeHandleTierUp(
      Player player,
      ChestCavityInstance cc,
      ItemStack organ,
      OrganState state,
      OrganStateOps.Collector collector,
      Level level,
      long gameTime,
      int lxp) {
    int tier = Mth.clamp(state.getInt(KEY_TIER, 1), 1, MAX_TIER);
    boolean tierChanged = false;
    while (tier < MAX_TIER) {
      int nextTier = tier + 1;
      int requirement =
          nextTier < LXP_REQUIREMENTS.length ? LXP_REQUIREMENTS[nextTier] : Integer.MAX_VALUE;
      if (lxp < requirement) {
        break;
      }
      if (!canAdvanceTier(player, state, level, nextTier)) {
        break;
      }
      tier = nextTier;
      collector.record(state.setInt(KEY_TIER, tier, value -> Mth.clamp(value, 1, MAX_TIER), 1));
      collector.record(
          state.setLong(
              KEY_BATTLE_DECAY_UNTIL,
              gameTime + BATTLE_DECAY_DURATION_TICKS,
              value -> Math.max(0L, value),
              0L));
      tierChanged = true;
      if (player instanceof ServerPlayer sp) {
        sp.displayClientMessage(Component.literal("【月光蛊】进化至第" + tier + "转"), true);
      }
    }
    return tierChanged;
  }

  private boolean canAdvanceTier(Player player, OrganState state, Level level, int nextTier) {
    if (nextTier <= 3) {
      return true;
    }
    int stage = resolvePlayerStage(player);
    if (nextTier == 4) {
      return stage >= 1;
    }
    if (nextTier == 5) {
      if (stage < 2) {
        return false;
      }
      double last = Math.max(0.0D, state.getDouble(KEY_LAST_FULL_MOON_ACTIVE_TICKS, 0.0D));
      if (level != null && level.getMoonPhase() == 0) {
        last =
            Math.max(
                last, Math.max(0.0D, state.getDouble(KEY_CURRENT_FULL_MOON_ACTIVE_TICKS, 0.0D)));
      }
      return last + LXP_MIN_STEP >= FULL_MOON_REQUIRED_ACTIVE_TICKS;
    }
    return true;
  }

  private int resolvePlayerStage(Player player) {
    if (player == null) {
      return 0;
    }
    Optional<GuzhenrenResourceBridge.ResourceHandle> handleOpt =
        GuzhenrenResourceBridge.open(player);
    if (handleOpt.isEmpty()) {
      return 0;
    }
    OptionalDouble value = handleOpt.get().read("jieduan");
    return (int) Math.floor(value.orElse(0.0D));
  }

  private double computeLxpMultiplier(int moonPhase) {
    if (moonPhase == 0) {
      return 1.5D;
    }
    if (moonPhase == 4) {
      return 0.5D;
    }
    return 1.0D;
  }

  private boolean isEliteMob(Mob mob) {
    if (mob == null) {
      return false;
    }
    if (isBossEntity(mob)) {
      return false;
    }
    MobCategory category = mob.getType().getCategory();
    if (category != MobCategory.MONSTER && category != MobCategory.MISC) {
      return false;
    }
    if (mob.getType().is(EntityTypeTags.RAIDERS)) {
      return true;
    }
    return mob.getMaxHealth() >= 40.0F;
  }

  private boolean isBossEntity(Mob mob) {
    if (mob == null) {
      return false;
    }
    if (mob instanceof EnderDragon || mob instanceof WitherBoss) {
      return true;
    }
    return mob.getMaxHealth() >= 150.0F;
  }

  private enum LxpSource {
    ACTIVE(false),
    SHIELD(false),
    BATTLE_ELITE(true),
    BATTLE_BOSS(true),
    SURGE(false);

    private final boolean battle;

    LxpSource(boolean battle) {
      this.battle = battle;
    }

    boolean isBattle() {
      return battle;
    }
  }

  private record GrantOutcome(boolean lxpChanged, boolean tierChanged) {
    static final GrantOutcome NONE = new GrantOutcome(false, false);
  }

  private void applySurgeSlow(LivingEntity victim) {
    Level level = victim.level();
    if (!(level instanceof ServerLevel serverLevel)) {
      return;
    }
    AABB area = victim.getBoundingBox().inflate(SURGE_SLOW_RADIUS);
    List<LivingEntity> targets =
        serverLevel.getEntitiesOfClass(
            LivingEntity.class,
            area,
            other -> other != victim && other.isAlive() && !other.isAlliedTo(victim));
    if (targets.isEmpty()) {
      return;
    }
    for (LivingEntity target : targets) {
      target.addEffect(
          new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SURGE_SLOW_DURATION, 0, false, true));
    }
  }

  private boolean isPvpServer(Level level) {
    if (!(level instanceof ServerLevel serverLevel)) {
      return false;
    }
    MinecraftServer server = serverLevel.getServer();
    return server != null && server.isPvpAllowed();
  }

  private record MoonPhaseStats(
      double healthPercent,
      double ward,
      double damageReduction,
      double speedPercent,
      int jumpLevel) {}
}
