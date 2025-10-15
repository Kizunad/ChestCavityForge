package net.tigereye.chestcavity.guzhenren.util;

import com.mojang.logging.LogUtils;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge.ResourceHandle;
import org.slf4j.Logger;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Consumer;

/**
 * Centralised helper for consuming Guzhenren resources. Prefers draining
 * zhenyuan/jingli from players via {@link GuzhenrenResourceBridge} and falls
 * back to a health payment for other entities when requested.
 * <p>
 * Relocated from the compat namespace alongside the new guzhenren util module.
 */
public final class GuzhenrenResourceCostHelper {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final double DEFAULT_RESOURCE_TO_HEALTH_RATIO = 100.0;
    private static final float EPSILON = 1.0E-4f;

    private static volatile boolean debugEnabled = Boolean.getBoolean("chestcavity.guzhenren.resources.debug");

    private GuzhenrenResourceCostHelper() {
    }

    /** Enables or disables debug level logging for helper operations. */
    public static void setDebugEnabled(boolean enabled) {
        debugEnabled = enabled;
    }

    /** Returns whether debug level logging is currently enabled. */
    public static boolean isDebugEnabled() {
        return debugEnabled;
    }

    /** Executes the provided consumer if the player exposes a Guzhenren resource handle. */
    public static boolean withHandle(Player player, Consumer<ResourceHandle> consumer) {
        Objects.requireNonNull(consumer, "consumer");
        if (player == null) {
            debug("withHandle called with null player");
            return false;
        }
        Optional<ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
        if (handleOpt.isEmpty()) {
            debug("withHandle could not acquire handle for {}", player.getName().getString());
            return false;
        }
        consumer.accept(handleOpt.get());
        return true;
    }

    public static ConsumptionResult consumeStrict(LivingEntity entity, double baseZhenyuanCost, double baseJingliCost) {
        return consume(entity, baseZhenyuanCost, baseJingliCost, DEFAULT_RESOURCE_TO_HEALTH_RATIO, false);
    }

    public static ConsumptionResult consumeWithFallback(LivingEntity entity, double baseZhenyuanCost, double baseJingliCost) {
        return consume(entity, baseZhenyuanCost, baseJingliCost, DEFAULT_RESOURCE_TO_HEALTH_RATIO, true);
    }

    /**
     * Consumes hunpo (魂魄) from the given entity.
     * - For players with Guzhenren attachment, deducts from the {@code hunpo} field (clamped to [0, max]).
     * - For non-players, optionally falls back to draining health if {@code allowHealthFallback} is true.
     *
     * The health fallback uses the same {@link #DEFAULT_RESOURCE_TO_HEALTH_RATIO} as zhenyuan/jingli.
     */
    public static ConsumptionResult consumeHunpo(LivingEntity entity, double hunpoCost, boolean allowHealthFallback) {
        if (entity == null || !entity.isAlive()) {
            return ConsumptionResult.failure(FailureReason.ENTITY_INVALID);
        }
        double cost = sanitiseCost(hunpoCost);
        if (cost <= 0.0) {
            return ConsumptionResult.successWithResources(0.0, 0.0);
        }

        if (entity instanceof Player player) {
            Optional<ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
            if (handleOpt.isEmpty()) {
                return allowHealthFallback
                        ? consumeWithHealth(player, cost, 0.0, DEFAULT_RESOURCE_TO_HEALTH_RATIO)
                        : ConsumptionResult.failure(FailureReason.ATTACHMENT_MISSING_HANDLE);
            }
            ResourceHandle handle = handleOpt.get();
            double current = handle.read("hunpo").orElse(0.0);
            if (!Double.isFinite(current) || current + EPSILON < cost) {
                return ConsumptionResult.failure(FailureReason.INSUFFICIENT_HUNPO);
            }
            if (handle.adjustDouble("hunpo", -cost, true, "zuida_hunpo").isEmpty()) {
                return ConsumptionResult.failure(FailureReason.INSUFFICIENT_HUNPO);
            }
            debug("{} consumed hunpo: cost={}, remaining={}", player.getName().getString(), cost,
                    handle.read("hunpo").orElse(Double.NaN));
            return ConsumptionResult.successWithResources(0.0, 0.0);
        }

        if (!allowHealthFallback) {
            return ConsumptionResult.failure(FailureReason.HEALTH_FALLBACK_DISABLED);
        }
        // Treat 1 hunpo as 1 resource unit with the standard conversion ratio
        return consumeWithHealth(entity, cost, 0.0, DEFAULT_RESOURCE_TO_HEALTH_RATIO);
    }

    /** Strictly consumes hunpo from the entity without allowing health fallback. */
    public static ConsumptionResult consumeStrict(LivingEntity entity, double hunpoCost) {
        return consumeHunpo(entity, hunpoCost, false);
    }

    public static ConsumptionResult consume(
            LivingEntity entity,
            double baseZhenyuanCost,
            double baseJingliCost,
            double resourceToHealthRatio,
            boolean allowHealthFallback
    ) {
        if (entity == null || !entity.isAlive()) {
            return ConsumptionResult.failure(FailureReason.ENTITY_INVALID);
        }
        double zCost = sanitiseCost(baseZhenyuanCost);
        double jCost = sanitiseCost(baseJingliCost);
        if (zCost <= 0.0 && jCost <= 0.0) {
            return ConsumptionResult.successWithResources(0.0, 0.0);
        }

        if (entity instanceof Player player) {
            return consumeForPlayer(player, zCost, jCost, resourceToHealthRatio, allowHealthFallback);
        }

        if (!allowHealthFallback) {
            return ConsumptionResult.failure(FailureReason.HEALTH_FALLBACK_DISABLED);
        }
        return consumeWithHealth(entity, zCost, jCost, resourceToHealthRatio);
    }

    private static ConsumptionResult consumeForPlayer(
            Player player,
            double zhenyuanCost,
            double jingliCost,
            double ratio,
            boolean allowHealthFallback
    ) {
        Optional<ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
        if (handleOpt.isPresent()) {
            ResourceHandle handle = handleOpt.get();
            try (ResourceTransaction transaction = new ResourceTransaction(player, handle)) {
                FailureReason failure = transaction.spendZhenyuan(zhenyuanCost);
                if (failure != null) {
                    return ConsumptionResult.failure(failure);
                }
                failure = transaction.spendJingli(jingliCost);
                if (failure != null) {
                    return ConsumptionResult.failure(failure);
                }
                ConsumptionResult result = transaction.commit();
                debug("consumeForPlayer succeeded for {}: zhenyuanSpent={}, jingliSpent={}",
                        player.getName().getString(), result.zhenyuanSpent(), result.jingliSpent());
                return result;
            }
        }

        if (!allowHealthFallback) {
            return ConsumptionResult.failure(FailureReason.ATTACHMENT_MISSING_HANDLE);
        }
        return consumeWithHealth(player, zhenyuanCost, jingliCost, ratio);
    }

    private static ConsumptionResult consumeWithHealth(LivingEntity entity, double zhenyuanCost, double jingliCost, double ratio) {
        double combinedCost = Math.max(0.0, zhenyuanCost) + Math.max(0.0, jingliCost);
        if (!Double.isFinite(combinedCost) || combinedCost <= 0.0) {
            return ConsumptionResult.failure(FailureReason.NON_FINITE_COST);
        }
        if (!Double.isFinite(ratio) || ratio <= 0.0) {
            return ConsumptionResult.failure(FailureReason.NON_FINITE_COST);
        }
        float healthCost = (float) (combinedCost / ratio);
        if (!Float.isFinite(healthCost) || healthCost <= 0.0f) {
            return ConsumptionResult.failure(FailureReason.NON_FINITE_COST);
        }

        boolean drained = drainHealth(entity, healthCost, EPSILON, entity.damageSources().generic());
        if (!drained) {
            return ConsumptionResult.failure(FailureReason.INSUFFICIENT_HEALTH);
        }

        return ConsumptionResult.successWithHealth(healthCost);
    }

    private static double sanitiseCost(double value) {
        return Double.isFinite(value) ? Math.max(0.0, value) : 0.0;
    }

    public enum Mode {
        PLAYER_RESOURCES,
        HEALTH_FALLBACK
    }

    public enum FailureReason {
        NONE,
        ENTITY_INVALID,
        NON_FINITE_COST,
        ATTACHMENT_MISSING_HANDLE,
        ATTACHMENT_MISSING_FIELD,
        INSUFFICIENT_HUNPO,
        INSUFFICIENT_ZHENYUAN,
        INSUFFICIENT_JINGLI,
        INSUFFICIENT_HEALTH,
        HEALTH_FALLBACK_DISABLED
    }

    public record ConsumptionResult(
            boolean succeeded,
            double zhenyuanSpent,
            double jingliSpent,
            float healthSpent,
            Mode mode,
            FailureReason failureReason
    ) {
        public static ConsumptionResult successWithResources(double zhenyuanSpent, double jingliSpent) {
            return new ConsumptionResult(true, zhenyuanSpent, jingliSpent, 0.0f, Mode.PLAYER_RESOURCES, FailureReason.NONE);
        }

        public static ConsumptionResult successWithHealth(float healthSpent) {
            return new ConsumptionResult(true, 0.0, 0.0, healthSpent, Mode.HEALTH_FALLBACK, FailureReason.NONE);
        }

        public static ConsumptionResult failure(FailureReason reason) {
            Objects.requireNonNull(reason, "reason");
            return new ConsumptionResult(false, 0.0, 0.0, 0.0f, null, reason);
        }
    }

    /** Attempts to refund the provided resource consumption back to the player. */
    public static boolean refund(Player player, ConsumptionResult result) {
        if (player == null || result == null) {
            return false;
        }
        if (result.mode() != Mode.PLAYER_RESOURCES) {
            return true;
        }
        final boolean[] success = {true};
        boolean executed = withHandle(player, handle -> {
            if (result.zhenyuanSpent() > 0.0) {
                success[0] &= handle.adjustZhenyuan(result.zhenyuanSpent(), true).isPresent();
            }
            if (result.jingliSpent() > 0.0) {
                success[0] &= handle.adjustJingli(result.jingliSpent(), true).isPresent();
            }
            debug("Refunded resources for {}: zhenyuan={}, jingli={}",
                    player.getName().getString(), result.zhenyuanSpent(), result.jingliSpent());
        });
        return executed && success[0];
    }

    /** Drains health and absorption while maintaining vanilla rollback guarantees. */
    public static boolean drainHealth(LivingEntity entity, float amount, float minimumReserve, DamageSource source) {
        if (entity == null || amount <= 0.0f) {
            return true;
        }
        float startingHealth = entity.getHealth();
        float startingAbsorption = Math.max(0.0f, entity.getAbsorptionAmount());
        float available = startingHealth + startingAbsorption;
        float reserve = Math.max(0.0f, minimumReserve);
        float postDrain = available - amount;
        if (reserve > 0.0f) {
            if (postDrain < reserve) {
                debug("Health drain rejected for {} (available={}, amount={}, reserve={})",
                        entity.getName().getString(), available, amount, reserve);
                return false;
            }
        } else if (postDrain <= 0.0f) {
            debug("Health drain rejected for {} (available={}, amount={}, reserve={})",
                    entity.getName().getString(), available, amount, reserve);
            return false;
        }

        entity.invulnerableTime = 0;
        DamageSource appliedSource = source == null ? entity.damageSources().generic() : source;
        entity.hurt(appliedSource, amount);
        entity.invulnerableTime = 0;

        float remaining = amount;
        float absorptionConsumed = Math.min(startingAbsorption, remaining);
        remaining -= absorptionConsumed;
        float targetAbsorption = Math.max(0.0f, startingAbsorption - absorptionConsumed);

        if (!entity.isDeadOrDying()) {
            entity.setAbsorptionAmount(targetAbsorption);
            if (remaining > 0.0f) {
                float targetHealth = Math.max(0.0f, startingHealth - remaining);
                if (entity.getHealth() > targetHealth) {
                    entity.setHealth(targetHealth);
                }
            }
            entity.hurtTime = 0;
            entity.hurtDuration = 0;
        }

        debug("Drained health for {}: amount={}, remainingHealth={}, remainingAbsorption={}",
                entity.getName().getString(), amount, entity.getHealth(), entity.getAbsorptionAmount());
        return true;
    }

    public static boolean drainHealth(LivingEntity entity, float amount, DamageSource source) {
        return drainHealth(entity, amount, 0.0f, source);
    }

    public static boolean drainHealth(LivingEntity entity, float amount) {
        return drainHealth(entity, amount, 0.0f, entity == null ? null : entity.damageSources().generic());
    }

    private static void debug(String message, Object... args) {
        if (debugEnabled) {
            LOGGER.debug(message, args);
        }
    }

    private static final class ResourceTransaction implements AutoCloseable {
        private final Player player;
        private final ResourceHandle handle;
        private double zhenyuanSpent;
        private double jingliSpent;
        private boolean committed;

        private ResourceTransaction(Player player, ResourceHandle handle) {
            this.player = player;
            this.handle = handle;
        }

        private FailureReason spendZhenyuan(double baseCost) {
            double cost = sanitiseCost(baseCost);
            if (cost <= 0.0) {
                return null;
            }
            OptionalDouble beforeOpt = handle.getZhenyuan();
            if (beforeOpt.isEmpty()) {
                return FailureReason.ATTACHMENT_MISSING_FIELD;
            }
            double before = beforeOpt.getAsDouble();
            if (!Double.isFinite(before)) {
                return FailureReason.NON_FINITE_COST;
            }
            OptionalDouble afterOpt = handle.consumeScaledZhenyuan(cost);
            if (afterOpt.isEmpty()) {
                return FailureReason.INSUFFICIENT_ZHENYUAN;
            }
            double after = afterOpt.getAsDouble();
            double spent = before - after;
            if (!(Double.isFinite(spent) && spent > 0.0)) {
                return FailureReason.INSUFFICIENT_ZHENYUAN;
            }
            zhenyuanSpent += spent;
            debug("{} consumed zhenyuan: baseCost={}, spent={}, remaining={}",
                    player.getName().getString(), cost, spent, after);
            return null;
        }

        private FailureReason spendJingli(double baseCost) {
            double cost = sanitiseCost(baseCost);
            if (cost <= 0.0) {
                return null;
            }
            OptionalDouble beforeOpt = handle.getJingli();
            if (beforeOpt.isEmpty()) {
                return FailureReason.ATTACHMENT_MISSING_FIELD;
            }
            double before = beforeOpt.getAsDouble();
            if (!Double.isFinite(before) || before + EPSILON < cost) {
                return FailureReason.INSUFFICIENT_JINGLI;
            }
            // 若拥有“精力消耗减少”效果，则按配置倍率降低消耗（默认0.7倍成本 → 30%减耗）。
            try {
                if (player != null && player.hasEffect(net.tigereye.chestcavity.registration.CCStatusEffects.HLTN_STAMINA_REDUCE)) {
                    // 采用行为级配置，若未提供则默认0.7
                    double multiplier = net.tigereye.chestcavity.compat.guzhenren.util.behavior.BehaviorConfigAccess
                            .getFloat(net.tigereye.chestcavity.compat.guzhenren.item.li_dao.behavior.HuangLuoTianNiuGuOrganBehavior.class,
                                    "ACTIVE_STAMINA_REDUCTION_MULTIPLIER", 0.7f);
                    multiplier = Math.max(0.0D, Math.min(1.0D, multiplier));
                    cost *= multiplier;
                }
            } catch (Throwable ignored) {}
            OptionalDouble afterOpt = handle.adjustJingli(-cost, true);
            if (afterOpt.isEmpty()) {
                return FailureReason.INSUFFICIENT_JINGLI;
            }
            double after = afterOpt.getAsDouble();
            double spent = before - after;
            if (!(Double.isFinite(spent) && spent >= cost - EPSILON)) {
                return FailureReason.INSUFFICIENT_JINGLI;
            }
            jingliSpent += spent;
            debug("{} consumed jingli: cost={}, spent={}, remaining={}",
                    player.getName().getString(), cost, spent, after);
            return null;
        }

        private ConsumptionResult commit() {
            committed = true;
            return ConsumptionResult.successWithResources(zhenyuanSpent, jingliSpent);
        }

        @Override
        public void close() {
            if (!committed) {
                rollback();
            }
        }

        private void rollback() {
            if (zhenyuanSpent > 0.0) {
                handle.adjustZhenyuan(zhenyuanSpent, true);
            }
            if (jingliSpent > 0.0) {
                handle.adjustJingli(jingliSpent, true);
            }
            if (zhenyuanSpent > 0.0 || jingliSpent > 0.0) {
                debug("Rolled back transaction for {} (zhenyuan={}, jingli={})",
                        player.getName().getString(), zhenyuanSpent, jingliSpent);
            }
        }
    }
}
