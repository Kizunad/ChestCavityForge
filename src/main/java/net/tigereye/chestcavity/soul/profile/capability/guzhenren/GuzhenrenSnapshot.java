package net.tigereye.chestcavity.soul.profile.capability.guzhenren;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.soul.profile.capability.CapabilitySnapshot;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;

/**
 * Snapshot implementation for Guzhenren player variables.
 * <p>
 * This snapshot is intentionally conservative: it only mirrors the numeric fields exposed by
 * {@link GuzhenrenResourceBridge}. Non-numeric payloads (e.g. ItemStack based ShaZhao slots)
 * are ignored for now because they require additional inventory style handling to avoid duping.
 */
public final class GuzhenrenSnapshot implements CapabilitySnapshot {

    public static final ResourceLocation ID = ChestCavity.id("guzhenren");

    private final Map<String, Double> cachedValues;
    private boolean dirty;

    public GuzhenrenSnapshot() {
        this(createDefaultValues(), false);
    }

    private GuzhenrenSnapshot(Map<String, Double> cachedValues, boolean dirty) {
        this.cachedValues = cachedValues;
        this.dirty = dirty;
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public CapabilitySnapshot capture(ServerPlayer player) {
        if (!GuzhenrenResourceBridge.isAvailable()) {
            // 模组未安装，保持静默：我们维持当前缓存，不标记脏位。
            return this;
        }
        Optional<GuzhenrenResourceBridge.ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
        if (handleOpt.isEmpty()) {
            return this;
        }
        Map<String, Double> snapshot = handleOpt.get().snapshotAll();
        this.cachedValues.clear();
        for (Map.Entry<String, Double> entry : snapshot.entrySet()) {
            Double value = entry.getValue();
            if (value != null && Double.isFinite(value)) {
                this.cachedValues.put(entry.getKey(), value);
            }
        }
        this.dirty = true;
        return this;
    }

    @Override
    public void apply(ServerPlayer player) {
        if (cachedValues.isEmpty() || !GuzhenrenResourceBridge.isAvailable()) {
            return;
        }
        Optional<GuzhenrenResourceBridge.ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
        if (handleOpt.isEmpty()) {
            return;
        }
        GuzhenrenResourceBridge.ResourceHandle handle = handleOpt.get();
        for (Map.Entry<String, Double> entry : cachedValues.entrySet()) {
            String identifier = entry.getKey();
            Double value = entry.getValue();
            if (identifier == null || value == null || !Double.isFinite(value)) {
                continue;
            }
            OptionalDouble result = handle.writeDouble(identifier, value);
            if (result.isEmpty()) {
                ChestCavity.LOGGER.debug("[soul] Failed to apply Guzhenren field {} for {}", identifier, player.getGameProfile().getName());
            }
        }
    }

    @Override
    public CompoundTag save(HolderLookup.Provider provider) {
        CompoundTag root = new CompoundTag();
        if (cachedValues.isEmpty()) {
            return root;
        }
        CompoundTag valuesTag = new CompoundTag();
        for (Map.Entry<String, Double> entry : cachedValues.entrySet()) {
            String identifier = entry.getKey();
            Double value = entry.getValue();
            if (identifier == null || value == null || !Double.isFinite(value)) {
                continue;
            }
            valuesTag.putDouble(identifier, value);
        }
        if (!valuesTag.isEmpty()) {
            root.put("values", valuesTag);
        }
        return root;
    }

    @Override
    public CapabilitySnapshot load(CompoundTag tag, HolderLookup.Provider provider) {
        this.cachedValues.clear();
        if (tag != null && tag.contains("values", Tag.TAG_COMPOUND)) {
            CompoundTag valuesTag = tag.getCompound("values");
            for (String key : valuesTag.getAllKeys()) {
                double value = valuesTag.getDouble(key);
                if (Double.isFinite(value)) {
                    this.cachedValues.put(key, value);
                }
            }
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

    public Map<String, Double> values() {
        return Collections.unmodifiableMap(cachedValues);
    }

    private static Map<String, Double> createDefaultValues() {
        // 默认构造：为每个 SoulProfile 创建独立的缓存映射。
        // 给出安全的初始寿元，避免尚未捕获前被外部兼容逻辑判死。
        Map<String, Double> map = new LinkedHashMap<>();
        map.put("shouyuan", 100.0d);
        return map;
    }
}
