package net.tigereye.chestcavity.client.modernui.config.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.client.modernui.config.data.SoulConfigDataClient;

public record SoulConfigFollowTeleportSyncPayload(boolean teleportEnabled, double followDist, double teleportDist) implements CustomPacketPayload {

    public static final Type<SoulConfigFollowTeleportSyncPayload> TYPE = new Type<>(ChestCavity.id("soul_config_follow_tp_sync"));

    public static final StreamCodec<FriendlyByteBuf, SoulConfigFollowTeleportSyncPayload> STREAM_CODEC =
            StreamCodec.of(SoulConfigFollowTeleportSyncPayload::write, SoulConfigFollowTeleportSyncPayload::read);

    private static void write(FriendlyByteBuf buf, SoulConfigFollowTeleportSyncPayload payload) {
        buf.writeBoolean(payload.teleportEnabled);
        buf.writeDouble(payload.followDist);
        buf.writeDouble(payload.teleportDist);
    }

    private static SoulConfigFollowTeleportSyncPayload read(FriendlyByteBuf buf) {
        boolean tp = buf.readBoolean();
        double follow = buf.readDouble();
        double tpDist = buf.readDouble();
        return new SoulConfigFollowTeleportSyncPayload(tp, follow, tpDist);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SoulConfigFollowTeleportSyncPayload payload, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        context.enqueueWork(() -> {
            SoulConfigDataClient.INSTANCE.updateFollowTp(new SoulConfigDataClient.FollowTpTuning(
                    payload.teleportEnabled(), payload.followDist(), payload.teleportDist()));
        });
    }
}

