package net.tigereye.chestcavity.soul.util;

import net.minecraft.server.level.ServerPlayer;
import net.tigereye.chestcavity.registration.CCAttachments;
import net.tigereye.chestcavity.soul.container.SoulContainer;
import net.tigereye.chestcavity.soul.storage.SoulOfflineStore;

import java.util.Map;
import java.util.UUID;

public final class SoulPersistence {

    private SoulPersistence() {}

    private static final boolean LOG_PERSISTENCE = Boolean.getBoolean("chestcavity.debugSoul.persistence");

    public static void saveAll(ServerPlayer player) {
        SoulContainer container = CCAttachments.getSoulContainer(player);
        Map<UUID, net.minecraft.nbt.CompoundTag> snapshots = container.snapshotAll(player, true);
        SoulOfflineStore.get(player.serverLevel().getServer()).putAll(player.getUUID(), snapshots);
        if (LOG_PERSISTENCE) {
            SoulLog.info("[soul] persistence-save owner={} profiles={}", player.getUUID(), snapshots.size());
        }
    }

    public static void saveDirty(ServerPlayer player) {
        SoulContainer container = CCAttachments.getSoulContainer(player);
        Map<UUID, net.minecraft.nbt.CompoundTag> snapshots = container.snapshotDirty(player, true);
        if (snapshots.isEmpty()) {
            return;
        }
        SoulOfflineStore store = SoulOfflineStore.get(player.serverLevel().getServer());
        snapshots.forEach((soulId, tag) -> store.put(player.getUUID(), soulId, tag));
        if (LOG_PERSISTENCE) {
            SoulLog.info("[soul] persistence-save-dirty owner={} profiles={}", player.getUUID(), snapshots.size());
        }
    }

    public static void loadAll(ServerPlayer player) {
        var store = SoulOfflineStore.get(player.serverLevel().getServer());
        Map<UUID, net.minecraft.nbt.CompoundTag> pending = store.consume(player.getUUID());
        if (pending.isEmpty()) {
            if (LOG_PERSISTENCE) {
                SoulLog.info("[soul] persistence-load owner={} profiles=0", player.getUUID());
            }
            return;
        }
        SoulContainer container = CCAttachments.getSoulContainer(player);
        container.restoreAll(player, pending);
        if (LOG_PERSISTENCE) {
            SoulLog.info("[soul] persistence-load owner={} profiles={}", player.getUUID(), pending.size());
        }
}
}
