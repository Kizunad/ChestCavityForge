package net.tigereye.chestcavity.soul.fakeplayer.actions.registry;

import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.soul.fakeplayer.actions.api.Action;

/** Factory for dynamic Action instances derived from an id. */
public interface ActionFactory {
  boolean supports(ResourceLocation id);

  Action create(ResourceLocation id);
}
