package net.tigereye.chestcavity.test;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuConstructor;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

import java.io.File;
import java.io.FileWriter;

public class ExampleChestContainer extends AbstractContainerMenu {
    //private final Inventory playerInv;
    //private final BlockPos pos;
    private final ExampleChestBlockEntity entity;
    private final ContainerLevelAccess containerAccess;

    // Client Constructor
    public ExampleChestContainer(int id, Inventory playerInv) {
        this(id, playerInv, new ItemStackHandler(27), BlockPos.ZERO);
    }

    // Server constructor
    public ExampleChestContainer(int id, Inventory playerInv, IItemHandler slots, BlockPos pos) {
        super(BlockEntityRegistry.EXAMPLE_CHEST_MENU.get(), id);
        //this.playerInv = playerInv;
        //this.pos = pos;
        this.entity = (ExampleChestBlockEntity) playerInv.player.level.getBlockEntity(pos);
        this.containerAccess = ContainerLevelAccess.create(playerInv.player.level, pos);
        
        final int slotSizePlus2 = 18, startX = 8, startY = 86, hotbarY = 144, inventoryY = 18;
        
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new SlotItemHandler(slots, row * 9 + column, startX + column * slotSizePlus2,
                        inventoryY + row * slotSizePlus2));
            }
        }
        
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInv, 9 + row * 9 + column, startX + column * slotSizePlus2,
                        startY + row * slotSizePlus2));
            }
        }

        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInv, column, startX + column * slotSizePlus2, hotbarY));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        var retStack = ItemStack.EMPTY;
        final Slot slot = getSlot(index);
        if (slot.hasItem()) {
            final ItemStack item = slot.getItem();
            retStack = item.copy();
            if (index < 27) {
                if (!moveItemStackTo(item, 27, this.slots.size(), true))
                    return ItemStack.EMPTY;
            } else if (!moveItemStackTo(item, 0, 27, false))
                return ItemStack.EMPTY;

            if (item.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return retStack;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.containerAccess, player, BlockEntityRegistry.EXAMPLE_CHEST_BLOCK.get());
    }
    
    public static MenuConstructor getServerContainer(ExampleChestBlockEntity chest, BlockPos pos) {
        return (id, playerInv, player) -> new ExampleChestContainer(id, playerInv, chest.inventory, pos);
    }


    public void saveToDesk() {
        System.out.println("Creating data!");
        StringBuilder builder = new StringBuilder();
        builder.append("{\n\"defaultChestCavity\": [\n");
        boolean addedFirst = true;
        for(int count = 0; count < 27; count++) {
            ItemStack stack = this.slots.get(count).getItem();
            if(!stack.getItem().getRegistryName().toString().equals("minecraft:air")) {
                if(addedFirst) {
                    addedFirst = false;
                } else {
                    builder.append(",\n");
                }
                builder.append("{\"item\": \"").append(stack.getItem().getRegistryName().toString()).append("\", \"position\": ").append(count).append("}");
            }
        }
        builder.append("\n]\n}");

        System.out.println("Finished, now saving data!");
        System.out.println("if saving failed, here:");
        System.out.println(builder);
        File file = new File("/Users/booneldan/Desktop/cavity.txt");

        try {
            file.createNewFile();
            FileWriter writer = new FileWriter(file);
            writer.write(builder.toString());
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("Finished saving data!");
    }
}