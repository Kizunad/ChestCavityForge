package net.tigereye.chestcavity.compat.common.passive;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;

public interface PassiveHook {
  /**
   * Called every tick for entities with this passive ability.
   *
   * @param owner The entity that has this passive ability.
   * @param cc The chest cavity of the entity.
   * @param now The current game time.
   */
  default void onTick(LivingEntity owner, ChestCavityInstance cc, long now) {}

  /**
   * Called when an entity with this passive ability is hurt.
   *
   * @param self The entity that has this passive ability.
   * @param source The damage source.
   * @param amount The amount of damage.
   * @param cc The chest cavity of the entity.
   * @param now The current game time.
   */
  default void onHurt(
      LivingEntity self, DamageSource source, float amount, ChestCavityInstance cc, long now) {}

  /**
   * Called when an entity with this passive ability hits another entity in melee.
   *
   * @param attacker The entity that has this passive ability.
   * @param target The entity being attacked.
   * @param cc The chest cavity of the attacker.
   * @param now The current game time.
   */
  default void onHitMelee(
      LivingEntity attacker, LivingEntity target, ChestCavityInstance cc, long now) {}
}
