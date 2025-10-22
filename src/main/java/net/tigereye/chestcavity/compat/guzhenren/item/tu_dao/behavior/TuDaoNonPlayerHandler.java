package net.tigereye.chestcavity.compat.guzhenren.item.tu_dao.behavior;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * Utility for emulating Guzhenren resource costs on entities that lack zhenyuan/jingli pools (e.g.
 * mobs controlled through Chest Cavity).
 *
 * <p>The Guzhenren mod normally deducts both zhenyuan and jingli when certain Tu Dao organs
 * trigger. When a non-player entity uses these behaviours the compat layer replaces that
 * consumption with a direct health payment at a fixed ratio of {@code 1 health : 100 (zhenyuan +
 * jingli)}.
 */
public final class TuDaoNonPlayerHandler {

  private static final double RESOURCE_TO_HEALTH_RATIO = 100.0;
  private static final float EPSILON = 1.0E-4f;

  private TuDaoNonPlayerHandler() {}

  /**
   * Attempts to deduct an equivalent amount of health from the given entity for the provided
   * resource costs. Health is drained after absorption and will never outright kill the entity;
   * failure is reported via {@code false}.
   *
   * @param entity the living entity paying the cost
   * @param zhenyuanCost zhenyuan that would normally be consumed
   * @param jingliCost jingli that would normally be consumed
   * @return {@code true} if enough health (or absorption) was available and the deduction
   *     succeeded, {@code false} otherwise
   */
  public static boolean handleNonPlayer(
      LivingEntity entity, double zhenyuanCost, double jingliCost) {
    if (entity == null || !entity.isAlive()) {
      return false;
    }

    double totalResource = Math.max(0.0, zhenyuanCost) + Math.max(0.0, jingliCost);
    if (!Double.isFinite(totalResource) || totalResource <= 0.0) {
      return true;
    }

    float healthCost = (float) (totalResource / RESOURCE_TO_HEALTH_RATIO);
    if (!Float.isFinite(healthCost) || healthCost <= 0.0f) {
      return false;
    }

    float absorption = Math.max(0.0f, entity.getAbsorptionAmount());
    float remaining = healthCost;
    if (absorption > EPSILON) {
      float absorbed = Math.min(absorption, remaining);
      entity.setAbsorptionAmount(absorption - absorbed);
      remaining -= absorbed;
    }

    if (remaining <= EPSILON) {
      return true;
    }

    float currentHealth = entity.getHealth();
    float available = currentHealth + Math.max(0.0f, entity.getAbsorptionAmount());
    if (available - remaining <= EPSILON) {
      return false;
    }

    float targetHealth = Math.max(0.0f, currentHealth - remaining);

    entity.invulnerableTime = 0;
    DamageSource damageSource = entity.damageSources().generic();
    entity.hurt(damageSource, remaining);
    entity.invulnerableTime = 0;

    if (!entity.isDeadOrDying()) {
      if (entity.getHealth() > targetHealth) {
        entity.setHealth(targetHealth);
      }
      entity.hurtTime = 0;
      entity.hurtDuration = 0;
    }

    return !entity.isDeadOrDying();
  }
}
