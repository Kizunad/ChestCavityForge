package net.tigereye.chestcavity.compat.guzhenren.util;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.tigereye.chestcavity.guzhenren.nudao.GuzhenrenNudaoBridge;
import net.tigereye.chestcavity.guzhenren.nudao.SoulBeastIntimidationHooks;

/**
 * Utility methods for applying intimidation-style减益。
 * 调用方提供血量阈值和关系范围，可在 flow/能力里直接复用。
 */
public final class IntimidationHelper {

    private static final Logger LOGGER = LogManager.getLogger("ChestCavity/Intimidation");

    private IntimidationHelper() {}

    /** Attitude filter for intimidation impact. */
    public enum AttitudeScope {
        HOSTILE,
        NEUTRAL,
        FRIENDLY,
        ALL;

        public boolean includes(Player performer, LivingEntity candidate) {
            return switch (this) {
                case ALL -> true;
                case HOSTILE -> IntimidationHelper.isHostile(candidate);
                case FRIENDLY -> IntimidationHelper.isFriendly(performer, candidate);
                case NEUTRAL -> !IntimidationHelper.isHostile(candidate)
                        && !IntimidationHelper.isFriendly(performer, candidate);
            };
        }

        public static AttitudeScope fromString(String value, AttitudeScope fallback) {
            if (value == null) {
                return fallback;
            }
            String normalised = value.trim().toUpperCase(Locale.ROOT);
            for (AttitudeScope scope : values()) {
                if (scope.name().equals(normalised)) {
                    return scope;
                }
            }
            return fallback;
        }
    }

    public record Settings(
            double healthThreshold,
            AttitudeScope attitude,
            Holder<MobEffect> effect,
            int durationTicks,
            int amplifier,
            boolean ambient,
            boolean showParticles,
            boolean showIcon,
            boolean includeSelf
    ) {
        public Settings {
            healthThreshold = Double.isNaN(healthThreshold) ? 0.0D : Math.max(0.0D, healthThreshold);
            attitude = attitude == null ? AttitudeScope.HOSTILE : attitude;
            effect = effect == null ? MobEffects.WEAKNESS : effect;
            durationTicks = Math.max(1, durationTicks);
            amplifier = Math.max(0, amplifier);
        }

        public static Settings defaultHostile(double thresholdFraction) {
            return new Settings(thresholdFraction, AttitudeScope.HOSTILE, MobEffects.WEAKNESS, 100, 0, false, true, true, false);
        }
    }

    public static int intimidateNearby(Player performer, double radius, Settings settings) {
        if (performer == null || settings == null) {
            return 0;
        }
        if (!(performer.level() instanceof ServerLevel server)) {
            return 0;
        }
        double r = Math.max(0.0D, radius);
        Vec3 origin = performer.position();
        AABB box = new AABB(origin, origin).inflate(r);
        List<LivingEntity> candidates = server.getEntitiesOfClass(LivingEntity.class, box, entity ->
                entity.isAlive() && (settings.includeSelf || entity != performer));
        int applied = 0;
        for (LivingEntity candidate : candidates) {
            if (applyIntimidation(performer, candidate, settings)) {
                applied++;
            }
        }
        return applied;
    }

    public static boolean applyIntimidation(Player performer, LivingEntity candidate, Settings settings) {
        if (performer == null || candidate == null || settings == null) {
            return false;
        }
        if (!candidate.isAlive()) {
            return false;
        }
        if (!settings.includeSelf && candidate == performer) {
            return false;
        }
        if (!settings.attitude.includes(performer, candidate)) {
            return false;
        }
        if (!isBelowThreshold(candidate, settings.healthThreshold)) {
            return false;
        }
        MobEffectInstance instance = new MobEffectInstance(
                settings.effect,
                settings.durationTicks,
                settings.amplifier,
                settings.ambient,
                settings.showParticles,
                settings.showIcon
        );
        return candidate.addEffect(instance);
    }

    public static Holder<MobEffect> resolveEffect(ResourceLocation effectId, Holder<MobEffect> fallback) {
        Holder<MobEffect> defaultEffect = fallback != null ? fallback : MobEffects.WEAKNESS;
        if (effectId == null) {
            return defaultEffect;
        }
        Optional<? extends Holder<MobEffect>> resolved = BuiltInRegistries.MOB_EFFECT.getHolder(effectId);
        if (resolved.isPresent()) {
            return resolved.get();
        }
        LOGGER.warn("[IntimidationHelper] Unknown effect id: {}", effectId);
        return defaultEffect;
    }

    private static boolean isBelowThreshold(LivingEntity entity, double threshold) {
        if (entity == null || threshold <= 0.0D) {
            return false;
        }
        double maxHealth = entity.getMaxHealth();
        if (maxHealth <= 0.0D) {
            return false;
        }
        double absolute = threshold <= 1.0D ? maxHealth * threshold : threshold;
        return entity.getHealth() <= absolute + 1.0E-4D;
    }

    private static boolean isHostile(Entity entity) {
        return entity instanceof Enemy;
    }

    private static boolean isFriendly(Player performer, LivingEntity candidate) {
        if (performer == null || candidate == null) {
            return false;
        }
        if (candidate == performer) {
            return true;
        }
        if (performer.isAlliedTo(candidate)) {
            return true;
        }
        if (candidate instanceof Player other && performer.isAlliedTo(other)) {
            return true;
        }
        if (candidate instanceof TamableAnimal tamable && tamable.isOwnedBy(performer)) {
            return true;
        }
        return GuzhenrenNudaoBridge.openSubject(candidate)
                .map(handle -> handle.isOwnedBy(performer))
                .orElse(false);
    }

    public static boolean isSoulBeastIntimidationActive(Player performer) {
        return SoulBeastIntimidationHooks.isIntimidationEnabled(performer);
    }
}
