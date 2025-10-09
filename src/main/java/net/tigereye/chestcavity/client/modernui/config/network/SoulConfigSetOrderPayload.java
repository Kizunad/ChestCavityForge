package net.tigereye.chestcavity.client.modernui.config.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.chat.Component;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.registration.CCAttachments;
import net.tigereye.chestcavity.soul.ai.SoulAIOrders;
import net.tigereye.chestcavity.soul.container.SoulContainer;
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
            UUID soulId = payload.soulId();
            SoulContainer container = CCAttachments.getSoulContainer(serverPlayer);
            UUID ownerId = serverPlayer.getUUID();
            UUID activeId = container.getActiveProfileId().orElse(ownerId);
            if (!soulId.equals(ownerId) && activeId.equals(soulId)) {
                serverPlayer.displayClientMessage(Component.translatable("text.chestcavity.soul.config.cannot_change_order_active"), false);
                serverPlayer.connection.send(new SoulConfigSyncPayload(SoulConfigNetworkHelper.buildEntries(serverPlayer)));
                return;
            }
            SoulAIOrders.set(serverPlayer, soulId, payload.order(), "config-set-order");
            serverPlayer.connection.send(new SoulConfigSyncPayload(SoulConfigNetworkHelper.buildEntries(serverPlayer)));
        });
    }
}
