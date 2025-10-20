package net.tigereye.chestcavity.client.modernui.config.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.tigereye.chestcavity.client.modernui.config.network.SoulConfigSyncPayload.Entry;
import net.tigereye.chestcavity.registration.CCAttachments;
import net.tigereye.chestcavity.soul.container.SoulContainer;
import net.tigereye.chestcavity.soul.fakeplayer.service.SoulIdentityViews;
import net.tigereye.chestcavity.soul.profile.PlayerStatsSnapshot;
import net.tigereye.chestcavity.soul.profile.SoulProfile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

final class SoulConfigNetworkHelper {

    private static final ResourceLocation MAX_HEALTH_ID = ResourceLocation.withDefaultNamespace("generic.max_health");

    private SoulConfigNetworkHelper() {}

    static List<Entry> buildEntries(ServerPlayer owner) {
        SoulContainer container = CCAttachments.getSoulContainer(owner);
        UUID ownerId = owner.getUUID();
        UUID activeId = container.getActiveProfileId().orElse(ownerId);

        List<Entry> entries = new ArrayList<>();
        collectEntry(entries, owner, container, ownerId, activeId, true);
        for (UUID soulId : container.getKnownSoulIds()) {
            if (soulId.equals(ownerId)) continue;
            collectEntry(entries, owner, container, soulId, activeId, false);
        }
        return entries;
    }

    private static void collectEntry(List<Entry> entries,
                                     ServerPlayer owner,
                                     SoulContainer container,
                                     UUID soulId,
                                     UUID activeId,
                                     boolean isOwner) {
        SoulProfile profile = container.getOrCreateProfile(soulId);
        PlayerStatsSnapshot stats = profile.stats();
        if (stats == null) {
            stats = PlayerStatsSnapshot.empty();
        }
        double maxHealth = stats.attributeBaseValues().getOrDefault(MAX_HEALTH_ID, 20.0);

        String displayName = SoulIdentityViews.resolveDisplayName(owner, soulId);

        entries.add(new Entry(
                soulId,
                displayName,
                isOwner,
                activeId.equals(soulId),
                stats.health(),
                (float) maxHealth,
                stats.absorption(),
                stats.foodLevel(),
                stats.saturation(),
                stats.experienceLevel(),
                stats.experienceProgress(),
                container.getOrder(soulId)
        ));
    }
}
