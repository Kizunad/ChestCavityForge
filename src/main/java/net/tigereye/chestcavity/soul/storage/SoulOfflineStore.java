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
 * 灵魂离线存储。
 *
 * <p>当宿主玩家离线或服务器重启时，将其最新的灵魂快照写入 {@link SavedData}，等下一次玩家登录时再回填至容器。
 * 这样可以避免在停服过程中丢失 FakePlayer 状态或跨维位置等敏感数据。</p>
 */
public final class SoulOfflineStore extends SavedData {

    private static final String DATA_NAME = "chestcavity_soul_offline";

    private final Map<UUID, Map<UUID, CompoundTag>> pending = new HashMap<>();

    /**
     * 从主世界的数据存储加载（或创建）离线存储实例。
     */
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

    /**
     * SavedData 反序列化入口。将嵌套结构展开为 owner → soul → snapshot。
     */
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
    /**
     * SavedData 序列化入口。按 owner → soul 分组写回所有待处理的快照。
     */
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
     * 用提供的快照映射替换指定玩家的所有离线数据。
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
     * 更新单个灵魂的离线快照。
     */
    public void put(UUID owner, UUID soul, CompoundTag profileTag) {
        pending.computeIfAbsent(owner, id -> new HashMap<>()).put(soul, profileTag.copy());
        setDirty();
    }

    /**
     * 取回并清除某个玩家的所有离线快照，通常在玩家重新登录时调用。
     */
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
