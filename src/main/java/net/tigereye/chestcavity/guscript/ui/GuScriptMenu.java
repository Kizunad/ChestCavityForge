package net.tigereye.chestcavity.guscript.ui;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.guscript.data.BindingTarget;
import net.tigereye.chestcavity.guscript.data.GuScriptAttachment;
import net.tigereye.chestcavity.guscript.data.ListenerType;
import net.tigereye.chestcavity.registration.CCAttachments;
import net.tigereye.chestcavity.registration.CCContainers;

public class GuScriptMenu extends AbstractContainerMenu {
    private final Container container;

    public static final int ROWS = 2;
    private static final int PLAYER_INV_Y_BASE = 103;
    private static final int HOTBAR_Y_BASE = 161;
    private static final int SLOT_SIZE = 18;
    private int bindingTargetOrdinal = BindingTarget.KEYBIND.ordinal();
    private int listenerTypeOrdinal = ListenerType.ON_HIT.ordinal();
    private int pageIndex;
    private int pageCount = 1;

    public GuScriptMenu(int syncId, Inventory inventory) {
        this(syncId, inventory, resolveContainer(inventory.player));
    }

    private static Container resolveContainer(Player player) {
        if (player == null) {
            return new SimpleContainer(GuScriptAttachment.TOTAL_SLOTS);
        }
        if (player.level().isClientSide) {
            ChestCavity.LOGGER.debug("[GuScript] Creating client-side stub container");
            return new SimpleContainer(GuScriptAttachment.TOTAL_SLOTS);
        }
        ChestCavity.LOGGER.debug("[GuScript] Fetching server attachment container for {}", player.getGameProfile().getName());
        return CCAttachments.getGuScript(player);
    }

    public GuScriptMenu(int syncId, Inventory inventory, Container container) {
        super(CCContainers.GUSCRIPT_MENU.get(), syncId);
        this.container = container;
        readAttachmentState(container);
        Player owner = inventory.player;
        if (owner != null) {
            container.startOpen(owner);
        }

        int i;
        for (i = 0; i < GuScriptAttachment.ITEM_SLOT_COUNT; i++) {
            addSlot(new Slot(container, i, 8 + i * SLOT_SIZE, 18));
        }

        addSlot(new BindingSlot(container, GuScriptAttachment.BINDING_SLOT_INDEX, 8, 18 + SLOT_SIZE));

        int verticalOffset = (getRows() - 4) * SLOT_SIZE;
        for (i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                addSlot(new Slot(inventory, j + i * 9 + 9, 8 + j * SLOT_SIZE, PLAYER_INV_Y_BASE + i * SLOT_SIZE + verticalOffset));
            }
        }

        for (i = 0; i < 9; ++i) {
            addSlot(new Slot(inventory, i, 8 + i * SLOT_SIZE, HOTBAR_Y_BASE + verticalOffset));
        }

        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return bindingTargetOrdinal;
            }

            @Override
            public void set(int value) {
                bindingTargetOrdinal = value;
            }
        });

        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return listenerTypeOrdinal;
            }

            @Override
            public void set(int value) {
                listenerTypeOrdinal = value;
            }
        });

        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return pageIndex;
            }

            @Override
            public void set(int value) {
                pageIndex = value;
            }
        });

        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return pageCount;
            }

            @Override
            public void set(int value) {
                pageCount = value;
            }
        });
    }

    public int getRows() {
        return ROWS;
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            itemstack = slotStack.copy();
            int containerSlots = GuScriptAttachment.TOTAL_SLOTS;

            if (index < containerSlots) {
                if (!this.moveItemStackTo(slotStack, containerSlots, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(slotStack, 0, containerSlots, false)) {
                return ItemStack.EMPTY;
            }

            if (slotStack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return itemstack;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (player != null) {
            container.stopOpen(player);
        }
    }

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        if (container instanceof GuScriptAttachment attachment && attachment.consumeChangedFlag()) {
            bindingTargetOrdinal = attachment.getBindingTarget().ordinal();
            listenerTypeOrdinal = attachment.getListenerType().ordinal();
            pageIndex = attachment.getCurrentPageIndex();
            pageCount = attachment.getPageCount();
            broadcastChanges();
        }
    }

    private static class BindingSlot extends Slot {
        public BindingSlot(Container container, int index, int xPosition, int yPosition) {
            super(container, index, xPosition, yPosition);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return true;
        }
    }

    private void readAttachmentState(Container container) {
        if (container instanceof GuScriptAttachment attachment) {
            bindingTargetOrdinal = attachment.getBindingTarget().ordinal();
            listenerTypeOrdinal = attachment.getListenerType().ordinal();
            pageIndex = attachment.getCurrentPageIndex();
            pageCount = attachment.getPageCount();
        }
    }

    public BindingTarget getBindingTarget() {
        return BindingTarget.fromOrdinal(bindingTargetOrdinal);
    }

    public ListenerType getListenerType() {
        return ListenerType.fromOrdinal(listenerTypeOrdinal);
    }

    public void syncFromAttachment(GuScriptAttachment attachment) {
        bindingTargetOrdinal = attachment.getBindingTarget().ordinal();
        listenerTypeOrdinal = attachment.getListenerType().ordinal();
        pageIndex = attachment.getCurrentPageIndex();
        pageCount = attachment.getPageCount();
        broadcastChanges();
    }

    public int getPageIndex() {
        return pageIndex;
    }

    public int getPageCount() {
        return pageCount;
    }
}
