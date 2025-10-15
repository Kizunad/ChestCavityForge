package net.tigereye.chestcavity.soul.util;

import net.minecraft.server.level.ServerPlayer;
import net.tigereye.chestcavity.registration.CCAttachments;
import net.tigereye.chestcavity.soul.container.SoulContainer;
import net.tigereye.chestcavity.soul.profile.SoulProfile;
import net.tigereye.chestcavity.soul.profile.PlayerPositionSnapshot;
import net.minecraft.server.level.ServerLevel;

public final class SoulProfileOps {

    private static final boolean LOG_CONTAINER_DIRTY = Boolean.getBoolean("chestcavity.debugSoul.containerDirty");

    private SoulProfileOps() {
    }

    public static void markContainerDirty(ServerPlayer player,
                                          SoulContainer container,
                                          String reason) {
        player.setData(CCAttachments.SOUL_CONTAINER.get(), container);
        if (LOG_CONTAINER_DIRTY) {
            SoulLog.info("[soul] container-dirty reason={} owner={} active={}",
                    reason,
                    player.getUUID(),
                    container.getActiveProfileId().orElse(player.getUUID()));
        }
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
            // 夹取 Y，避免越界/NaN 导致的“无效玩家数据”。
            double minY = targetLevel.getMinBuildHeight() + 1;
            double maxY = targetLevel.getMaxBuildHeight() - 2;
            double safeY = snapshot.y();
            if (!Double.isFinite(safeY)) {
                safeY = Math.max(minY, Math.min(maxY, player.getY()));
            } else {
                safeY = Math.max(minY, Math.min(maxY, safeY));
            }
            player.teleportTo(targetLevel,
                    snapshot.x(),
                    safeY,
                    snapshot.z(),
                    snapshot.yaw(),
                    snapshot.pitch());
            player.setYHeadRot(snapshot.headYaw());
        }
    }
}
