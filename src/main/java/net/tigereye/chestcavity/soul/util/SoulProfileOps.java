package net.tigereye.chestcavity.soul.util;

import net.minecraft.core.HolderLookup;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.tigereye.chestcavity.registration.CCAttachments;
import net.tigereye.chestcavity.soul.container.SoulContainer;
import net.tigereye.chestcavity.soul.fakeplayer.SoulFakePlayerSpawner;
import net.tigereye.chestcavity.soul.profile.SoulProfile;
import net.tigereye.chestcavity.soul.storage.SoulOfflineStore;

import java.util.UUID;

/**
 * Shared helpers for manipulating soul profiles, attachment persistence and offline queueing.
 */
public final class SoulProfileOps {

    private SoulProfileOps() {
    }

    public static void registerAutospawn(ServerPlayer ownerPlayer,
                                         SoulContainer container,
                                         UUID profileId,
                                         String reason) {
        UUID ownerId = ownerPlayer.getUUID();
        if (profileId.equals(ownerId)) {
            SoulLog.info("[soul] autospawn-skip reason={} owner={} profile={} (owner profile)", reason, ownerId, profileId);
            return;
        }
        container.addAutospawnSoul(profileId);
        ownerPlayer.setData(CCAttachments.SOUL_CONTAINER.get(), container);
        SoulLog.info("[soul] autospawn-register reason={} owner={} soul={}", reason, ownerId, profileId);
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

    public static void markResumeActive(ServerPlayer player,
                                        SoulContainer container,
                                        UUID activeSoulId) {
        UUID owner = player.getUUID();
        if (activeSoulId.equals(owner)) {
            return;
        }
        container.setResumeActiveOnLogin(activeSoulId);
        player.setData(CCAttachments.SOUL_CONTAINER.get(), container);
        SoulLog.info("[soul] logout-markResumeActive owner={} soul={}", owner, activeSoulId);
    }

    public static void tryResumeActive(ServerPlayer player) {
        SoulContainer container = CCAttachments.getSoulContainer(player);
        UUID owner = player.getUUID();
        container.getResumeActiveOnLogin().ifPresent(resumeId -> {
            if (!container.hasProfile(resumeId)) {
                SoulLog.warn("[soul] login-resumeActive missing profile owner={} soul={}", owner, resumeId);
            }
            else if (SoulFakePlayerSpawner.findSoulPlayer(resumeId).isPresent()) {
                SoulLog.info("[soul] login-resumeActive owner={} soul={} skipped=alreadySpawned", owner, resumeId);
            }
            else {
                SoulLog.info("[soul] login-resumeActive owner={} soul={}", owner, resumeId);
                SoulFakePlayerSpawner.saveSoulPlayerState(owner);
                SoulFakePlayerSpawner.respawnSoulFromProfile(player,
                        resumeId,
                        player.getGameProfile(),
                        true,
                        "login-resumeActive");
            }
            container.setResumeActiveOnLogin(null);
            player.setData(CCAttachments.SOUL_CONTAINER.get(), container);
        });
    }
}
