package net.tigereye.chestcavity.compat.guzhenren.registry;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.Level;

/** Guzhenren 特定伤害来源构造工具。 */
public final class GRDamageSources {

  private GRDamageSources() {}

  public static DamageSource organInternal(Level level) {
    Holder<DamageType> type =
        level.registryAccess()
            .lookupOrThrow(Registries.DAMAGE_TYPE)
            .getOrThrow(GRDamageTypes.ORGAN_INTERNAL);
    return new DamageSource(type);
  }
}
