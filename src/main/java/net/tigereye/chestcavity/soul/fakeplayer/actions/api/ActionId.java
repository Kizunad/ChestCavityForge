package net.tigereye.chestcavity.soul.fakeplayer.actions.api;

import net.minecraft.resources.ResourceLocation;

public record ActionId(ResourceLocation id) {
    @Override
    public String toString() { return id.toString(); }
}

