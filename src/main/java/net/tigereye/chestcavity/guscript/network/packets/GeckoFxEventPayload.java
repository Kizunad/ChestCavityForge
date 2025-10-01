package net.tigereye.chestcavity.guscript.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.guscript.runtime.flow.fx.GeckoFxAnchor;
import net.tigereye.chestcavity.guscript.fx.gecko.client.GeckoFxClient;

import java.util.UUID;

/**
 * S2C payload for GeckoLib-powered FX dispatched from flow actions.
 */
public record GeckoFxEventPayload(
        ResourceLocation fxId,
        GeckoFxAnchor anchor,
        int attachedEntityId,
        double basePosX,
        double basePosY,
        double basePosZ,
        double offsetX,
        double offsetY,
        double offsetZ,
        double relativeOffsetX,
        double relativeOffsetY,
        double relativeOffsetZ,
        float yaw,
        float pitch,
        float roll,
        float scale,
        int tint,
        float alpha,
        boolean loop,
        int duration,
        UUID eventId
) implements CustomPacketPayload {

    public static final Type<GeckoFxEventPayload> TYPE = new Type<>(ChestCavity.id("gecko_fx_event"));

    public static final StreamCodec<FriendlyByteBuf, GeckoFxEventPayload> STREAM_CODEC = StreamCodec.of(
            GeckoFxEventPayload::write,
            GeckoFxEventPayload::read
    );

    private static void write(FriendlyByteBuf buf, GeckoFxEventPayload payload) {
        buf.writeResourceLocation(payload.fxId);
        buf.writeEnum(payload.anchor);
        buf.writeVarInt(payload.attachedEntityId);
        buf.writeDouble(payload.basePosX);
        buf.writeDouble(payload.basePosY);
        buf.writeDouble(payload.basePosZ);
        buf.writeDouble(payload.offsetX);
        buf.writeDouble(payload.offsetY);
        buf.writeDouble(payload.offsetZ);
        buf.writeDouble(payload.relativeOffsetX);
        buf.writeDouble(payload.relativeOffsetY);
        buf.writeDouble(payload.relativeOffsetZ);
        buf.writeFloat(payload.yaw);
        buf.writeFloat(payload.pitch);
        buf.writeFloat(payload.roll);
        buf.writeFloat(payload.scale);
        buf.writeInt(payload.tint);
        buf.writeFloat(payload.alpha);
        buf.writeBoolean(payload.loop);
        buf.writeVarInt(payload.duration);
        buf.writeUUID(payload.eventId);
    }

    private static GeckoFxEventPayload read(FriendlyByteBuf buf) {
        ResourceLocation fxId = buf.readResourceLocation();
        GeckoFxAnchor anchor = buf.readEnum(GeckoFxAnchor.class);
        int attachedEntityId = buf.readVarInt();
        double basePosX = buf.readDouble();
        double basePosY = buf.readDouble();
        double basePosZ = buf.readDouble();
        double offsetX = buf.readDouble();
        double offsetY = buf.readDouble();
        double offsetZ = buf.readDouble();
        double relativeOffsetX = buf.readDouble();
        double relativeOffsetY = buf.readDouble();
        double relativeOffsetZ = buf.readDouble();
        float yaw = buf.readFloat();
        float pitch = buf.readFloat();
        float roll = buf.readFloat();
        float scale = buf.readFloat();
        int tint = buf.readInt();
        float alpha = buf.readFloat();
        boolean loop = buf.readBoolean();
        int duration = buf.readVarInt();
        UUID eventId = buf.readUUID();
        return new GeckoFxEventPayload(
                fxId,
                anchor,
                attachedEntityId,
                basePosX,
                basePosY,
                basePosZ,
                offsetX,
                offsetY,
                offsetZ,
                relativeOffsetX,
                relativeOffsetY,
                relativeOffsetZ,
                yaw,
                pitch,
                roll,
                scale,
                tint,
                alpha,
                loop,
                duration,
                eventId
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(GeckoFxEventPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist.isClient()) {
                GeckoFxClient.handle(payload);
            }
        });
    }
}
