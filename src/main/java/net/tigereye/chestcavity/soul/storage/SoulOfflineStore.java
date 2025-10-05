package net.tigereye.chestcavity.soul.storage;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Stores pending soul profile snapshots for owners that log out or when the server shuts down.
 * Snapshots are merged back into the owner's container the next time they log in.
 */
public final class SoulOfflineStore extends SavedData {

    private static final String DATA_NAME = "chestcavity_soul_offline";

    private final Map<UUID, Map<UUID, CompoundTag>> pending = new HashMap<>();

    public static SoulOfflineStore get(MinecraftServer server) {
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null) {
            throw new IllegalStateException("Overworld is null while accessing SoulOfflineStore");
        }
        return overworld.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(SoulOfflineStore::new, SoulOfflineStore::load),
                DATA_NAME);
    }

    private SoulOfflineStore() {
    }

    private static SoulOfflineStore load(CompoundTag tag, HolderLookup.Provider provider) {
        SoulOfflineStore store = new SoulOfflineStore();
        if (tag == null) {
            return store;
        }
        for (String ownerKey : tag.getAllKeys()) {
            try {
                UUID ownerId = UUID.fromString(ownerKey);
                CompoundTag byOwner = tag.getCompound(ownerKey);
                Map<UUID, CompoundTag> inner = new HashMap<>();
                for (String soulKey : byOwner.getAllKeys()) {
                    try {
                        UUID soulId = UUID.fromString(soulKey);
                        inner.put(soulId, byOwner.getCompound(soulKey).copy());
                    } catch (IllegalArgumentException ignored) {
                    }
                }
                if (!inner.isEmpty()) {
                    store.pending.put(ownerId, inner);
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
        return store;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        for (Map.Entry<UUID, Map<UUID, CompoundTag>> e : pending.entrySet()) {
            CompoundTag byOwner = new CompoundTag();
            for (Map.Entry<UUID, CompoundTag> inner : e.getValue().entrySet()) {
                byOwner.put(inner.getKey().toString(), inner.getValue().copy());
            }
            tag.put(e.getKey().toString(), byOwner);
        }
        return tag;
    }

    /**
     * Replace all stored snapshots for an owner with the provided map.
     */
    public void putAll(UUID owner, Map<UUID, CompoundTag> profiles) {
        if (profiles.isEmpty()) {
            return;
        }
        Map<UUID, CompoundTag> copy = new HashMap<>();
        profiles.forEach((soulId, tag) -> copy.put(soulId, tag.copy()));
        pending.put(owner, copy);
        setDirty();
    }

    /**
     * Store/overwrite a single profile snapshot for an owner.
     */
    public void put(UUID owner, UUID soul, CompoundTag profileTag) {
        pending.computeIfAbsent(owner, id -> new HashMap<>()).put(soul, profileTag.copy());
        setDirty();
    }

    public Map<UUID, CompoundTag> consume(UUID owner) {
        Map<UUID, CompoundTag> stored = pending.remove(owner);
        if (stored == null) {
            return Collections.emptyMap();
        }
        setDirty();
        Map<UUID, CompoundTag> copy = new HashMap<>();
        stored.forEach((soulId, tag) -> copy.put(soulId, tag.copy()));
        return copy;
    }
}
