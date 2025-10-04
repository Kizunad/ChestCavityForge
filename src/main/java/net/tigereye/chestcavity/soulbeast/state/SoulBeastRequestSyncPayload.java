package net.tigereye.chestcavity.soulbeast.state;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.ChestCavity;

/**
 * Serverbound request asking the server to re-send the client's SoulBeastState snapshot.
 * This avoids overloading the clientbound soul_beast_sync id.
 */
public record SoulBeastRequestSyncPayload() implements CustomPacketPayload {

    public static final Type<SoulBeastRequestSyncPayload> TYPE =
            new Type<>(ChestCavity.id("soul_beast_request_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SoulBeastRequestSyncPayload> STREAM_CODEC =
            StreamCodec.of(SoulBeastRequestSyncPayload::encode, SoulBeastRequestSyncPayload::decode);

    private static void encode(RegistryFriendlyByteBuf buf, SoulBeastRequestSyncPayload payload) {
        // no fields
    }

    private static SoulBeastRequestSyncPayload decode(RegistryFriendlyByteBuf buf) {
        return new SoulBeastRequestSyncPayload();
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
