package net.tigereye.chestcavity.compat.guzhenren.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;

/** Guzhenren 自定义伤害类型。 */
public final class GRDamageTypes {

  public static final ResourceKey<DamageType> ORGAN_INTERNAL =
      ResourceKey.create(
          Registries.DAMAGE_TYPE,
          ResourceLocation.fromNamespaceAndPath("guzhenren", "organ_internal"));

  private GRDamageTypes() {}
}
