package net.tigereye.chestcavity.client.modernui.container;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.registration.CCContainers;

/**
 * Debug container menu used to validate Modern UI + NeoForge inventory bridging.
 * Provides a simple 3x3 storage grid backed by a {@link SimpleContainer}, plus
 * the player's inventory slots.
 */
public class TestModernUIContainerMenu extends AbstractContainerMenu {

    public static final int STORAGE_COLUMNS = 3;
    public static final int STORAGE_ROWS = 3;
    public static final int STORAGE_SLOT_COUNT = STORAGE_COLUMNS * STORAGE_ROWS;

    private final Container container;

    public TestModernUIContainerMenu(int syncId, Inventory inventory) {
        this(syncId, inventory, createSeededContainer());
    }

    public TestModernUIContainerMenu(int syncId, Inventory inventory, Container container) {
        super(CCContainers.TEST_MODERN_UI_MENU.get(), syncId);
        this.container = container;

        container.startOpen(inventory.player);

        // Storage slots (3x3)
        for (int row = 0; row < STORAGE_ROWS; row++) {
            for (int col = 0; col < STORAGE_COLUMNS; col++) {
                int slotIndex = col + row * STORAGE_COLUMNS;
                int x = 8 + col * 18;
                int y = 18 + row * 18;
                addSlot(new Slot(container, slotIndex, x, y));
            }
        }

        // Player inventory (3 rows x 9)
        int playerInventoryStartY = 18 + STORAGE_ROWS * 18 + 12;
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                int index = col + row * 9 + 9;
                int x = 8 + col * 18;
                int y = playerInventoryStartY + row * 18;
                addSlot(new Slot(inventory, index, x, y));
            }
        }

        // Hotbar
        int hotbarY = playerInventoryStartY + 58;
        for (int col = 0; col < 9; ++col) {
            int x = 8 + col * 18;
            addSlot(new Slot(inventory, col, x, hotbarY));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack original = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            original = slotStack.copy();

            int storageEnd = STORAGE_SLOT_COUNT;
            int totalSlots = slots.size();

            if (index < storageEnd) {
                if (!moveItemStackTo(slotStack, storageEnd, totalSlots, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(slotStack, 0, storageEnd, false)) {
                return ItemStack.EMPTY;
            }

            if (slotStack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return original;
    }

    @Override
    public void clicked(int slotId, int dragType, ClickType clickType, Player player) {
        super.clicked(slotId, dragType, clickType, player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        container.stopOpen(player);
    }

    public static SimpleContainer createSeededContainer() {
        var container = new SimpleContainer(STORAGE_SLOT_COUNT);
        TestModernUIContainerDebug.seedDefaultItems(container);
        return container;
    }
}
