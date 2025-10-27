package net.tigereye.chestcavity.compat.guzhenren.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;

/** Guzhenren 自定义伤害标签。 */
public final class GRDamageTags {

  public static final TagKey<DamageType> BYPASS_ORGAN_HOOKS =
      TagKey.create(
          Registries.DAMAGE_TYPE,
          ResourceLocation.fromNamespaceAndPath("guzhenren", "bypass_organ_hooks"));

  private GRDamageTags() {}
}
