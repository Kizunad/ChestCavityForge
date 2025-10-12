package net.tigereye.chestcavity.soul.navigation.net;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.tigereye.chestcavity.ChestCavity;

import java.util.ArrayList;
import java.util.List;

public record SoulNavPlanResponsePayload(long requestId, List<Vec3> waypoints)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SoulNavPlanResponsePayload> TYPE = new CustomPacketPayload.Type<>(ChestCavity.id("soul_nav_plan_res"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SoulNavPlanResponsePayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeLong(payload.requestId);
                buf.writeVarInt(payload.waypoints.size());
                for (Vec3 v : payload.waypoints) {
                    buf.writeDouble(v.x);
                    buf.writeDouble(v.y);
                    buf.writeDouble(v.z);
                }
            },
            buf -> {
                long id = buf.readLong();
                int n = buf.readVarInt();
                List<Vec3> points = new ArrayList<>(Math.max(0, n));
                for (int i = 0; i < n; i++) {
                    points.add(new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()));
                }
                return new SoulNavPlanResponsePayload(id, points);
            }
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SoulNavPlanResponsePayload payload, IPayloadContext ctx) {
        // Server-side: deliver response to waiting future
        ctx.enqueueWork(() -> SoulNavPlanBroker.onResponse(payload));
    }
}

