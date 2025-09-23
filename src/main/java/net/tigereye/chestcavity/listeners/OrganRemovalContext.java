package net.tigereye.chestcavity.listeners;

import net.minecraft.world.item.ItemStack;

/**
 * Stores the association between an organ stack and its removal listener implementation.
 */
public class OrganRemovalContext {
    /** Slot index the organ stack occupied when the listener was registered, or -1 if unknown. */
    public final int slotIndex;
    public final ItemStack organ;
    public final OrganRemovalListener listener;

    public OrganRemovalContext(int slotIndex, ItemStack organ, OrganRemovalListener listener) {
        this.slotIndex = slotIndex;
        this.organ = organ;
        this.listener = listener;
    }

    public OrganRemovalContext(ItemStack organ, OrganRemovalListener listener) {
        this(-1, organ, listener);
    }
}
