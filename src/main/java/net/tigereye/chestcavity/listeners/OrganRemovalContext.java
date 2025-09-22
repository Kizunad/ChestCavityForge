package net.tigereye.chestcavity.listeners;

import net.minecraft.world.item.ItemStack;

/**
 * Stores the association between an organ stack and its removal listener implementation.
 */
public class OrganRemovalContext {
    public final ItemStack organ;
    public final OrganRemovalListener listener;

    public OrganRemovalContext(ItemStack organ, OrganRemovalListener listener) {
        this.organ = organ;
        this.listener = listener;
    }
}
