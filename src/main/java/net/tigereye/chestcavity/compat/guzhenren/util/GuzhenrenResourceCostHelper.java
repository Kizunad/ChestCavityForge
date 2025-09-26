package net.tigereye.chestcavity.compat.guzhenren.util;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge.ResourceHandle;

import java.util.Objects;
import java.util.OptionalDouble;

/**
 * Centralised helper for consuming Guzhenren resources. Prefers draining
 * zhenyuan/jingli from players via {@link GuzhenrenResourceBridge} and falls
 * back to a health payment for other entities when requested.
 */
public final class GuzhenrenResourceCostHelper {

    private static final double DEFAULT_RESOURCE_TO_HEALTH_RATIO = 100.0;
    private static final float EPSILON = 1.0E-4f;

    private GuzhenrenResourceCostHelper() {
    }

    public static ConsumptionResult consumeStrict(LivingEntity entity, double baseZhenyuanCost, double baseJingliCost) {
        return consume(entity, baseZhenyuanCost, baseJingliCost, DEFAULT_RESOURCE_TO_HEALTH_RATIO, false);
    }

    public static ConsumptionResult consumeWithFallback(LivingEntity entity, double baseZhenyuanCost, double baseJingliCost) {
        return consume(entity, baseZhenyuanCost, baseJingliCost, DEFAULT_RESOURCE_TO_HEALTH_RATIO, true);
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
        ResourceHandle handle = GuzhenrenResourceBridge.open(player).orElse(null);
        if (handle != null) {
            double zhenyuanRequired = 0.0;
            if (zhenyuanCost > 0.0) {
                OptionalDouble zhenyuanRequiredOpt = handle.estimateScaledZhenyuanCost(zhenyuanCost);
                if (zhenyuanRequiredOpt.isEmpty()) {
                    return ConsumptionResult.failure(FailureReason.NON_FINITE_COST);
                }
                zhenyuanRequired = zhenyuanRequiredOpt.getAsDouble();
                OptionalDouble zhenyuanCurrentOpt = handle.getZhenyuan();
                if (zhenyuanCurrentOpt.isEmpty()) {
                    return ConsumptionResult.failure(FailureReason.ATTACHMENT_MISSING_FIELD);
                }
                double available = zhenyuanCurrentOpt.getAsDouble();
                if (!Double.isFinite(available) || available + EPSILON < zhenyuanRequired) {
                    return ConsumptionResult.failure(FailureReason.INSUFFICIENT_ZHENYUAN);
                }
            }

            double jingliRequired = Math.max(0.0, jingliCost);
            if (jingliRequired > 0.0) {
                OptionalDouble jingliCurrentOpt = handle.getJingli();
                if (jingliCurrentOpt.isEmpty()) {
                    return ConsumptionResult.failure(FailureReason.ATTACHMENT_MISSING_FIELD);
                }
                double availableJingli = jingliCurrentOpt.getAsDouble();
                if (!Double.isFinite(availableJingli) || availableJingli + EPSILON < jingliRequired) {
                    return ConsumptionResult.failure(FailureReason.INSUFFICIENT_JINGLI);
                }
            }
            double zhenyuanSpent = 0.0;
            if (zhenyuanRequired > 0.0) {
                zhenyuanSpent = zhenyuanRequired;
                if (handle.adjustZhenyuan(-zhenyuanSpent, true).isEmpty()) {
                    return ConsumptionResult.failure(FailureReason.INSUFFICIENT_ZHENYUAN);
                }
            }

            if (jingliCost > 0.0) {
                if (handle.adjustJingli(-jingliCost, true).isEmpty()) {
                    if (zhenyuanSpent > 0.0) {
                        handle.adjustZhenyuan(zhenyuanSpent, true);
                    }
                    return ConsumptionResult.failure(FailureReason.INSUFFICIENT_JINGLI);
                }
            }

            return ConsumptionResult.successWithResources(zhenyuanSpent, jingliCost);
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

        float startingHealth = entity.getHealth();
        float startingAbsorption = Math.max(0.0f, entity.getAbsorptionAmount());
        float available = startingHealth + startingAbsorption;
        if (available <= healthCost + EPSILON) {
            return ConsumptionResult.failure(FailureReason.INSUFFICIENT_HEALTH);
        }

        entity.invulnerableTime = 0;
        DamageSource damageSource = entity.damageSources().generic();
        entity.hurt(damageSource, healthCost);
        entity.invulnerableTime = 0;

        float remaining = healthCost;
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
}
