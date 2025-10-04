package net.tigereye.chestcavity.soulbeast.state;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.ChestCavity;

import javax.annotation.Nullable;

/**
 * Network payload used to mirror {@link SoulBeastState} updates to clients.
 */
public record SoulBeastSyncPayload(int entityId,
                                   boolean active,
                                   boolean permanent,
                                   long lastTick,
                                   @Nullable ResourceLocation source) implements CustomPacketPayload {

    public static final Type<SoulBeastSyncPayload> TYPE =
            new Type<>(ChestCavity.id("soul_beast_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SoulBeastSyncPayload> STREAM_CODEC =
            StreamCodec.of(SoulBeastSyncPayload::encode, SoulBeastSyncPayload::decode);

    private static void encode(RegistryFriendlyByteBuf buf, SoulBeastSyncPayload payload) {
        buf.writeVarInt(payload.entityId());
        buf.writeBoolean(payload.active());
        buf.writeBoolean(payload.permanent());
        buf.writeVarLong(payload.lastTick());
        if (payload.source() != null) {
            buf.writeBoolean(true);
            buf.writeResourceLocation(payload.source());
        } else {
            buf.writeBoolean(false);
        }
    }

    private static SoulBeastSyncPayload decode(RegistryFriendlyByteBuf buf) {
        int entityId = buf.readVarInt();
        boolean active = buf.readBoolean();
        boolean permanent = buf.readBoolean();
        long lastTick = buf.readVarLong();
        ResourceLocation source = null;
        if (buf.readBoolean()) {
            source = buf.readResourceLocation();
        }
        return new SoulBeastSyncPayload(entityId, active, permanent, lastTick, source);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
