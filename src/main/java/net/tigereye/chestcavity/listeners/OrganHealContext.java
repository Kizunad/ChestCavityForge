package net.tigereye.chestcavity.listeners;

import net.minecraft.world.item.ItemStack;

public class OrganHealContext {
    public final ItemStack organ;
    public final OrganHealListener listener;

    public OrganHealContext(ItemStack organ, OrganHealListener listener) {
        this.organ = organ;
        this.listener = listener;
    }
}

