package net.tigereye.chestcavity.compat.guzhenren.util;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nullable;

/**
 * Shared helpers for determining melee interactions and hostility.
 */
public final class CombatEntityUtil {

    private CombatEntityUtil() {
    }

    public static boolean isMeleeHit(@Nullable DamageSource source) {
        if (source == null) {
            return false;
        }
        return !source.is(DamageTypeTags.IS_PROJECTILE) && !source.is(DamageTypeTags.IS_EXPLOSION);
    }

    public static boolean areEnemies(@Nullable Entity attacker, @Nullable Entity target) {
        if (!(attacker instanceof LivingEntity livingAttacker) || !(target instanceof LivingEntity livingTarget)) {
            return false;
        }
        if (livingAttacker == livingTarget) {
            return false;
        }
        return !livingAttacker.isAlliedTo(livingTarget);
    }
}
