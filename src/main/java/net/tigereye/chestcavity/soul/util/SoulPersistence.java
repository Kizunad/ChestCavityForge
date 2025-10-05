package net.tigereye.chestcavity.soul.util;

import net.minecraft.server.level.ServerPlayer;
import net.tigereye.chestcavity.registration.CCAttachments;
import net.tigereye.chestcavity.soul.container.SoulContainer;
import net.tigereye.chestcavity.soul.storage.SoulOfflineStore;

import java.util.Map;
import java.util.UUID;

public final class SoulPersistence {

    private SoulPersistence() {}

    public static void saveAll(ServerPlayer player) {
        SoulContainer container = CCAttachments.getSoulContainer(player);
        Map<UUID, net.minecraft.nbt.CompoundTag> snapshots = container.snapshotAll(player, true);
        SoulOfflineStore.get(player.serverLevel().getServer()).putAll(player.getUUID(), snapshots);
        SoulLog.info("[soul] persistence-save owner={} profiles={}", player.getUUID(), snapshots.size());
    }

    public static void loadAll(ServerPlayer player) {
        var store = SoulOfflineStore.get(player.serverLevel().getServer());
        Map<UUID, net.minecraft.nbt.CompoundTag> pending = store.consume(player.getUUID());
        if (pending.isEmpty()) {
            SoulLog.info("[soul] persistence-load owner={} profiles=0", player.getUUID());
            return;
        }
        SoulContainer container = CCAttachments.getSoulContainer(player);
        container.restoreAll(player, pending);
        SoulLog.info("[soul] persistence-load owner={} profiles={}", player.getUUID(), pending.size());
    }
}
