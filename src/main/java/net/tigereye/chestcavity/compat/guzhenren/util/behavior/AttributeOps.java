package net.tigereye.chestcavity.compat.guzhenren.util.behavior;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/** Common helpers for applying and removing transient attribute modifiers. */
public final class AttributeOps {

  private AttributeOps() {}

  /**
   * Removes any existing modifier with the given id and adds the provided modifier as transient.
   */
  public static void replaceTransient(
      AttributeInstance attribute, ResourceLocation id, AttributeModifier modifier) {
    if (attribute == null || id == null || modifier == null) {
      return;
    }
    attribute.removeModifier(id);
    attribute.addTransientModifier(modifier);
  }

  /** Removes a modifier by its id if present. */
  public static void removeById(AttributeInstance attribute, ResourceLocation id) {
    if (attribute == null || id == null) {
      return;
    }
    attribute.removeModifier(id);
  }
}
