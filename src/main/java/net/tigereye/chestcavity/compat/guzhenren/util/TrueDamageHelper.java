package net.tigereye.chestcavity.compat.guzhenren.util;

import net.minecraft.world.entity.LivingEntity;

/**
 * Simple wrapper around the vanilla damage call to emulate "true" damage (ignores invulnerability frames).
 */
public final class TrueDamageHelper {

    private TrueDamageHelper() {
    }

    public static void apply(LivingEntity target, float amount) {
        if (target == null || amount <= 0.0f) {
            return;
        }
        int previousHurtTime = target.hurtTime;
        target.invulnerableTime = 0;
        target.hurtTime = 0;
        target.hurt(target.damageSources().generic(), amount);
        target.hurtTime = previousHurtTime;
    }
}
