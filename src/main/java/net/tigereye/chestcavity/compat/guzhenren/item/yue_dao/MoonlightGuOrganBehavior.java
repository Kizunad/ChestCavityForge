package net.tigereye.chestcavity.compat.guzhenren.item.yue_dao;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
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

import java.util.List;
import java.util.Optional;

/**
 * 月光蛊 - 夜月增益器官。
 */
public final class MoonlightGuOrganBehavior extends AbstractGuzhenrenOrganBehavior
        implements OrganSlowTickListener, OrganIncomingDamageListener, OrganOnHitListener, OrganRemovalListener {

    public static final MoonlightGuOrganBehavior INSTANCE = new MoonlightGuOrganBehavior();

    private static final String MOD_ID = "guzhenren";
    private static final ResourceLocation ORGAN_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "yue_guang_gu");
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

    private static final ClampPolicy NON_NEGATIVE = new ClampPolicy(0.0D, Double.MAX_VALUE);

    private static final MoonPhaseStats[] PHASE_STATS = new MoonPhaseStats[]{
            new MoonPhaseStats(0.30D, 8.0D, 0.12D, 0.10D, 2), // 0 满月
            new MoonPhaseStats(0.22D, 6.0D, 0.09D, 0.07D, 1), // 1 凸月
            new MoonPhaseStats(0.14D, 4.0D, 0.06D, 0.05D, 1), // 2 上弦
            new MoonPhaseStats(0.08D, 2.0D, 0.04D, 0.03D, 0), // 3 娥眉
            new MoonPhaseStats(-0.05D, 0.0D, 0.00D, 0.00D, 0), // 4 朔月
            new MoonPhaseStats(0.08D, 2.0D, 0.04D, 0.03D, 0), // 5 娥眉
            new MoonPhaseStats(0.14D, 4.0D, 0.06D, 0.05D, 1), // 6 下弦
            new MoonPhaseStats(0.22D, 6.0D, 0.09D, 0.07D, 1)  // 7 凸月
    };

    private MoonlightGuOrganBehavior() {
    }

    @Override
    public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (!(entity instanceof Player player) || entity.level().isClientSide() || cc == null || organ == null || organ.isEmpty()) {
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
            collector.record(state.setInt(KEY_TIER, storedTier, value -> Mth.clamp(value, 0, MAX_TIER), 1));
        }
        final int tier = storedTier;

        boolean isNight = level.isNight();
        BlockPos pos = entity.blockPosition();
        boolean skyVisible = level.canSeeSky(pos.above());
        int brightness = level.getMaxLocalRawBrightness(pos);

        double indoorRatio = tier >= 1 ? INDOOR_L1_RATIO : INDOOR_BASE_RATIO;
        double activeFactor = 0.0D;
        if (isNight && brightness <= 7) {
            double candidate = skyVisible ? 1.0D : indoorRatio;
            if (candidate > EPSILON) {
                Optional<GuzhenrenResourceBridge.ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
                if (handleOpt.isPresent()
                        && ResourceOps.tryConsumeScaledZhenyuan(handleOpt.get(), BASE_COST).isPresent()) {
                    activeFactor = candidate;
                }
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
                AttributeModifier modifier = new AttributeModifier(HEALTH_MODIFIER_ID, healthPercent, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
                AttributeOps.replaceTransient(maxHealthAttr, HEALTH_MODIFIER_ID, modifier);
            } else {
                AttributeOps.removeById(maxHealthAttr, HEALTH_MODIFIER_ID);
            }
        }

        AttributeInstance speedAttr = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttr != null) {
            if (Math.abs(speedPercent) > EPSILON) {
                AttributeModifier modifier = new AttributeModifier(SPEED_MODIFIER_ID, speedPercent, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
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
        boolean canRegen = wardCap > 0.0D && currentWard < wardCap && activeFactor > 0.0D
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
        collector.record(state.setDouble(KEY_TEMP_WARD, tempWard, value -> Math.max(0.0D, value), 0.0D));
        collector.record(state.setDouble(KEY_WARD_CAP, wardCap, value -> Math.max(0.0D, value), 0.0D));
        collector.record(state.setDouble(KEY_DAMAGE_REDUCTION, damageReduction, value -> Mth.clamp(value, 0.0D, DR_SOFT_CAP), 0.0D));
        collector.record(state.setDouble(KEY_SPEED_PERCENT, speedPercent, value -> value, 0.0D));
        collector.record(state.setInt(KEY_JUMP_LEVEL, jumpLevel, value -> Mth.clamp(value, 0, 2), 0));
        collector.record(state.setDouble(KEY_ACTIVE_FACTOR, activeFactor, value -> Mth.clamp(value, 0.0D, 1.0D), 0.0D));
        collector.record(state.setInt(KEY_MOON_PHASE, moonPhase, value -> Math.max(0, value), 0));
        collector.record(state.setInt(KEY_BRIGHTNESS, brightness, value -> Math.max(0, value), 0));
        collector.record(state.setBoolean(KEY_SKY_VISIBLE, skyVisible, false));
        collector.record(state.setLong(KEY_LAST_SLOW_TICK, gameTime, value -> Math.max(0L, value), 0L));

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
    public float onIncomingDamage(net.minecraft.world.damagesource.DamageSource source, LivingEntity victim, ChestCavityInstance cc, ItemStack organ, float damage) {
        if (!(victim instanceof Player) || victim.level().isClientSide() || cc == null || organ == null || organ.isEmpty()) {
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

        OrganState state = organState(organ, STATE_ROOT);
        OrganStateOps.Collector collector = OrganStateOps.collector(cc, organ);

        int tier = Mth.clamp(state.getInt(KEY_TIER, 1), 0, MAX_TIER);
        double activeFactor = state.getDouble(KEY_ACTIVE_FACTOR, 0.0D);

        double tempWard = Math.max(0.0D, state.getDouble(KEY_TEMP_WARD, 0.0D));
        double currentWard = Math.max(0.0D, state.getDouble(KEY_WARD, 0.0D));
        double wardCap = Math.max(0.0D, state.getDouble(KEY_WARD_CAP, 0.0D));
        double damageReduction = Mth.clamp(state.getDouble(KEY_DAMAGE_REDUCTION, 0.0D), 0.0D, DR_SOFT_CAP);

        double remaining = damage;
        double absorbed = 0.0D;

        if (tier >= 5 && activeFactor > 0.0D && state.getBoolean(KEY_SURGE_READY, false)) {
            tempWard += SURGE_TEMP_WARD;
            collector.record(state.setBoolean(KEY_SURGE_READY, false, false));
            collector.record(state.setLong(KEY_SURGE_COOLDOWN, gameTime + SURGE_COOLDOWN_TICKS, value -> Math.max(0L, value), 0L));
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
                collector.record(state.setLong(KEY_LAST_BREAK_TICK, gameTime, value -> Math.max(0L, value), 0L));
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
                collector.record(state.setInt(KEY_TIDE_STACKS, 0, value -> Mth.clamp(value, 0, MAX_TIDE_STACKS), 0));
                collector.record(state.setLong(KEY_TIDE_LOCKOUT, gameTime + TIDE_LOCKOUT_TICKS, value -> Math.max(0L, value), 0L));
                collector.record(state.setLong(KEY_TIDE_LAST_GAIN, gameTime, value -> Math.max(0L, value), 0L));
            }
        }

        if (absorbed > 0.0D || remaining < damage) {
            collector.record(state.setLong(KEY_LAST_DAMAGE_TICK, gameTime, value -> Math.max(0L, value), 0L));
        }

        collector.record(state.setDouble(KEY_WARD, Math.max(0.0D, currentWard), value -> Math.max(0.0D, value), 0.0D));
        collector.record(state.setDouble(KEY_TEMP_WARD, Math.max(0.0D, tempWard), value -> Math.max(0.0D, value), 0.0D));
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
    public float onHit(net.minecraft.world.damagesource.DamageSource source, LivingEntity attacker, LivingEntity target, ChestCavityInstance cc, ItemStack organ, float damage) {
        if (!(attacker instanceof Player) || attacker.level().isClientSide()) {
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

        int tier = Mth.clamp(state.getInt(KEY_TIER, 1), 0, MAX_TIER);
        if (tier < 4) {
            return damage;
        }

        long gameTime = attacker.level().getGameTime();
        long lockout = state.getLong(KEY_TIDE_LOCKOUT, 0L);
        if (gameTime < lockout) {
            return damage;
        }

        int stacks = Mth.clamp(state.getInt(KEY_TIDE_STACKS, 0), 0, MAX_TIDE_STACKS);
        if (stacks >= MAX_TIDE_STACKS) {
            return damage;
        }

        OrganStateOps.Collector collector = OrganStateOps.collector(cc, organ);
        collector.record(state.setInt(KEY_TIDE_STACKS, stacks + 1, value -> Mth.clamp(value, 0, MAX_TIDE_STACKS), 0));
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

    public void onEquip(ChestCavityInstance cc, ItemStack organ, List<OrganRemovalContext> staleRemovalContexts) {
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

    private void handleTideStacks(Level level, OrganState state, OrganStateOps.Collector collector,
                                  int tier, double activeFactor, long gameTime) {
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
            collector.record(state.setInt(KEY_TIDE_STACKS, stacks + 1, value -> Mth.clamp(value, 0, MAX_TIDE_STACKS), 0));
            collector.record(state.setLong(KEY_TIDE_LAST_GAIN, gameTime, value -> Math.max(0L, value), 0L));
        }
    }

    private void handleSurgeReady(OrganState state, OrganStateOps.Collector collector,
                                  int tier, double activeFactor, long gameTime) {
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

    private void applySurgeSlow(LivingEntity victim) {
        Level level = victim.level();
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        AABB area = victim.getBoundingBox().inflate(SURGE_SLOW_RADIUS);
        List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, area,
                other -> other != victim && other.isAlive() && !other.isAlliedTo(victim));
        if (targets.isEmpty()) {
            return;
        }
        for (LivingEntity target : targets) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SURGE_SLOW_DURATION, 0, false, true));
        }
    }

    private boolean isPvpServer(Level level) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }
        MinecraftServer server = serverLevel.getServer();
        return server != null && server.isPvpAllowed();
    }

    private record MoonPhaseStats(double healthPercent, double ward, double damageReduction, double speedPercent, int jumpLevel) {
    }
}

