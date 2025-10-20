package net.tigereye.chestcavity.compat.guzhenren.item.yue_dao.behavior;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.AbstractGuzhenrenOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.AttributeOps;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.LedgerOps;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.OrganStateOps;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.linkage.ActiveLinkageContext;
import net.tigereye.chestcavity.linkage.LinkageChannel;
import net.tigereye.chestcavity.linkage.LinkageManager;
import net.tigereye.chestcavity.linkage.policy.ClampPolicy;
import net.tigereye.chestcavity.listeners.OrganIncomingDamageListener;
import net.tigereye.chestcavity.listeners.OrganOnHitListener;
import net.tigereye.chestcavity.listeners.OrganRemovalContext;
import net.tigereye.chestcavity.listeners.OrganRemovalListener;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import org.slf4j.Logger;

import java.util.List;
import java.util.Locale;
import java.util.function.DoubleUnaryOperator;

/**
 * Implementation of Moonlight Gu (月光蛊) behaviour.
 */
public final class YueGuangGuOrganBehavior extends AbstractGuzhenrenOrganBehavior implements
        OrganSlowTickListener, OrganIncomingDamageListener, OrganOnHitListener, OrganRemovalListener {

    public static final YueGuangGuOrganBehavior INSTANCE = new YueGuangGuOrganBehavior();

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String MOD_ID = "guzhenren";
    private static final ResourceLocation ORGAN_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "yue_guang_gu");

    private static final ResourceLocation HP_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "modifiers/yue_guang_gu_hp");
    private static final ResourceLocation SPEED_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "modifiers/yue_guang_gu_speed");

    private static final ResourceLocation WARD_CHANNEL_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "lunar_ward");
    private static final ClampPolicy NON_NEGATIVE = new ClampPolicy(0.0D, Double.MAX_VALUE);

    private static final String STATE_ROOT = "YueGuangGu";
    private static final String KEY_TIER = "Tier";
    private static final String KEY_WARD = "Ward";
    private static final String KEY_WARD_MAX = "WardMax";
    private static final String KEY_REGEN_ACCUM = "RegenAccum";
    private static final String KEY_LAST_HIT_TICK = "LastHitTick";
    private static final String KEY_LAST_BREAK_TICK = "LastBreakTick";
    private static final String KEY_LAST_STACK_TICK = "LastStackTick";
    private static final String KEY_TIDE_STACKS = "TideStacks";
    private static final String KEY_TIDE_LOCKOUT = "TideLockout";
    private static final String KEY_SURGE_COOLDOWN = "SurgeCooldown";
    private static final String KEY_SURGE_PRIMED = "SurgePrimed";
    private static final String KEY_TEMP_WARD = "TempWard";

    private static final String KEY_APPLIED_HP = "AppliedHp";
    private static final String KEY_APPLIED_DR = "AppliedDr";
    private static final String KEY_APPLIED_SPEED = "AppliedSpeed";
    private static final String KEY_APPLIED_EXPOSURE = "AppliedExposure";
    private static final String KEY_APPLIED_PHASE = "AppliedPhase";
    private static final String KEY_APPLIED_JUMP = "AppliedJump";

    private static final double BASE_ZHENYUAN_COST = 96.0D;
    private static final double INDOOR_RATIO_BASE = 0.5D;
    private static final double INDOOR_RATIO_L1_BONUS = 0.1D;
    private static final double PVP_MULTIPLIER = 0.75D;
    private static final double DR_SOFT_CAP = 0.18D;

    private static final int MAX_TIER = 5;

    private static final int WARD_REGEN_DELAY_TICKS = 40;
    private static final int WARD_REGEN_INTERVAL_TICKS = 10;
    private static final int WARD_BREAK_NO_REGEN_TICKS = 30;
    private static final double WARD_REGEN_PER_INTERVAL = 1.0D;
    private static final double REGEN_SPEED_L2_BONUS = 0.2D;

    private static final int TIDE_MAX_STACKS = 6;
    private static final int TIDE_STACK_INTERVAL_TICKS = 160;
    private static final int TIDE_LOCKOUT_TICKS = 160;
    private static final double SURGE_DAMAGE_REDUCTION = 0.08D;
    private static final double SURGE_WARD = 2.0D;
    private static final double SURGE_TEMP_WARD = 4.0D;
    private static final int SURGE_COOLDOWN_TICKS = 400;
    private static final int SURGE_SLOW_RADIUS = 4;
    private static final int SURGE_SLOW_DURATION = 40;

    private static final double EPSILON = 1.0E-4D;

    private static final PhaseProfile[] PHASE_PROFILES = {
            new PhaseProfile(0.30D, 8.0D, 0.12D, 0.10D, 1),
            new PhaseProfile(0.22D, 6.0D, 0.09D, 0.07D, 0),
            new PhaseProfile(0.14D, 4.0D, 0.06D, 0.05D, 0),
            new PhaseProfile(0.08D, 2.0D, 0.04D, 0.03D, -1),
            new PhaseProfile(-0.05D, 0.0D, 0.0D, 0.0D, -1),
            new PhaseProfile(0.08D, 2.0D, 0.04D, 0.03D, -1),
            new PhaseProfile(0.14D, 4.0D, 0.06D, 0.05D, 0),
            new PhaseProfile(0.22D, 6.0D, 0.09D, 0.07D, 0)
    };

    private YueGuangGuOrganBehavior() {
    }

    public void ensureAttached(ChestCavityInstance cc) {
        if (cc == null) {
            return;
        }
        ActiveLinkageContext context = LinkageManager.getContext(cc);
        LedgerOps.ensureChannel(context, WARD_CHANNEL_ID, NON_NEGATIVE);
    }

    public void onEquip(ChestCavityInstance cc, ItemStack organ, List<OrganRemovalContext> staleRemovalContexts) {
        if (cc == null || organ == null || organ.isEmpty()) {
            return;
        }
        registerRemovalHook(cc, organ, this, staleRemovalContexts);
        ensureAttached(cc);
        sendSlotUpdate(cc, organ);
    }

    @Override
    public void onRemoved(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (entity == null) {
            return;
        }
        AttributeInstance maxHealth = entity.getAttribute(Attributes.MAX_HEALTH);
        AttributeInstance speed = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        AttributeOps.removeById(maxHealth, HP_MODIFIER_ID);
        AttributeOps.removeById(speed, SPEED_MODIFIER_ID);
        if (cc != null) {
            ActiveLinkageContext context = LinkageManager.getContext(cc);
            if (context != null) {
                LinkageChannel channel = LedgerOps.ensureChannel(context, WARD_CHANNEL_ID, NON_NEGATIVE);
                if (channel != null) {
                    channel.set(0.0D);
                }
            }
        }
        if (organ != null && !organ.isEmpty()) {
            OrganState state = organState(organ, STATE_ROOT);
            state.setDouble(KEY_WARD, 0.0D);
            state.setDouble(KEY_WARD_MAX, 0.0D);
            state.setDouble(KEY_TEMP_WARD, 0.0D);
            state.setDouble(KEY_REGEN_ACCUM, 0.0D);
            state.setDouble(KEY_APPLIED_HP, 0.0D);
            state.setDouble(KEY_APPLIED_DR, 0.0D);
            state.setDouble(KEY_APPLIED_SPEED, 0.0D);
            state.setDouble(KEY_APPLIED_EXPOSURE, 0.0D);
            state.setInt(KEY_TIDE_STACKS, 0);
            state.setBoolean(KEY_SURGE_PRIMED, false);
        }
    }

    @Override
    public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (entity == null || entity.level().isClientSide() || cc == null || organ == null || organ.isEmpty()) {
            return;
        }
        if (!matchesOrgan(organ, ORGAN_ID)) {
            return;
        }

        Level level = entity.level();
        OrganState state = organState(organ, STATE_ROOT);
        OrganStateOps.Collector collector = OrganStateOps.collector(cc, organ);

        int tier = ensureTier(cc, organ, state, collector);
        int stackCount = Math.max(1, organ.getCount());

        double indoorRatio = INDOOR_RATIO_BASE + (tier >= 1 ? INDOOR_RATIO_L1_BONUS : 0.0D);
        double exposure = computeExposure(entity, level, indoorRatio);

        if (exposure > EPSILON) {
            double cost = BASE_ZHENYUAN_COST * stackCount;
            boolean paid = ResourceOps.tryConsumeScaledZhenyuan(entity, cost).isPresent();
            if (!paid) {
                exposure = 0.0D;
            }
        }

        double appliedExposure = exposure;

        int moonPhase = Mth.clamp(level.getMoonPhase(), 0, PHASE_PROFILES.length - 1);
        PhaseProfile profile = PHASE_PROFILES[moonPhase];

        double regenMultiplier = 1.0D;
        double hpBonus = profile.hpBonus();
        double shieldCapacity = profile.shieldCapacity();
        double dr = profile.damageReduction();
        double speedBonus = profile.speedBonus();
        int jumpAmplifier = profile.jumpAmplifier();

        if (tier >= 1) {
            shieldCapacity += 1.0D;
        }
        if (tier >= 2) {
            shieldCapacity += 1.0D;
            regenMultiplier += REGEN_SPEED_L2_BONUS;
        }
        if (tier >= 3) {
            dr += 0.03D;
            speedBonus += 0.03D;
        }
        if (tier >= 4 && moonPhase == 4 && hpBonus < 0.0D) {
            hpBonus = 0.0D;
        }
        if (tier >= 5 && moonPhase == 0) {
            hpBonus = scalePositive(hpBonus, 1.30D);
            shieldCapacity = scalePositive(shieldCapacity, 1.30D);
            dr = scalePositive(dr, 1.30D);
            speedBonus = scalePositive(speedBonus, 1.30D);
            regenMultiplier = scalePositive(regenMultiplier, 1.30D);
        }

        double pvpMultiplier = isPvpEnabled(level) ? PVP_MULTIPLIER : 1.0D;
        double stackScale = Math.max(1.0D, stackCount);

        double appliedHp = hpBonus * exposure * pvpMultiplier * stackScale;
        double appliedShieldCap = shieldCapacity * exposure * pvpMultiplier * stackScale;
        double appliedDr = Math.min(DR_SOFT_CAP, dr * exposure * pvpMultiplier * stackScale);
        double appliedSpeed = speedBonus * exposure * pvpMultiplier * stackScale;

        AttributeInstance maxHealth = entity.getAttribute(Attributes.MAX_HEALTH);
        AttributeInstance movement = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        applyAttributeModifier(maxHealth, HP_MODIFIER_ID, appliedHp);
        applyAttributeModifier(movement, SPEED_MODIFIER_ID, appliedSpeed);

        collector.record(state.setDouble(KEY_APPLIED_HP, appliedHp, value -> value, 0.0D));
        collector.record(state.setDouble(KEY_APPLIED_DR, appliedDr, clampNonNegative(), 0.0D));
        collector.record(state.setDouble(KEY_APPLIED_SPEED, appliedSpeed, value -> value, 0.0D));
        collector.record(state.setDouble(KEY_APPLIED_EXPOSURE, appliedExposure, clampRange(0.0D, 1.0D), 0.0D));
        collector.record(state.setInt(KEY_APPLIED_PHASE, moonPhase, value -> Mth.clamp(value, 0, 7), 0));

        double currentWard = Math.max(0.0D, state.getDouble(KEY_WARD, 0.0D));
        double tempWard = Math.max(0.0D, state.getDouble(KEY_TEMP_WARD, 0.0D));
        double regenAccum = Math.max(0.0D, state.getDouble(KEY_REGEN_ACCUM, 0.0D));
        long now = level.getGameTime();
        long lastHitTick = state.getLong(KEY_LAST_HIT_TICK, Long.MIN_VALUE);
        long lastBreakTick = state.getLong(KEY_LAST_BREAK_TICK, Long.MIN_VALUE);

        if (appliedShieldCap < currentWard) {
            currentWard = appliedShieldCap;
        }
        if (appliedShieldCap <= EPSILON) {
            currentWard = 0.0D;
            tempWard = 0.0D;
            regenAccum = 0.0D;
        } else {
            boolean canRegen = canRegen(now, lastHitTick, lastBreakTick) && exposure > EPSILON;
            if (canRegen && currentWard + tempWard < appliedShieldCap - EPSILON) {
                double regenRatePerSlowTick = (20.0D / WARD_REGEN_INTERVAL_TICKS) * WARD_REGEN_PER_INTERVAL;
                double toAdd = regenRatePerSlowTick * regenMultiplier * exposure * stackScale;
                regenAccum += toAdd;
                double missing = Math.max(0.0D, appliedShieldCap - (currentWard + tempWard));
                if (regenAccum > EPSILON && missing > EPSILON) {
                    double transfer = Math.min(regenAccum, missing);
                    currentWard += transfer;
                    regenAccum -= transfer;
                }
            }
        }

        collector.record(state.setDouble(KEY_WARD, Math.max(0.0D, currentWard), clampNonNegative(), 0.0D));
        collector.record(state.setDouble(KEY_TEMP_WARD, Math.max(0.0D, tempWard), clampNonNegative(), 0.0D));
        collector.record(state.setDouble(KEY_WARD_MAX, Math.max(0.0D, appliedShieldCap), clampNonNegative(), 0.0D));
        collector.record(state.setDouble(KEY_REGEN_ACCUM, Math.max(0.0D, regenAccum), clampNonNegative(), 0.0D));

        updateWardChannel(cc, appliedShieldCap, currentWard + tempWard, computeRegenCooldown(now, lastHitTick, lastBreakTick));

        handleTideStacks(entity, cc, state, collector, tier, exposure, now);
        handleSurgePriming(state, collector, tier, exposure, now);

        applyJump(entity, state, collector, tier, moonPhase, exposure, pvpMultiplier, stackScale, profile.jumpAmplifier());

        if (collector.commit()) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("[compat/guzhenren][yue_guang] synced state for {} (tier={}, exposure={})",
                        describeStack(organ), tier, String.format(Locale.ROOT, "%.3f", exposure));
            }
        }
    }

    @Override
    public float onIncomingDamage(DamageSource source, LivingEntity victim, ChestCavityInstance cc, ItemStack organ, float damage) {
        if (victim == null || victim.level().isClientSide() || cc == null || organ == null || organ.isEmpty()) {
            return damage;
        }
        if (!matchesOrgan(organ, ORGAN_ID)) {
            return damage;
        }
        if (damage <= 0.0F) {
            return damage;
        }

        OrganState state = organState(organ, STATE_ROOT);
        double exposure = state.getDouble(KEY_APPLIED_EXPOSURE, 0.0D);
        if (exposure <= EPSILON) {
            return damage;
        }

        int tier = Mth.clamp(state.getInt(KEY_TIER, 1), 1, MAX_TIER);
        double stackScale = Math.max(1.0D, organ.getCount());
        double tempWard = Math.max(0.0D, state.getDouble(KEY_TEMP_WARD, 0.0D));
        double currentWard = Math.max(0.0D, state.getDouble(KEY_WARD, 0.0D));
        double maxWard = Math.max(0.0D, state.getDouble(KEY_WARD_MAX, 0.0D));
        double appliedDr = Math.max(0.0D, Math.min(DR_SOFT_CAP, state.getDouble(KEY_APPLIED_DR, 0.0D)));
        long now = victim.level().getGameTime();

        double incoming = damage;
        double absorbed = 0.0D;

        OrganStateOps.Collector collector = OrganStateOps.collector(cc, organ);
        boolean surgeTriggered = false;
        if (tier >= 5 && state.getBoolean(KEY_SURGE_PRIMED, false)) {
            tempWard += SURGE_TEMP_WARD * stackScale;
            collector.record(state.setBoolean(KEY_SURGE_PRIMED, false, false));
            long cooldown = now + SURGE_COOLDOWN_TICKS;
            collector.record(state.setLong(KEY_SURGE_COOLDOWN, cooldown, value -> Math.max(0L, value), 0L));
            collector.record(state.setDouble(KEY_TEMP_WARD, tempWard, clampNonNegative(), 0.0D));
            applySurgeSlow(victim);
            spawnSurgePulse(victim);
            surgeTriggered = true;
        }

        if (tempWard > 0.0D) {
            double used = Math.min(tempWard, incoming);
            tempWard -= used;
            incoming -= used;
            absorbed += used;
            collector.record(state.setDouble(KEY_TEMP_WARD, Math.max(0.0D, tempWard), clampNonNegative(), 0.0D));
        }
        if (incoming > 0.0D && currentWard > 0.0D) {
            double used = Math.min(currentWard, incoming);
            currentWard -= used;
            incoming -= used;
            absorbed += used;
            collector.record(state.setDouble(KEY_WARD, Math.max(0.0D, currentWard), clampNonNegative(), 0.0D));
            collector.record(state.setLong(KEY_LAST_HIT_TICK, now, value -> Math.max(0L, value), 0L));
            if (currentWard <= EPSILON) {
                collector.record(state.setLong(KEY_LAST_BREAK_TICK, now, value -> Math.max(0L, value), 0L));
                playShieldBreak(victim);
            }
        } else if (absorbed > 0.0D) {
            collector.record(state.setLong(KEY_LAST_HIT_TICK, now, value -> Math.max(0L, value), 0L));
        }

        double result = incoming;
        if (incoming > 0.0D && appliedDr > 0.0D) {
            result = incoming * (1.0D - appliedDr);
        }

        if (tier >= 4) {
            int stacks = Mth.clamp(state.getInt(KEY_TIDE_STACKS, 0), 0, TIDE_MAX_STACKS);
            long lockout = state.getLong(KEY_TIDE_LOCKOUT, 0L);
            if (stacks >= TIDE_MAX_STACKS && now >= lockout) {
                result *= (1.0D - SURGE_DAMAGE_REDUCTION);
                double wardBonus = SURGE_WARD * stackScale;
                double newWard = Math.min(maxWard, currentWard + wardBonus);
                collector.record(state.setDouble(KEY_WARD, newWard, clampNonNegative(), 0.0D));
                collector.record(state.setInt(KEY_TIDE_STACKS, 0, value -> Mth.clamp(value, 0, TIDE_MAX_STACKS), 0));
                collector.record(state.setLong(KEY_TIDE_LOCKOUT, now + TIDE_LOCKOUT_TICKS, value -> Math.max(0L, value), 0L));
                collector.record(state.setLong(KEY_LAST_STACK_TICK, now, value -> Math.max(0L, value), 0L));
            }
        }

        updateWardChannel(cc, maxWard, Math.max(0.0D, currentWard + tempWard), computeRegenCooldown(now,
                state.getLong(KEY_LAST_HIT_TICK, Long.MIN_VALUE),
                state.getLong(KEY_LAST_BREAK_TICK, Long.MIN_VALUE)));

        collector.commit();

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("[compat/guzhenren][yue_guang] damage handled: incoming={} absorbed={} result={} tier={} surge={}",
                    String.format(Locale.ROOT, "%.2f", damage),
                    String.format(Locale.ROOT, "%.2f", absorbed),
                    String.format(Locale.ROOT, "%.2f", result),
                    tier, surgeTriggered);
        }

        return (float) Math.max(0.0D, result);
    }

    @Override
    public float onHit(DamageSource source, LivingEntity attacker, LivingEntity target, ChestCavityInstance cc, ItemStack organ, float damage) {
        if (attacker == null || attacker.level().isClientSide() || cc == null || organ == null || organ.isEmpty()) {
            return damage;
        }
        if (!matchesOrgan(organ, ORGAN_ID)) {
            return damage;
        }
        OrganState state = organState(organ, STATE_ROOT);
        double exposure = state.getDouble(KEY_APPLIED_EXPOSURE, 0.0D);
        if (exposure <= EPSILON) {
            return damage;
        }
        int tier = Mth.clamp(state.getInt(KEY_TIER, 1), 1, MAX_TIER);
        if (tier < 4) {
            return damage;
        }
        long now = attacker.level().getGameTime();
        int stacks = Mth.clamp(state.getInt(KEY_TIDE_STACKS, 0), 0, TIDE_MAX_STACKS);
        long lockout = state.getLong(KEY_TIDE_LOCKOUT, 0L);
        if (now < lockout || stacks >= TIDE_MAX_STACKS) {
            return damage;
        }
        long lastStackTick = state.getLong(KEY_LAST_STACK_TICK, now);
        if (now - lastStackTick < 5) {
            // Prevent multiple increments within the same tick burst.
            return damage;
        }
        stacks = Math.min(TIDE_MAX_STACKS, stacks + 1);
        state.setInt(KEY_TIDE_STACKS, stacks);
        state.setLong(KEY_LAST_STACK_TICK, now);
        sendSlotUpdate(cc, organ);
        return damage;
    }

    private int ensureTier(ChestCavityInstance cc, ItemStack organ, OrganState state, OrganStateOps.Collector collector) {
        int raw = state.getInt(KEY_TIER, 1);
        int clamped = Mth.clamp(raw, 1, MAX_TIER);
        collector.record(state.setInt(KEY_TIER, clamped, value -> Mth.clamp(value, 1, MAX_TIER), 1));
        if (raw != clamped && LOGGER.isDebugEnabled()) {
            LOGGER.debug("[compat/guzhenren][yue_guang] clamped tier from {} to {} for {}", raw, clamped, describeStack(organ));
        }
        return clamped;
    }

    private void applyAttributeModifier(AttributeInstance attribute, ResourceLocation id, double amount) {
        if (attribute == null) {
            return;
        }
        if (Math.abs(amount) <= EPSILON) {
            AttributeOps.removeById(attribute, id);
            return;
        }
        AttributeModifier modifier = new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        AttributeOps.replaceTransient(attribute, id, modifier);
    }

    private void applyJump(LivingEntity entity, OrganState state, OrganStateOps.Collector collector, int tier, int moonPhase,
                           double exposure, double pvpMultiplier, double stackScale, int baseAmplifier) {
        int amplifier = computeJumpAmplifier(baseAmplifier, exposure * pvpMultiplier * stackScale);
        if (tier >= 1 && moonPhase != 4 && exposure > EPSILON) {
            amplifier = Math.max(amplifier, 0);
        }
        collector.record(state.setInt(KEY_APPLIED_JUMP, amplifier, value -> Mth.clamp(value, -1, 2), -1));
        if (amplifier >= 0) {
            int duration = 80;
            MobEffectInstance effect = new MobEffectInstance(MobEffects.JUMP, duration, amplifier, true, false, true);
            entity.addEffect(effect);
        }
    }

    private int computeJumpAmplifier(int baseAmplifier, double multiplier) {
        if (baseAmplifier < 0 || multiplier <= EPSILON) {
            return -1;
        }
        double scaled = (baseAmplifier + 1) * multiplier;
        int result = Mth.clamp((int) Math.floor(scaled) - 1, -1, 2);
        return result;
    }

    private void handleTideStacks(LivingEntity entity, ChestCavityInstance cc, OrganState state,
                                  OrganStateOps.Collector collector, int tier, double exposure, long now) {
        if (tier < 4 || exposure <= EPSILON) {
            collector.record(state.setInt(KEY_TIDE_STACKS, 0, value -> 0, 0));
            return;
        }
        int stacks = Mth.clamp(state.getInt(KEY_TIDE_STACKS, 0), 0, TIDE_MAX_STACKS);
        long lockout = Math.max(0L, state.getLong(KEY_TIDE_LOCKOUT, 0L));
        long lastStackTick = state.getLong(KEY_LAST_STACK_TICK, now);
        if (now >= lockout && stacks < TIDE_MAX_STACKS) {
            if (now - lastStackTick >= TIDE_STACK_INTERVAL_TICKS) {
                stacks = Math.min(TIDE_MAX_STACKS, stacks + 1);
                lastStackTick = now;
            }
        }
        collector.record(state.setInt(KEY_TIDE_STACKS, stacks, value -> Mth.clamp(value, 0, TIDE_MAX_STACKS), 0));
        collector.record(state.setLong(KEY_LAST_STACK_TICK, lastStackTick, value -> Math.max(0L, value), 0L));
    }

    private void handleSurgePriming(OrganState state, OrganStateOps.Collector collector, int tier,
                                    double exposure, long now) {
        long cooldownUntil = Math.max(0L, state.getLong(KEY_SURGE_COOLDOWN, 0L));
        boolean ready = tier >= 5 && exposure > EPSILON && now >= cooldownUntil;
        collector.record(state.setBoolean(KEY_SURGE_PRIMED, ready, false));
    }

    private void updateWardChannel(ChestCavityInstance cc, double maxWard, double totalWard, double cooldownTicks) {
        if (cc == null) {
            return;
        }
        ActiveLinkageContext context = LinkageManager.getContext(cc);
        if (context == null) {
            return;
        }
        LinkageChannel channel = LedgerOps.ensureChannel(context, WARD_CHANNEL_ID, NON_NEGATIVE);
        if (channel == null) {
            return;
        }
        channel.set(encodeWardChannel(totalWard, maxWard, cooldownTicks));
    }

    private double encodeWardChannel(double current, double max, double cooldownTicks) {
        int currentScaled = (int) Mth.clamp(Math.round(current * 100.0D), 0L, 0xFFFFL);
        int maxScaled = (int) Mth.clamp(Math.round(max * 100.0D), 0L, 0xFFFFL);
        int cooldownScaled = (int) Mth.clamp(Math.round(cooldownTicks), 0L, 0xFFFFL);
        long bits = ((long) cooldownScaled & 0xFFFFL) << 32
                | ((long) maxScaled & 0xFFFFL) << 16
                | ((long) currentScaled & 0xFFFFL);
        return Double.longBitsToDouble(bits);
    }

    private double computeRegenCooldown(long now, long lastHitTick, long lastBreakTick) {
        long untilHit = lastHitTick == Long.MIN_VALUE ? 0L : Math.max(0L, (lastHitTick + WARD_REGEN_DELAY_TICKS) - now);
        long untilBreak = lastBreakTick == Long.MIN_VALUE ? 0L : Math.max(0L, (lastBreakTick + WARD_BREAK_NO_REGEN_TICKS) - now);
        return Math.max(untilHit, untilBreak);
    }

    private boolean canRegen(long now, long lastHitTick, long lastBreakTick) {
        if (lastHitTick != Long.MIN_VALUE && now - lastHitTick < WARD_REGEN_DELAY_TICKS) {
            return false;
        }
        if (lastBreakTick != Long.MIN_VALUE && now - lastBreakTick < WARD_BREAK_NO_REGEN_TICKS) {
            return false;
        }
        return true;
    }

    private void applySurgeSlow(LivingEntity victim) {
        if (!(victim.level() instanceof ServerLevel server)) {
            return;
        }
        Vec3 centre = victim.position();
        AABB area = new AABB(centre, centre).inflate(SURGE_SLOW_RADIUS);
        List<LivingEntity> targets = server.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != null && entity.isAlive() && entity != victim && !entity.isAlliedTo(victim));
        for (LivingEntity target : targets) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SURGE_SLOW_DURATION, 0, true, true, true));
        }
    }

    private void spawnSurgePulse(LivingEntity victim) {
        if (!(victim.level() instanceof ServerLevel server)) {
            return;
        }
        server.sendParticles(ParticleTypes.END_ROD,
                victim.getX(),
                victim.getY() + victim.getBbHeight() * 0.5D,
                victim.getZ(),
                20,
                0.5D,
                0.4D,
                0.5D,
                0.05D);
        victim.level().playSound(null, victim.getX(), victim.getY(), victim.getZ(),
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8F, 1.1F);
    }

    private void playShieldBreak(LivingEntity victim) {
        victim.level().playSound(null, victim.getX(), victim.getY(), victim.getZ(),
                SoundEvents.SHIELD_BREAK, SoundSource.PLAYERS, 0.6F, 1.2F);
    }

    private boolean isPvpEnabled(Level level) {
        if (level == null || level.getServer() == null) {
            return false;
        }
        return level.getServer().isPvpAllowed();
    }

    private double computeExposure(LivingEntity entity, Level level, double indoorRatio) {
        if (level == null || !level.dimensionType().hasSkyLight()) {
            return 0.0D;
        }
        if (level.isDay()) {
            return 0.0D;
        }
        BlockPos pos = entity.blockPosition();
        boolean canSeeSky = level.canSeeSkyFromBelowWater(pos);
        int blockLight = level.getBrightness(LightLayer.BLOCK, pos);
        int skyLight = level.getBrightness(LightLayer.SKY, pos);
        int rawBrightness = Math.max(blockLight, skyLight - level.getSkyDarken());
        boolean darkEnough = rawBrightness <= 7;
        if (canSeeSky && darkEnough) {
            return 1.0D;
        }
        return Mth.clamp(indoorRatio, 0.0D, 1.0D);
    }

    private DoubleUnaryOperator clampNonNegative() {
        return value -> Math.max(0.0D, value);
    }

    private DoubleUnaryOperator clampRange(double min, double max) {
        return value -> Mth.clamp(value, min, max);
    }

    private double scalePositive(double value, double multiplier) {
        if (value <= 0.0D) {
            return value;
        }
        return value * multiplier;
    }

    private record PhaseProfile(double hpBonus, double shieldCapacity, double damageReduction, double speedBonus,
                                int jumpAmplifier) {
    }
}
