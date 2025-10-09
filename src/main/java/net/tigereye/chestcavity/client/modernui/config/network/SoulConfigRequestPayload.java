package net.tigereye.chestcavity.client.modernui.config.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.client.modernui.config.network.SoulConfigSyncPayload.Entry;

import java.util.List;

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
            List<Entry> entries = SoulConfigNetworkHelper.buildEntries(serverPlayer);
            serverPlayer.connection.send(new SoulConfigSyncPayload(entries));
        });
    }
}
