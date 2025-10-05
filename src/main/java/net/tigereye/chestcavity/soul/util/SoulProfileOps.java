package net.tigereye.chestcavity.soul.util;

import net.minecraft.core.HolderLookup;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.tigereye.chestcavity.registration.CCAttachments;
import net.tigereye.chestcavity.soul.container.SoulContainer;
import net.tigereye.chestcavity.soul.profile.SoulProfile;
import net.tigereye.chestcavity.soul.storage.SoulOfflineStore;
import net.tigereye.chestcavity.soul.profile.PlayerPositionSnapshot;

import java.util.UUID;

public final class SoulProfileOps {

    private SoulProfileOps() {
    }

    public static void queueOfflineSnapshot(MinecraftServer server,
                                            UUID owner,
                                            UUID profileId,
                                            SoulProfile profile,
                                            HolderLookup.Provider provider,
                                            String reason) {
        SoulOfflineStore.get(server).put(owner, profileId, profile.save(provider));
        SoulLog.info("[soul] offline-queue reason={} owner={} soul={}", reason, owner, profileId);
    }

    public static void markContainerDirty(ServerPlayer player,
                                          SoulContainer container,
                                          String reason) {
        player.setData(CCAttachments.SOUL_CONTAINER.get(), container);
        SoulLog.info("[soul] container-dirty reason={} owner={} active={}",
                reason,
                player.getUUID(),
                container.getActiveProfileId().orElse(player.getUUID()));
    }

    public static void applyProfileToPlayer(SoulProfile profile,
                                            ServerPlayer player,
                                            String reason) {
        profile.restoreBase(player);
        profile.position().ifPresent(snapshot -> teleportToSnapshot(snapshot, player));
        SoulLog.info("[soul] apply-profile reason={} soul={} dim={} pos=({},{},{})",
                reason,
                profile.id(),
                profile.position().map(PlayerPositionSnapshot::dimension).map(k -> k.location().toString()).orElse("unknown"),
                profile.position().map(PlayerPositionSnapshot::x).orElse(player.getX()),
                profile.position().map(PlayerPositionSnapshot::y).orElse(player.getY()),
                profile.position().map(PlayerPositionSnapshot::z).orElse(player.getZ()));
    }

    private static void teleportToSnapshot(PlayerPositionSnapshot snapshot, ServerPlayer player) {
        ServerLevel targetLevel = player.server.getLevel(snapshot.dimension());
        if (targetLevel != null) {
            player.teleportTo(targetLevel,
                    snapshot.x(),
                    snapshot.y(),
                    snapshot.z(),
                    snapshot.yaw(),
                    snapshot.pitch());
            player.setYHeadRot(snapshot.headYaw());
        }
    }
}
