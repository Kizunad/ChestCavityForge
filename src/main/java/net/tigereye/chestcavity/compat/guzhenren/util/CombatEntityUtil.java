package net.tigereye.chestcavity.compat.guzhenren.util;

import javax.annotation.Nullable;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/** Shared helpers for determining melee interactions and hostility. */
public final class CombatEntityUtil {

  private CombatEntityUtil() {}

  public static boolean isMeleeHit(@Nullable DamageSource source) {
    if (source == null) {
      return false;
    }

    Entity direct = source.getDirectEntity();
    Entity owner = source.getEntity();

    // 仅当“直接命中体”就是活体施加者本人时，才认为是近战命中，避免 DoT/范围效果误判
    if (!(direct instanceof LivingEntity)) {
      return false;
    }
    if (direct != owner) {
      return false;
    }

    return !source.is(DamageTypeTags.IS_PROJECTILE) && !source.is(DamageTypeTags.IS_EXPLOSION);
  }

  public static boolean areEnemies(@Nullable Entity attacker, @Nullable Entity target) {
    if (!(attacker instanceof LivingEntity livingAttacker)
        || !(target instanceof LivingEntity livingTarget)) {
      return false;
    }
    if (livingAttacker == livingTarget) {
      return false;
    }
    return !livingAttacker.isAlliedTo(livingTarget);
  }
}
