package net.tigereye.chestcavity.client.modernui.config.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.soul.ai.SoulAIOrders;
import java.util.UUID;

public record SoulConfigSetOrderPayload(UUID soulId, SoulAIOrders.Order order) implements CustomPacketPayload {

    public static final Type<SoulConfigSetOrderPayload> TYPE = new Type<>(ChestCavity.id("soul_config_set_order"));

    public static final StreamCodec<FriendlyByteBuf, SoulConfigSetOrderPayload> STREAM_CODEC =
            StreamCodec.of(SoulConfigSetOrderPayload::write, SoulConfigSetOrderPayload::read);

    private static void write(FriendlyByteBuf buf, SoulConfigSetOrderPayload payload) {
        buf.writeUUID(payload.soulId);
        buf.writeVarInt(payload.order.ordinal());
    }

    private static SoulConfigSetOrderPayload read(FriendlyByteBuf buf) {
        UUID soulId = buf.readUUID();
        int ord = buf.readVarInt();
        SoulAIOrders.Order order = SoulAIOrders.Order.values()[Math.max(0, Math.min(ord, SoulAIOrders.Order.values().length - 1))];
        return new SoulConfigSetOrderPayload(soulId, order);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SoulConfigSetOrderPayload payload, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) {
                return;
            }
            SoulAIOrders.set(serverPlayer, payload.soulId(), payload.order(), "config-set-order");
            var entries = SoulConfigNetworkHelper.buildEntries(serverPlayer);
            serverPlayer.connection.send(new SoulConfigSyncPayload(entries));
        });
    }
}
