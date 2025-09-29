package net.tigereye.chestcavity.guscript.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.tigereye.chestcavity.ChestCavity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Stores the serialized state for GuScript notebook pages and exposes the active page as a Container.
 */
public class GuScriptAttachment implements Container {
    public static final int ITEM_SLOT_COUNT = 18; // 2 rows of 9
    public static final int BINDING_SLOT_INDEX = 18; // binding slot after items
    public static final int TOTAL_SLOTS = ITEM_SLOT_COUNT + 1; // +1 binding slot
    private static final int DEFAULT_ROWS = 2;

    private final List<GuScriptPageState> pages = new ArrayList<>();
    private int currentPage;
    private boolean changed;
    private final int rows;

    public static GuScriptAttachment create(IAttachmentHolder holder) {
        return new GuScriptAttachment(DEFAULT_ROWS);
    }

    public GuScriptAttachment(int rows) {
        this.rows = rows;
        this.currentPage = 0;
        ensurePageExists(0);
    }

    private void ensurePageExists(int index) {
        while (pages.size() <= index) {
            pages.add(new GuScriptPageState(rows));
        }
    }

    /**
     * Visits every slot across every page in the attachment.
     *
     * @param visitor callback that receives the page index, slot index, and live {@link ItemStack} for that slot
     */
    public void visitSlots(GuScriptSlotVisitor visitor) {
        Objects.requireNonNull(visitor, "visitor");
        for (int pageIndex = 0; pageIndex < pages.size(); pageIndex++) {
            GuScriptPageState page = pages.get(pageIndex);
            List<ItemStack> items = page.items();
            for (int slot = 0; slot < items.size(); slot++) {
                visitor.visit(pageIndex, slot, items.get(slot));
            }
        }
    }

    /**
     * Removes and returns every {@link ItemStack} stored across all pages.
     * <p>
     * The returned list contains defensive copies so callers may safely mutate the stacks when spawning drops.
     * Pages are marked dirty and compilation caches are invalidated when any slot is cleared.
     *
     * @return ordered list of drained item stacks; empty when the attachment held no items
     */
    public List<ItemStack> drainAllItems() {
        List<ItemStack> drained = new ArrayList<>();
        for (GuScriptPageState page : pages) {
            List<ItemStack> items = page.items();
            boolean pageChanged = false;
            for (int slot = 0; slot < items.size(); slot++) {
                ItemStack stack = items.get(slot);
                if (stack.isEmpty()) {
                    continue;
                }
                drained.add(stack.copy());
                items.set(slot, ItemStack.EMPTY);
                pageChanged = true;
            }
            if (pageChanged) {
                page.setCompiledProgram(null);
                page.setInventorySignature(0);
                page.markDirty();
            }
        }
        if (!drained.isEmpty()) {
            setChanged();
        }
        return drained;
    }

    public GuScriptPageState activePage() {
        ensurePageExists(currentPage);
        return pages.get(currentPage);
    }

    public List<GuScriptPageState> pages() {
        return pages;
    }

    public void copyFrom(GuScriptAttachment source) {
        Objects.requireNonNull(source, "source");
        pages.clear();
        for (GuScriptPageState page : source.pages) {
            pages.add(page.copy());
        }
        if (pages.isEmpty()) {
            pages.add(new GuScriptPageState(rows));
        }
        currentPage = Math.max(0, Math.min(source.currentPage, pages.size() - 1));
        changed = true;
    }

    public int getConfiguredRows() {
        return rows;
    }

    public int getPageCount() {
        return pages.size();
    }

    public int getCurrentPageIndex() {
        return currentPage;
    }

    public void setCurrentPage(int index) {
        if (index < 0) {
            index = 0;
        }
        if (index >= pages.size()) {
            index = pages.size() - 1;
        }
        if (index != currentPage) {
            currentPage = index;
            setChanged();
        }
    }

    public GuScriptPageState addPage() {
        GuScriptPageState page = new GuScriptPageState(rows);
        page.setTitle("Page " + (pages.size() + 1));
        pages.add(page);
        setChanged();
        return page;
    }

    @Override
    public int getContainerSize() {
        return TOTAL_SLOTS;
    }

    @Override
    public boolean isEmpty() {
        return activePage().items().stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        if (slot < 0 || slot >= TOTAL_SLOTS) {
            return ItemStack.EMPTY;
        }
        return activePage().items().get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ContainerHelper.removeItem(activePage().items(), slot, amount);
        if (!result.isEmpty()) {
            activePage().markDirty();
            setChanged();
            ChestCavity.LOGGER.debug("[GuScript] Removed {} items from slot {}", amount, slot);
        }
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack stack = ContainerHelper.takeItem(activePage().items(), slot);
        if (!stack.isEmpty()) {
            activePage().markDirty();
            setChanged();
            ChestCavity.LOGGER.debug("[GuScript] Cleared slot {} via removeItemNoUpdate", slot);
        }
        return stack;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot < 0 || slot >= TOTAL_SLOTS) {
            return;
        }
        ItemStack previous = activePage().items().get(slot);
        activePage().items().set(slot, stack);
        if (!stack.isEmpty() && stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
        if (!ItemStack.matches(previous, stack)) {
            ChestCavity.LOGGER.debug("[GuScript] Slot {} updated: {} -> {}", slot, describe(previous), describe(stack));
            activePage().markDirty();
            setChanged();
        }
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
        activePage().items().replaceAll(stack -> ItemStack.EMPTY);
        activePage().markDirty();
        setChanged();
    }

    public BindingTarget getBindingTarget() {
        return activePage().bindingTarget();
    }

    public ListenerType getListenerType() {
        return activePage().listenerType();
    }

    public void setBindingTarget(BindingTarget target) {
        activePage().setBindingTarget(target);
        setChanged();
    }

    public void cycleBindingTarget() {
        setBindingTarget(getBindingTarget().next());
    }

    public void setListenerType(ListenerType type) {
        activePage().setListenerType(type);
        setChanged();
    }

    public void cycleListenerType() {
        setListenerType(getListenerType().next());
    }

    public boolean consumePageDirtyFlag() {
        return activePage().consumeDirtyFlag();
    }

    public int currentInventorySignature() {
        return activePage().inventorySignature();
    }

    public void updateInventorySignature(int signature) {
        activePage().setInventorySignature(signature);
    }

    public GuScriptProgramCache getCompiledProgram() {
        return activePage().compiledProgram();
    }

    public void setCompiledProgram(GuScriptProgramCache cache) {
        activePage().setCompiledProgram(cache);
    }

    public long getLastListenerTrigger(ListenerType type) {
        return activePage().getLastListenerTrigger(type);
    }

    public void setLastListenerTrigger(ListenerType type, long gameTime) {
        activePage().setLastListenerTrigger(type, gameTime);
    }

    public void load(CompoundTag tag, HolderLookup.Provider provider) {
        pages.clear();
        int rows = DEFAULT_ROWS;
        if (tag != null && tag.contains("Rows")) {
            int saved = tag.getInt("Rows");
            // Upgrade older saves to at least DEFAULT_ROWS so container size matches page items
            rows = Math.max(DEFAULT_ROWS, saved);
        }
        int current = tag.contains("CurrentPage") ? tag.getInt("CurrentPage") : 0;
        ListTag list = tag.getList("Pages", Tag.TAG_COMPOUND);
        if (list.isEmpty()) {
            pages.add(new GuScriptPageState(rows));
        } else {
            for (Tag element : list) {
                CompoundTag pageTag = (CompoundTag) element;
                pages.add(GuScriptPageState.load(pageTag, provider, rows));
            }
        }
        this.currentPage = Math.max(0, Math.min(current, pages.size() - 1));
        this.changed = true;
    }

    public CompoundTag save(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (GuScriptPageState page : pages) {
            list.add(page.save(provider));
        }
        tag.put("Pages", list);
        tag.putInt("CurrentPage", currentPage);
        tag.putInt("Rows", rows);
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
