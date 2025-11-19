package net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.damage;

import java.util.Objects;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/** Immutable snapshot describing a Soul Beast damage interception. */
public record SoulBeastDamageContext(
    LivingEntity victim, DamageSource source, float incomingDamage) {

  /**
   * Validates the immutable damage snapshot.
   *
   * @param victim Entity receiving the hit.
   * @param source Damage source information.
   * @param incomingDamage Raw damage value before mitigation.
   */
  public SoulBeastDamageContext {
    Objects.requireNonNull(victim, "victim");
    Objects.requireNonNull(source, "source");
  }
}
