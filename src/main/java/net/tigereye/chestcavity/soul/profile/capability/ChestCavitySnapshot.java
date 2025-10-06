package net.tigereye.chestcavity.soul.profile.capability;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.registration.CCAttachments;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public final class ChestCavitySnapshot implements CapabilitySnapshot {

    public static final ResourceLocation ID = ChestCavity.id("chest_cavity");
    private static final ResourceLocation DEFAULT_RESOURCE = ChestCavity.id("types/humanoids/human");

    private static final CompoundTag DEFAULT_DATA = loadDefaultData();

    private CompoundTag chestCavityData;
    private boolean dirty;

    public ChestCavitySnapshot() {
        this(DEFAULT_DATA.copy(), false);
    }

    private ChestCavitySnapshot(CompoundTag data, boolean dirty) {
        this.chestCavityData = data;
        this.dirty = dirty;
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public CapabilitySnapshot capture(ServerPlayer player) {
        ChestCavityInstance instance = CCAttachments.getChestCavity(player);
        CompoundTag wrapper = new CompoundTag();
        instance.toTag(wrapper, player.registryAccess());
        this.chestCavityData = wrapper.getCompound("ChestCavity").copy();
        this.dirty = true;
        return this;
    }

    @Override
    public void apply(ServerPlayer player) {
        ChestCavityInstance instance = CCAttachments.getChestCavity(player);
        CompoundTag data = chestCavityData == null ? DEFAULT_DATA.copy() : chestCavityData.copy();
        CompoundTag wrapper = new CompoundTag();
        wrapper.put("ChestCavity", data);
        instance.fromTag(wrapper, player, player.registryAccess());
    }

    @Override
    public CompoundTag save(HolderLookup.Provider provider) {
        CompoundTag out = new CompoundTag();
        if (chestCavityData != null && !chestCavityData.isEmpty()) {
            out.put("ChestCavity", chestCavityData.copy());
        }
        return out;
    }

    @Override
    public CapabilitySnapshot load(CompoundTag tag, HolderLookup.Provider provider) {
        if (tag.contains("ChestCavity")) {
            this.chestCavityData = tag.getCompound("ChestCavity").copy();
        } else {
            this.chestCavityData = DEFAULT_DATA.copy();
        }
        this.dirty = false;
        return this;
    }

    @Override
    public boolean isDirty() {
        return dirty;
    }

    @Override
    public void clearDirty() {
        this.dirty = false;
    }

    private static CompoundTag loadDefaultData() {
        try (InputStream stream = ChestCavitySnapshot.class.getResourceAsStream("/data/" + DEFAULT_RESOURCE.getNamespace() + "/" + DEFAULT_RESOURCE.getPath() + ".json")) {
            if (stream == null) {
                ChestCavity.LOGGER.warn("[soul] Missing default chest cavity resource {}", DEFAULT_RESOURCE);
                return createEmptyChestCavity();
            }
            JsonObject root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonArray defaults = root.getAsJsonArray("defaultChestCavity");
            ListTag inventory = new ListTag();
            if (defaults != null) {
                for (JsonElement element : defaults) {
                    if (!element.isJsonObject()) {
                        continue;
                    }
                    JsonObject entry = element.getAsJsonObject();
                    if (!entry.has("item") || !entry.has("position")) {
                        continue;
                    }
                    ResourceLocation itemId;
                    try {
                        itemId = ResourceLocation.parse(entry.get("item").getAsString());
                    } catch (IllegalArgumentException ignored) {
                        ChestCavity.LOGGER.warn("[soul] Invalid item id '{}' in default chest cavity", entry.get("item").getAsString());
                        continue;
                    }
                    Optional<Item> itemOptional = BuiltInRegistries.ITEM.getOptional(itemId);
                    if (itemOptional.isEmpty()) {
                        ChestCavity.LOGGER.warn("[soul] Unknown item {} in default chest cavity", itemId);
                        continue;
                    }
                    Item item = itemOptional.get();
                    int position = entry.get("position").getAsInt();
                    int count = entry.has("count") ? entry.get("count").getAsInt() : item.getDefaultMaxStackSize();
                    ItemStack stack = new ItemStack(item, count);
                    if (stack.isEmpty()) {
                        continue;
                    }
                    CompoundTag stackTag = new CompoundTag();
                    stackTag.putByte("Slot", (byte) position);
                    stackTag.putString("id", BuiltInRegistries.ITEM.getKey(item).toString());
                    stackTag.putByte("Count", (byte) stack.getCount());
                    inventory.add(stackTag);
                }
            }
            CompoundTag chestCavity = createEmptyChestCavity();
            chestCavity.put("Inventory", inventory);
            return chestCavity;
        } catch (Exception e) {
            ChestCavity.LOGGER.error("[soul] Failed to load default chest cavity data", e);
            return createEmptyChestCavity();
        }
    }

    private static CompoundTag createEmptyChestCavity() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("opened", false);
        tag.putInt("HeartTimer", 0);
        tag.putInt("KidneyTimer", 0);
        tag.putInt("LiverTimer", 0);
        tag.putFloat("MetabolismRemainder", 0);
        tag.putFloat("LungRemainder", 0);
        tag.putInt("FurnaceProgress", 0);
        tag.putInt("PhotosynthesisProgress", 0);
        tag.put("Inventory", new ListTag());
        return tag;
    }
}
