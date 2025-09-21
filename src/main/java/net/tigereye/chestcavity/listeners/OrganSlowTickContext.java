package net.tigereye.chestcavity.listeners;

import net.minecraft.world.item.ItemStack;

public class OrganSlowTickContext {
    public final ItemStack organ;
    public final OrganSlowTickListener listener;

    public OrganSlowTickContext(ItemStack organ, OrganSlowTickListener listener) {
        this.organ = organ;
        this.listener = listener;
    }
}

