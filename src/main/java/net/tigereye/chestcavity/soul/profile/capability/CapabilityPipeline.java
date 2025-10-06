package net.tigereye.chestcavity.soul.profile.capability;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.tigereye.chestcavity.ChestCavity;

import java.util.LinkedHashMap;
import java.util.Map;

public final class CapabilityPipeline {

    private CapabilityPipeline() {
    }

    public static Map<ResourceLocation, CapabilitySnapshot> createDefaultSnapshots() {
        Map<ResourceLocation, CapabilitySnapshot> snapshots = new LinkedHashMap<>();
        for (ResourceLocation id : CapabilitySnapshotRegistry.keys()) {
            CapabilitySnapshotRegistry.create(id).ifPresent(snapshot -> snapshots.put(id, snapshot));
        }
        return snapshots;
    }

    public static Map<ResourceLocation, CapabilitySnapshot> captureFor(ServerPlayer player) {
        Map<ResourceLocation, CapabilitySnapshot> snapshots = createDefaultSnapshots();
        captureAll(snapshots, player);
        return snapshots;
    }

    public static void captureAll(Map<ResourceLocation, CapabilitySnapshot> snapshots, ServerPlayer player) {
        ensureSnapshotCoverage(snapshots);
        snapshots.values().forEach(snapshot -> snapshot.capture(player));
    }

    public static void applyAll(Map<ResourceLocation, CapabilitySnapshot> snapshots, ServerPlayer player) {
        ensureSnapshotCoverage(snapshots);
        snapshots.values().forEach(snapshot -> snapshot.apply(player));
    }

    public static CompoundTag save(Map<ResourceLocation, CapabilitySnapshot> snapshots, HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        for (Map.Entry<ResourceLocation, CapabilitySnapshot> entry : snapshots.entrySet()) {
            CapabilitySnapshot snapshot = entry.getValue();
            CompoundTag snapshotTag = snapshot.save(provider);
            if (!snapshotTag.isEmpty()) {
                tag.put(entry.getKey().toString(), snapshotTag);
            }
        }
        return tag;
    }

    public static Map<ResourceLocation, CapabilitySnapshot> load(CompoundTag tag, HolderLookup.Provider provider) {
        Map<ResourceLocation, CapabilitySnapshot> snapshots = createDefaultSnapshots();
        if (tag == null) {
            return snapshots;
        }
        for (String key : tag.getAllKeys()) {
            ResourceLocation id;
            try {
                id = ResourceLocation.parse(key);
            } catch (IllegalArgumentException ignored) {
                ChestCavity.LOGGER.warn("[soul] Ignored invalid capability snapshot id '{}' while loading", key);
                continue;
            }
            CompoundTag snapshotTag = tag.getCompound(key);
            CapabilitySnapshot snapshot = snapshots.get(id);
            if (snapshot == null) {
                snapshot = CapabilitySnapshotRegistry.create(id).orElse(null);
                if (snapshot != null) {
                    snapshots.put(id, snapshot);
                } else {
                    ChestCavity.LOGGER.warn("[soul] Missing capability snapshot factory for id={} during load", id);
                    continue;
                }
            }
            CapabilitySnapshot loaded = snapshot.load(snapshotTag, provider);
            if (loaded != snapshot) {
                snapshots.put(id, loaded);
            }
        }
        return snapshots;
    }

    public static boolean isDirty(Map<ResourceLocation, CapabilitySnapshot> snapshots) {
        return snapshots.values().stream().anyMatch(CapabilitySnapshot::isDirty);
    }

    public static void clearDirty(Map<ResourceLocation, CapabilitySnapshot> snapshots) {
        snapshots.values().forEach(CapabilitySnapshot::clearDirty);
    }

    private static void ensureSnapshotCoverage(Map<ResourceLocation, CapabilitySnapshot> snapshots) {
        for (ResourceLocation id : CapabilitySnapshotRegistry.keys()) {
            snapshots.computeIfAbsent(id, key -> CapabilitySnapshotRegistry.create(key).orElse(null));
        }
        snapshots.entrySet().removeIf(entry -> entry.getValue() == null);
    }
}
