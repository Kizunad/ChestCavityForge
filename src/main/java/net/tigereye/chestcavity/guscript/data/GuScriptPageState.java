package net.tigereye.chestcavity.guscript.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.ChestCavity;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

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
    private ResourceLocation flowId;
    private Map<String, String> flowParams;

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
        this.flowParams = new HashMap<>();
    }

    private static NonNullList<ItemStack> itemsSizeForRows(int rows) {
        int slotCount = rows * 9 + 1; // binding slot appended at end
        return NonNullList.withSize(slotCount, ItemStack.EMPTY);
    }

    public NonNullList<ItemStack> items() {
        return items;
    }

    /**
     * Creates a deep copy of this page, including item stacks and metadata but excluding cached compilation artifacts.
     */
    public GuScriptPageState copy() {
        NonNullList<ItemStack> copiedItems = NonNullList.withSize(items.size(), ItemStack.EMPTY);
        for (int i = 0; i < items.size(); i++) {
            copiedItems.set(i, items.get(i).copy());
        }
        GuScriptPageState clone = new GuScriptPageState(copiedItems);
        clone.bindingTarget = bindingTarget;
        clone.listenerType = listenerType;
        clone.title = title;
        clone.dirty = true;
        clone.inventorySignature = inventorySignature;
        clone.listenerCooldowns.putAll(listenerCooldowns);
        clone.flowId = flowId;
        clone.flowParams = flowParams == null ? new HashMap<>() : new HashMap<>(flowParams);
        clone.compiledProgram = null;
        return clone;
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

    public Optional<ResourceLocation> flowId() {
        return Optional.ofNullable(flowId);
    }

    public void setFlowId(ResourceLocation flowId) {
        if (!Objects.equals(this.flowId, flowId)) {
            this.flowId = flowId;
            markDirty();
        }
    }

    public Map<String, String> flowParams() {
        if (flowParams == null || flowParams.isEmpty()) {
            return Map.of();
        }
        return Map.copyOf(flowParams);
    }

    public void setFlowParams(Map<String, String> params) {
        Map<String, String> sanitized = sanitizeParams(params);
        if (!Objects.equals(this.flowParams, sanitized)) {
            this.flowParams = sanitized.isEmpty() ? new HashMap<>() : new HashMap<>(sanitized);
            markDirty();
        }
    }

    public void clearFlowBinding() {
        if (flowId != null || (flowParams != null && !flowParams.isEmpty())) {
            flowId = null;
            flowParams.clear();
            markDirty();
        }
    }

    public CompoundTag save(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        ContainerHelper.saveAllItems(tag, items, provider);
        tag.putString("BindingTarget", bindingTarget.getSerializedName());
        tag.putString("ListenerType", listenerType.getSerializedName());
        tag.putString("Title", title);
        tag.putInt("InventorySignature", inventorySignature);
        if (flowId != null) {
            tag.putString("FlowId", flowId.toString());
        }
        if (flowParams != null && !flowParams.isEmpty()) {
            CompoundTag params = new CompoundTag();
            flowParams.forEach(params::putString);
            tag.put("FlowParams", params);
        }
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
        if (tag.contains("FlowId", Tag.TAG_STRING)) {
            page.flowId = ResourceLocation.tryParse(tag.getString("FlowId"));
        }
        if (tag.contains("FlowParams", Tag.TAG_COMPOUND)) {
            CompoundTag params = tag.getCompound("FlowParams");
            page.flowParams = new HashMap<>();
            for (String key : params.getAllKeys()) {
                String value = params.getString(key);
                page.flowParams.put(key, value);
            }
        } else {
            page.flowParams = new HashMap<>();
        }
        page.dirty = true;
        return page;
    }

    private static Map<String, String> sanitizeParams(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return Map.of();
        }
        Map<String, String> sanitized = new HashMap<>();
        params.forEach((key, value) -> {
            if (key != null && !key.isBlank() && value != null) {
                sanitized.put(key, value);
            }
        });
        return sanitized;
    }
}
