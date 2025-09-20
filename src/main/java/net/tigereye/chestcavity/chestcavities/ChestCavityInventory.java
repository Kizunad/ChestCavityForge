package net.tigereye.chestcavity.chestcavities;


import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;

import java.util.ArrayList;
import java.util.List;

public class ChestCavityInventory extends SimpleContainer {

    ChestCavityInstance instance;
    boolean test;

    public ChestCavityInstance getInstance() {
        return instance;
    }

    public void setInstance(ChestCavityInstance instance) {
        this.instance = instance;
    }

    private static final int DEFAULT_COLUMNS = 9;
    private static final int DEFAULT_ROWS = 4;

    public ChestCavityInventory() {
        super(DEFAULT_COLUMNS * DEFAULT_ROWS);
    }

    public ChestCavityInventory(int size,ChestCavityInstance instance) {
        super(size);
        this.instance = instance;
    }

    public void readTags(ListTag tags) {
        clearContent();
        HolderLookup.Provider lookup = instance != null && instance.owner != null ? instance.owner.level().registryAccess() : null;
        for(int j = 0; j < tags.size(); ++j) {
            CompoundTag tag = tags.getCompound(j);
            int slot = tag.getByte("Slot") & 255;
            if (slot >= 0 && slot < this.getContainerSize() && lookup != null) {
                this.setItem(slot, ItemStack.parseOptional(lookup, tag));
            }
        }

    }

    public ListTag getTags() {
        ListTag list = new ListTag();

        HolderLookup.Provider lookup = instance != null && instance.owner != null ? instance.owner.level().registryAccess() : null;
        for(int i = 0; i < this.getContainerSize(); ++i) {
            ItemStack itemStack = this.getItem(i);
            if (!itemStack.isEmpty() && lookup != null) {
                CompoundTag tag = new CompoundTag();
                tag.putByte("Slot", (byte)i);
                itemStack.save(lookup, tag);
                list.add(tag);
            }
        }

        return list;
    }

    @Override
    public boolean stillValid(Player player) {

        if(instance == null) {return true;} //this is for if something goes wrong with that first moment before things sync
        if(instance.owner.isDeadOrDying()){return false;}
        return (player.distanceTo(instance.owner) < 8);
    }
}
