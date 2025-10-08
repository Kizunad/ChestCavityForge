package net.tigereye.chestcavity.client.modernui.config.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.client.modernui.config.data.SoulConfigDataClient;
import net.tigereye.chestcavity.registration.CCAttachments;
import net.tigereye.chestcavity.soul.container.SoulContainer;
import net.tigereye.chestcavity.soul.fakeplayer.SoulFakePlayerSpawner;
import net.tigereye.chestcavity.client.modernui.config.network.SoulConfigSyncPayload.Entry;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public record SoulConfigRequestPayload() implements CustomPacketPayload {

    public static final Type<SoulConfigRequestPayload> TYPE = new Type<>(ChestCavity.id("soul_config_request"));

    public static final StreamCodec<FriendlyByteBuf, SoulConfigRequestPayload> STREAM_CODEC =
            StreamCodec.of((buf, payload) -> {}, buf -> new SoulConfigRequestPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SoulConfigRequestPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) {
                return;
            }
            SoulContainer container = CCAttachments.getSoulContainer(serverPlayer);
            UUID ownerId = serverPlayer.getUUID();
            UUID activeId = container.getActiveProfileId().orElse(ownerId);

            List<Entry> entries = new ArrayList<>();
            // include owner baseline first
            collectEntry(entries, serverPlayer, container, ownerId, activeId, true);
            for (UUID soulId : container.getKnownSoulIds()) {
                if (soulId.equals(ownerId)) {
                    continue;
                }
                collectEntry(entries, serverPlayer, container, soulId, activeId, false);
            }
            serverPlayer.connection.send(new SoulConfigSyncPayload(entries));
        });
    }

    private static void collectEntry(List<Entry> entries,
                                     net.minecraft.server.level.ServerPlayer owner,
                                     SoulContainer container,
                                     UUID soulId,
                                     UUID activeId,
                                     boolean isOwner) {
        var profile = container.getOrCreateProfile(soulId);
        var stats = profile.stats();
        if (stats == null) {
            stats = net.tigereye.chestcavity.soul.profile.PlayerStatsSnapshot.empty();
        }

        String displayName;
        if (isOwner) {
            displayName = owner.getGameProfile().getName();
        } else {
            displayName = container.getName(soulId);
            if (displayName == null || displayName.isBlank()) {
                displayName = SoulFakePlayerSpawner.resolveDisplayName(owner, soulId);
            }
        }

        double maxHealth = stats.attributeBaseValues()
                .getOrDefault(net.minecraft.resources.ResourceLocation.withDefaultNamespace("generic.max_health"), 20.0);

        entries.add(new Entry(
                soulId,
                displayName,
                activeId.equals(soulId),
                stats.health(),
                (float) maxHealth,
                stats.absorption(),
                stats.foodLevel(),
                stats.saturation(),
                stats.experienceLevel(),
                stats.experienceProgress()
        ));
    }
}
