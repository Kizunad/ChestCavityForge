package net.tigereye.chestcavity.test;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileWriter;

public class TestContainer extends AbstractContainerMenu {
    private final int size;
    private final int rows;

    private final TestBlockEntity entity;


    public TestContainer(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new ItemStackHandler(27), BlockPos.ZERO);
    }

    public TestContainer(int syncId, Inventory playerInventory, IItemHandler inventory, BlockPos pos) {
        super(null, syncId);//BlockEntityRegistry.CONT.get(), syncId);

        if(pos != BlockPos.ZERO) {
            entity = (TestBlockEntity) playerInventory.player.level.getBlockEntity(pos);
        } else {
            entity = null;
        }

        this.size = inventory.getSlots();
        //this.inventory = inventory;
        this.rows = (size-1)/9 + 1;
        //inventory.startOpen(playerInventory.player);
        int i = (rows - 4) * 18;

        int n;
        int m;
        for(n = 0; n < this.rows; ++n) {
            for(m = 0; m < 9 && (n*9)+m < size; ++m) {
                this.addSlot(new SlotItemHandler(inventory, m + n * 9, 8 + m * 18, 18 + n * 18));//18 + n * 18));
            }
        }

        for(n = 0; n < 9; ++n) {
            this.addSlot(new Slot(playerInventory, n, 8 + n * 18, 160 + i));//161 + i));
        }

        for(n = 0; n < 3; ++n) {
            for(m = 0; m < 9; ++m) {
                this.addSlot(new Slot(playerInventory, m + n * 9 + 9, 8 + m * 18, 102 + n * 18 + i));//103 + n * 18 + i));
            }
        }


    }

    @Override
    public ItemStack quickMoveStack(Player player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);
        if (slot != null && slot.hasItem()) {
            ItemStack originalStack = slot.getItem();
            newStack = originalStack.copy();
            if (invSlot < this.entity.inventory.getSlots()) {
                /*if(inventory.getInstance().type.isSlotForbidden(invSlot)){
                    return ItemStack.EMPTY;
                }*/
                if (!this.moveItemStackTo(originalStack, this.entity.inventory.getSlots(), this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(originalStack, 0, this.entity.inventory.getSlots(), false)) {
                return ItemStack.EMPTY;
            }

            if (originalStack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return newStack;
    }

    @Override
    public boolean stillValid(Player p_38874_) {
        return false;
    }

    public void saveToDesk() {
        if(!this.entity.getLevel().isClientSide) {
            return;
        }
        StringBuilder builder = new StringBuilder();
        builder.append("{\n\"defaultChestCavity\": [");
        for(int count = 0; count <= entity.inventory.getSlots(); count++) {
            builder.append("{\"item\": \"").append(this.entity.inventory.getStackInSlot(count).getItem().getRegistryName().toString()).append("\",\"position\": ").append(count).append("},\n");
        }
        builder.append("\n]\n}");

        File file = new File("/Users/booneldan/Desktop/cavity.txt");

        try {
            file.createNewFile();
            FileWriter writer = new FileWriter(file);
            writer.write(builder.toString());
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
