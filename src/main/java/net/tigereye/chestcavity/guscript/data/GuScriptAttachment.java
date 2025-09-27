package net.tigereye.chestcavity.guscript.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.tigereye.chestcavity.ChestCavity;

import java.util.Objects;

/**
 * Stores the serialized state for GuScript inventory data.
 * Provides a single row of item slots plus an additional binding slot.
 */
public class GuScriptAttachment implements Container {
    public static final int ITEM_SLOT_COUNT = 9;
    public static final int BINDING_SLOT_INDEX = 9;
    public static final int TOTAL_SLOTS = ITEM_SLOT_COUNT + 1;

    private final NonNullList<ItemStack> items = NonNullList.withSize(TOTAL_SLOTS, ItemStack.EMPTY);
    private boolean changed;

    public static GuScriptAttachment create(IAttachmentHolder holder) {
        return new GuScriptAttachment();
    }

    @Override
    public int getContainerSize() {
        return TOTAL_SLOTS;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot >= 0 && slot < items.size() ? items.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ContainerHelper.removeItem(items, slot, amount);
        if (!result.isEmpty()) {
            setChanged();
            ChestCavity.LOGGER.debug("[GuScript] Removed {} items from slot {}", amount, slot);
        }
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack stack = ContainerHelper.takeItem(items, slot);
        if (!stack.isEmpty()) {
            setChanged();
            ChestCavity.LOGGER.debug("[GuScript] Cleared slot {} via removeItemNoUpdate", slot);
        }
        return stack;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot < 0 || slot >= items.size()) {
            return;
        }
        ItemStack previous = items.get(slot);
        items.set(slot, stack);
        if (!stack.isEmpty() && stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
        if (!ItemStack.matches(previous, stack)) {
            ChestCavity.LOGGER.debug("[GuScript] Slot {} updated: {} -> {}", slot, describe(previous), describe(stack));
        }
        setChanged();
    }

    @Override
    public void setChanged() {
        changed = true;
    }

    public boolean consumeChangedFlag() {
        boolean current = changed;
        changed = false;
        return current;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void startOpen(Player player) {
        ChestCavity.LOGGER.info("[GuScript] {} opened GuScript notebook", player.getName().getString());
    }

    @Override
    public void stopOpen(Player player) {
        ChestCavity.LOGGER.info("[GuScript] {} closed GuScript notebook", player.getName().getString());
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < items.size(); i++) {
            items.set(i, ItemStack.EMPTY);
        }
        setChanged();
    }

    public void load(CompoundTag tag, HolderLookup.Provider provider) {
        ContainerHelper.loadAllItems(tag, items, provider);
    }

    public CompoundTag save(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        ContainerHelper.saveAllItems(tag, items, provider);
        return tag;
    }

    private static String describe(ItemStack stack) {
        if (stack.isEmpty()) {
            return "empty";
        }
        return Objects.toString(stack.getItem().builtInRegistryHolder().key().location()) + " x" + stack.getCount();
    }

    public static class Serializer implements IAttachmentSerializer<CompoundTag, GuScriptAttachment> {
        @Override
        public GuScriptAttachment read(IAttachmentHolder holder, CompoundTag tag, HolderLookup.Provider provider) {
            GuScriptAttachment attachment = GuScriptAttachment.create(holder);
            if (tag != null) {
                attachment.load(tag, provider);
            }
            return attachment;
        }

        @Override
        public CompoundTag write(GuScriptAttachment attachment, HolderLookup.Provider provider) {
            return attachment.save(provider);
        }
    }
}
