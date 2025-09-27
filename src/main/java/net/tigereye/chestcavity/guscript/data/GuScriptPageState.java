package net.tigereye.chestcavity.guscript.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.ChestCavity;

import java.util.EnumMap;

/**
 * Represents a single notebook page containing items and binding metadata.
 */
public final class GuScriptPageState {

    private final NonNullList<ItemStack> items;
    private BindingTarget bindingTarget;
    private ListenerType listenerType;
    private String title;
    private boolean dirty;
    private int inventorySignature;
    private GuScriptProgramCache compiledProgram;
    private final EnumMap<ListenerType, Long> listenerCooldowns = new EnumMap<>(ListenerType.class);

    public GuScriptPageState(int rows) {
        this(itemsSizeForRows(rows));
    }

    public GuScriptPageState(NonNullList<ItemStack> items) {
        this.items = items;
        this.bindingTarget = BindingTarget.KEYBIND;
        this.listenerType = ListenerType.ON_HIT;
        this.title = "Page";
        this.dirty = true;
        this.inventorySignature = 0;
    }

    private static NonNullList<ItemStack> itemsSizeForRows(int rows) {
        int slotCount = rows * 9 + 1; // binding slot appended at end
        return NonNullList.withSize(slotCount, ItemStack.EMPTY);
    }

    public NonNullList<ItemStack> items() {
        return items;
    }

    public BindingTarget bindingTarget() {
        return bindingTarget;
    }

    public void setBindingTarget(BindingTarget target) {
        if (target == null) {
            target = BindingTarget.KEYBIND;
        }
        if (this.bindingTarget != target) {
            ChestCavity.LOGGER.debug("[GuScript] Page binding target changed: {} -> {}", this.bindingTarget, target);
            this.bindingTarget = target;
            markDirty();
        }
    }

    public ListenerType listenerType() {
        return listenerType;
    }

    public void setListenerType(ListenerType type) {
        if (type == null) {
            type = ListenerType.ON_HIT;
        }
        if (this.listenerType != type) {
            ChestCavity.LOGGER.debug("[GuScript] Page listener type changed: {} -> {}", this.listenerType, type);
            this.listenerType = type;
            markDirty();
        }
    }

    public void markDirty() {
        this.dirty = true;
    }

    public boolean consumeDirtyFlag() {
        boolean current = dirty;
        dirty = false;
        return current;
    }

    public void setTitle(String title) {
        if (title != null && !title.isBlank()) {
            this.title = title;
        }
    }

    public String title() {
        return title;
    }

    public void setCompiledProgram(GuScriptProgramCache cache) {
        this.compiledProgram = cache;
        this.inventorySignature = cache == null ? 0 : cache.inventorySignature();
    }

    public GuScriptProgramCache compiledProgram() {
        return compiledProgram;
    }

    public int inventorySignature() {
        return inventorySignature;
    }

    public void setInventorySignature(int signature) {
        this.inventorySignature = signature;
    }

    public long getLastListenerTrigger(ListenerType type) {
        return listenerCooldowns.getOrDefault(type, Long.MIN_VALUE);
    }

    public void setLastListenerTrigger(ListenerType type, long gameTime) {
        listenerCooldowns.put(type, gameTime);
    }

    public CompoundTag save(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        ContainerHelper.saveAllItems(tag, items, provider);
        tag.putString("BindingTarget", bindingTarget.getSerializedName());
        tag.putString("ListenerType", listenerType.getSerializedName());
        tag.putString("Title", title);
        tag.putInt("InventorySignature", inventorySignature);
        return tag;
    }

    public static GuScriptPageState load(CompoundTag tag, HolderLookup.Provider provider, int expectedRows) {
        NonNullList<ItemStack> items = NonNullList.withSize(expectedRows * 9 + 1, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, provider);
        GuScriptPageState page = new GuScriptPageState(items);
        if (tag.contains("BindingTarget")) {
            page.bindingTarget = BindingTarget.fromSerializedName(tag.getString("BindingTarget"));
        }
        if (tag.contains("ListenerType")) {
            page.listenerType = ListenerType.fromSerializedName(tag.getString("ListenerType"));
        }
        if (tag.contains("Title")) {
            page.title = tag.getString("Title");
        }
        if (tag.contains("InventorySignature")) {
            page.inventorySignature = tag.getInt("InventorySignature");
        }
        page.dirty = true;
        return page;
    }
}
