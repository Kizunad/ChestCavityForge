package net.tigereye.chestcavity.guscript.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.guscript.fx.client.FxClientDispatcher;

/**
 * Server-to-client payload for dispatching GuScript FX events.
 */
public record FxEventPayload(
        ResourceLocation effectId,
        double originX,
        double originY,
        double originZ,
        float directionX,
        float directionY,
        float directionZ,
        float lookX,
        float lookY,
        float lookZ,
        float intensity,
        boolean hasTarget,
        double targetX,
        double targetY,
        double targetZ,
        int performerId,
        int targetId
) implements CustomPacketPayload {

    public static final Type<FxEventPayload> TYPE = new Type<>(ChestCavity.id("guscript_fx_event"));

    public static final StreamCodec<FriendlyByteBuf, FxEventPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeResourceLocation(payload.effectId);
                buf.writeDouble(payload.originX);
                buf.writeDouble(payload.originY);
                buf.writeDouble(payload.originZ);
                buf.writeFloat(payload.directionX);
                buf.writeFloat(payload.directionY);
                buf.writeFloat(payload.directionZ);
                buf.writeFloat(payload.lookX);
                buf.writeFloat(payload.lookY);
                buf.writeFloat(payload.lookZ);
                buf.writeFloat(payload.intensity);
                buf.writeBoolean(payload.hasTarget);
                if (payload.hasTarget) {
                    buf.writeDouble(payload.targetX);
                    buf.writeDouble(payload.targetY);
                    buf.writeDouble(payload.targetZ);
                }
                buf.writeVarInt(payload.performerId);
                buf.writeVarInt(payload.targetId);
            },
            buf -> {
                ResourceLocation effectId = buf.readResourceLocation();
                double originX = buf.readDouble();
                double originY = buf.readDouble();
                double originZ = buf.readDouble();
                float directionX = buf.readFloat();
                float directionY = buf.readFloat();
                float directionZ = buf.readFloat();
                float lookX = buf.readFloat();
                float lookY = buf.readFloat();
                float lookZ = buf.readFloat();
                float intensity = buf.readFloat();
                boolean hasTarget = buf.readBoolean();
                double targetX = hasTarget ? buf.readDouble() : 0.0D;
                double targetY = hasTarget ? buf.readDouble() : 0.0D;
                double targetZ = hasTarget ? buf.readDouble() : 0.0D;
                int performerId = buf.readVarInt();
                int targetId = buf.readVarInt();
                return new FxEventPayload(effectId, originX, originY, originZ, directionX, directionY, directionZ, lookX, lookY, lookZ, intensity, hasTarget, targetX, targetY, targetZ, performerId, targetId);
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(FxEventPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist.isClient()) {
                FxClientDispatcher.handle(payload);
            }
        });
    }
}
