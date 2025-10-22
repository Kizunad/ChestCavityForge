package net.tigereye.chestcavity.soulbeast.damage;

import java.util.Objects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * @deprecated 迁移至 {@link
 *     net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.damage.SoulBeastDamageContext}
 */
@Deprecated(forRemoval = true)
public record SoulBeastDamageContext(
    LivingEntity victim, DamageSource source, float incomingDamage) {

  public SoulBeastDamageContext {
    Objects.requireNonNull(victim, "victim");
    Objects.requireNonNull(source, "source");
  }
}
