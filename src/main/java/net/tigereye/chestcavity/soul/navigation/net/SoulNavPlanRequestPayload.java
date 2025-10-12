package net.tigereye.chestcavity.soul.navigation.net;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.tigereye.chestcavity.ChestCavity;

import java.util.UUID;

public record SoulNavPlanRequestPayload(long requestId, UUID soulId, Vec3 from, Vec3 target, long timeoutMs)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SoulNavPlanRequestPayload> TYPE = new CustomPacketPayload.Type<>(ChestCavity.id("soul_nav_plan_req"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SoulNavPlanRequestPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeLong(payload.requestId);
                buf.writeUUID(payload.soulId);
                buf.writeDouble(payload.from.x);
                buf.writeDouble(payload.from.y);
                buf.writeDouble(payload.from.z);
                buf.writeDouble(payload.target.x);
                buf.writeDouble(payload.target.y);
                buf.writeDouble(payload.target.z);
                buf.writeVarLong(payload.timeoutMs);
            },
            buf -> new SoulNavPlanRequestPayload(
                    buf.readLong(),
                    buf.readUUID(),
                    new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()),
                    new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()),
                    buf.readVarLong()
            )
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SoulNavPlanRequestPayload payload, IPayloadContext ctx) {
        // Client-side: compute path via Baritone and send response back
        ctx.enqueueWork(() -> BaritoneClientPlanner.onRequest(payload));
    }
}

