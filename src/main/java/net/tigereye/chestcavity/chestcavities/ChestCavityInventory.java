package net.tigereye.chestcavity.chestcavities;


import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
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
        readTags(tags, null);
    }

    public void readTags(ListTag tags, HolderLookup.Provider lookup) {
        clearContent();
        HolderLookup.Provider effectiveLookup = lookup;
        if (effectiveLookup == null && instance != null && instance.owner != null) {
            effectiveLookup = instance.owner.level().registryAccess();
        }
        if (effectiveLookup == null) {
            return;
        }
        for (int j = 0; j < tags.size(); ++j) {
            CompoundTag tag = tags.getCompound(j);
            int slot = tag.getByte("Slot") & 255;
            if (slot >= 0 && slot < this.getContainerSize()) {
                this.setItem(slot, ItemStack.parseOptional(effectiveLookup, tag));
            }
        }
    }

    public ListTag getTags() {
        return getTags(null);
    }

    public ListTag getTags(HolderLookup.Provider lookup) {
        ListTag list = new ListTag();

        HolderLookup.Provider effectiveLookup = lookup;
        if (effectiveLookup == null && instance != null && instance.owner != null) {
            effectiveLookup = instance.owner.level().registryAccess();
        }
        if (effectiveLookup == null) {
            return list;
        }
        for (int i = 0; i < this.getContainerSize(); ++i) {
            ItemStack itemStack = this.getItem(i);
            if (!itemStack.isEmpty()) {
                Tag raw = itemStack.save(effectiveLookup);
                if (raw instanceof CompoundTag tag && !tag.isEmpty()) {
                    tag.putByte("Slot", (byte)i);
                    list.add(tag);
                }
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
