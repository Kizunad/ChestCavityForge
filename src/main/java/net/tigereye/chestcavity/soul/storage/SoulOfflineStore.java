package net.tigereye.chestcavity.soul.storage;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Persistent store for offline soul profile updates. When an owner is offline during a save
 * (e.g., singleplayer server shutting down right after logout), we queue the snapshot here and
 * merge it back into the owner's SoulContainer on next login.
 */
public class SoulOfflineStore extends SavedData {

    private static final String DATA_NAME = "chestcavity_soul_offline";

    private final Map<UUID, Map<UUID, CompoundTag>> pending = new HashMap<>();

    public static SoulOfflineStore get(MinecraftServer server) {
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null) {
            throw new IllegalStateException("Overworld is null while accessing SoulOfflineStore");
        }
        return overworld.getDataStorage().computeIfAbsent(new SavedData.Factory<SoulOfflineStore>(SoulOfflineStore::new, SoulOfflineStore::load), DATA_NAME);
    }

    public static SoulOfflineStore load(CompoundTag tag, net.minecraft.core.HolderLookup.Provider provider) {
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
    public CompoundTag save(CompoundTag tag, net.minecraft.core.HolderLookup.Provider provider) {
        for (Map.Entry<UUID, Map<UUID, CompoundTag>> e : pending.entrySet()) {
            CompoundTag byOwner = new CompoundTag();
            for (Map.Entry<UUID, CompoundTag> inner : e.getValue().entrySet()) {
                byOwner.put(inner.getKey().toString(), inner.getValue().copy());
            }
            tag.put(e.getKey().toString(), byOwner);
        }
        return tag;
    }

    public void put(UUID owner, UUID soul, CompoundTag profileTag) {
        pending.computeIfAbsent(owner, k -> new HashMap<>()).put(soul, profileTag.copy());
        setDirty();
    }

    public Map<UUID, CompoundTag> consume(UUID owner) {
        Map<UUID, CompoundTag> m = pending.remove(owner);
        if (m == null) {
            return java.util.Collections.emptyMap();
        }
        setDirty();
        return m;
    }
}
