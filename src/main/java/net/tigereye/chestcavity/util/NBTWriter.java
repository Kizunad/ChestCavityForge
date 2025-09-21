package net.tigereye.chestcavity.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.function.Consumer;

/**
 * Utility helpers for writing custom NBT data onto item stacks.
 */
public final class NBTWriter {

    private NBTWriter() {
    }

    public static void updateCustomData(ItemStack stack, Consumer<CompoundTag> modifier) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, modifier);
    }
}
