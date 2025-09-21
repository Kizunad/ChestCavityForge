package net.tigereye.chestcavity.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * Helper methods for reading/writing an integer "Charge" value inside {@link CustomData} payloads.
 */
public final class NBTCharge {

    /** Standardised key used to store integer charge values. */
    public static final String CHARGE_KEY = "Charge";

    private NBTCharge() {
    }

    /** Retrieves the charge value from the given tag (defaults to 0 when absent). */
    public static int getCharge(CompoundTag tag) {
        return tag.getInt(CHARGE_KEY);
    }

    /** Writes the given charge value into the supplied tag. */
    public static void setCharge(CompoundTag tag, int value) {
        tag.putInt(CHARGE_KEY, value);
    }

    /**
     * Reads the charge value from a stack's custom data within the provided sub-compound.
     *
     * @param stack    stack holding {@link CustomData}
     * @param stateKey sub-compound name that contains the charge entry
     * @return stored charge or 0 when absent
     */
    public static int getCharge(ItemStack stack, String stateKey) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return 0;
        }
        CompoundTag root = data.copyTag();
        if (!root.contains(stateKey, Tag.TAG_COMPOUND)) {
            return 0;
        }
        return getCharge(root.getCompound(stateKey));
    }

    /**
     * Writes the charge value into a stack's custom data within the provided sub-compound.
     *
     * @param stack    stack to update
     * @param stateKey sub-compound name that will own the charge entry
     * @param value    charge amount to persist
     */
    public static void setCharge(ItemStack stack, String stateKey, int value) {
        int clamped = Math.max(0, value);
        NBTWriter.updateCustomData(stack, tag -> {
            CompoundTag state = tag.contains(stateKey, Tag.TAG_COMPOUND) ? tag.getCompound(stateKey) : new CompoundTag();
            setCharge(state, clamped);
            tag.put(stateKey, state);
        });
    }
}
