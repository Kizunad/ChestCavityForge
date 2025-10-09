package net.tigereye.chestcavity.client.modernui.config.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.chat.Component;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.registration.CCAttachments;
import net.tigereye.chestcavity.soul.container.SoulContainer;
import net.tigereye.chestcavity.soul.fakeplayer.SoulFakePlayerSpawner;
import java.util.UUID;

public record SoulConfigRenamePayload(UUID soulId, String newName) implements CustomPacketPayload {

    public static final Type<SoulConfigRenamePayload> TYPE = new Type<>(ChestCavity.id("soul_config_rename"));

    public static final StreamCodec<FriendlyByteBuf, SoulConfigRenamePayload> STREAM_CODEC =
            StreamCodec.of(SoulConfigRenamePayload::write, SoulConfigRenamePayload::read);

    private static void write(FriendlyByteBuf buf, SoulConfigRenamePayload payload) {
        buf.writeUUID(payload.soulId);
        buf.writeUtf(payload.newName, 32);
    }

    private static SoulConfigRenamePayload read(FriendlyByteBuf buf) {
        return new SoulConfigRenamePayload(buf.readUUID(), buf.readUtf(32));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SoulConfigRenamePayload payload, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) {
                return;
            }
            var soulId = payload.soulId();
            String newName = payload.newName().trim();
            SoulContainer container = CCAttachments.getSoulContainer(serverPlayer);
            var ownerId = serverPlayer.getUUID();
            var activeId = container.getActiveProfileId().orElse(ownerId);
            if (!soulId.equals(ownerId) && activeId.equals(soulId)) {
                serverPlayer.displayClientMessage(Component.translatable("text.chestcavity.soul.config.cannot_rename_active"), false);
                serverPlayer.connection.send(new SoulConfigSyncPayload(SoulConfigNetworkHelper.buildEntries(serverPlayer)));
                return;
            }
            boolean changed = SoulFakePlayerSpawner.rename(serverPlayer, soulId, newName, false);
            if (changed) {
                // Apply immediately if entity is present
                SoulFakePlayerSpawner.rename(serverPlayer, soulId, newName, true);
            } else {
                serverPlayer.displayClientMessage(Component.literal("[soul] 重命名失败，检查 ID 是否正确。"), false);
            }
            serverPlayer.connection.send(new SoulConfigSyncPayload(SoulConfigNetworkHelper.buildEntries(serverPlayer)));
        });
    }
}
