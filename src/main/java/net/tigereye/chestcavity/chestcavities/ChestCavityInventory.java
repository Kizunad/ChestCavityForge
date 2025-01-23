package net.tigereye.chestcavity.chestcavities;


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

    public ChestCavityInventory() {
        super(27);
    }

    public ChestCavityInventory(int size,ChestCavityInstance instance) {
        super(size);
        this.instance = instance;
    }

    public void readTags(ListTag tags) {
        clearContent();
        for(int j = 0; j < tags.size(); ++j) {
            CompoundTag NbtCompound = tags.getCompound(j);
            int k = NbtCompound.getByte("Slot") & 255;
            boolean f = NbtCompound.getBoolean("Forbidden");
            if (k >= 0 && k < this.getContainerSize()) {
                this.setItem(k, ItemStack.of(NbtCompound));
            }
        }

    }

    public ListTag getTags() {
        ListTag list = new ListTag();

        for(int i = 0; i < this.getContainerSize(); ++i) {
            ItemStack itemStack = this.getItem(i);
            if (!itemStack.isEmpty()) {
                CompoundTag NbtCompound = new CompoundTag();
                NbtCompound.putByte("Slot", (byte)i);
                itemStack.save(NbtCompound);
                list.add(NbtCompound);
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
