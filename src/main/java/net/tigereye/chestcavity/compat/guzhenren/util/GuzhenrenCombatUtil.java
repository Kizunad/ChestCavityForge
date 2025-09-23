package net.tigereye.chestcavity.compat.guzhenren.util;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Shared combat helpers for Guzhenren organ behaviours.
 */
public final class GuzhenrenCombatUtil {

    private GuzhenrenCombatUtil() {
    }

    /**
     * Performs a short horizontal dodge for the target. The direction is based on the attacker's look
     * vector when available, otherwise a random heading is used. The motion is applied via {@link LivingEntity#push}
     * so that vanilla movement syncing is preserved.
     */
    public static boolean performShortDodge(
            LivingEntity target,
            LivingEntity attacker,
            float minDistance,
            float maxDistance,
            float yawRangeDeg,
            SoundEvent sound,
            ParticleOptions particle
    ) {
        if (target == null) {
            return false;
        }
        Level level = target.level();
        RandomSource random = target.getRandom();
        Vec3 base = attacker != null ? attacker.getLookAngle() : target.getLookAngle();
        Vec3 horizontal = new Vec3(base.x, 0.0, base.z);
        if (horizontal.lengthSqr() < 1.0E-6) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            horizontal = new Vec3(Math.cos(angle), 0.0, Math.sin(angle));
        }
        float yawOffset = (random.nextFloat() - 0.5f) * yawRangeDeg;
        Vec3 rotated = horizontal.normalize().yRot((float) Math.toRadians(yawOffset));
        double distance = Mth.lerp(random.nextDouble(), minDistance, maxDistance);
        Vec3 motion = rotated.scale(distance);
        target.push(motion.x, 0.05, motion.z);
        if (level instanceof ServerLevel server) {
            ParticleOptions particleType = particle != null ? particle : ParticleTypes.CLOUD;
            server.sendParticles(
                    particleType,
                    target.getX(),
                    target.getY() + target.getBbHeight() * 0.4,
                    target.getZ(),
                    6,
                    0.2,
                    0.1,
                    0.2,
                    0.01
            );
        }
        if (sound != null) {
            level.playSound(
                    null,
                    target.getX(),
                    target.getY(),
                    target.getZ(),
                    sound,
                    target.getSoundSource(),
                    0.8f,
                    0.9f + random.nextFloat() * 0.2f
            );
        }
        return true;
    }

    /**
     * Applies a randomised attack offset by rotating the attacker's view. The magnitude is scaled by
     * {@code intensity} so callers can tie the effect to a "drunkness" factor.
     */
    public static void applyRandomAttackOffset(LivingEntity attacker, float yawRangeDeg, float pitchRangeDeg, double intensity) {
        if (attacker == null || intensity <= 0.0) {
            return;
        }
        RandomSource random = attacker.getRandom();
        float scale = (float) Mth.clamp(intensity, 0.0, 1.0);
        float yawOffset = (random.nextFloat() * 2.0f - 1.0f) * yawRangeDeg * scale;
        float pitchOffset = (random.nextFloat() * 2.0f - 1.0f) * pitchRangeDeg * scale;
        float newYaw = attacker.getYRot() + yawOffset;
        float newPitch = Mth.clamp(attacker.getXRot() + pitchOffset, -90.0f, 90.0f);
        attacker.setYRot(newYaw);
        attacker.setXRot(newPitch);
        attacker.setYHeadRot(newYaw);
        attacker.setYBodyRot(newYaw);
        attacker.yHeadRotO = newYaw;
        attacker.yBodyRotO = newYaw;
        attacker.yRotO = newYaw;
        attacker.xRotO = newPitch;
    }
}
