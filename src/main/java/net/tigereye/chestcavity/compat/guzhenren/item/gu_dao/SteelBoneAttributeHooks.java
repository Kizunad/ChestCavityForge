package net.tigereye.chestcavity.compat.guzhenren.item.gu_dao;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;

/**
 * Registers supplemental attributes required by the steel bone combo (e.g. MAX_ABSORPTION for
 * players).
 */
public final class SteelBoneAttributeHooks {

  private SteelBoneAttributeHooks() {}

  public static void onAttributeModification(EntityAttributeModificationEvent event) {
    event.add(EntityType.PLAYER, Attributes.MAX_ABSORPTION);
  }
}
