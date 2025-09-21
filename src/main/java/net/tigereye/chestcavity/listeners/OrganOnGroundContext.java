package net.tigereye.chestcavity.listeners;

import net.minecraft.world.item.ItemStack;

public class OrganOnGroundContext {
    public final ItemStack organ;
    public final OrganOnGroundListener listener;

    public OrganOnGroundContext(ItemStack organ, OrganOnGroundListener listener) {
        this.organ = organ;
        this.listener = listener;
    }
}

