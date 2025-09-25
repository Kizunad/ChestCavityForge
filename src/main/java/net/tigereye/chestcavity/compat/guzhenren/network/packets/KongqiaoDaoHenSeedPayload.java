package net.tigereye.chestcavity.compat.guzhenren.network.packets;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.ChestCavity;

public record KongqiaoDaoHenSeedPayload(Map<ResourceLocation, Double> seeds) implements CustomPacketPayload {

    public static final Type<KongqiaoDaoHenSeedPayload> TYPE =
            new Type<>(ChestCavity.id("kongqiao_daohen_seed"));

    public static final StreamCodec<RegistryFriendlyByteBuf, KongqiaoDaoHenSeedPayload> STREAM_CODEC =
            StreamCodec.of(KongqiaoDaoHenSeedPayload::encode, KongqiaoDaoHenSeedPayload::decode);

    private static void encode(RegistryFriendlyByteBuf buf, KongqiaoDaoHenSeedPayload payload) {
        Map<ResourceLocation, Double> entries = payload.seeds == null ? Map.of() : payload.seeds;
        buf.writeVarInt(entries.size());
        entries.forEach((id, value) -> {
            buf.writeResourceLocation(id);
            buf.writeDouble(value);
        });
    }

    private static KongqiaoDaoHenSeedPayload decode(RegistryFriendlyByteBuf buf) {
        int size = buf.readVarInt();
        Map<ResourceLocation, Double> entries = new LinkedHashMap<>(size);
        for (int i = 0; i < size; i++) {
            ResourceLocation id = buf.readResourceLocation();
            double value = buf.readDouble();
            entries.put(id, value);
        }
        return new KongqiaoDaoHenSeedPayload(Map.copyOf(entries));
    }

    @Override
    public Type<KongqiaoDaoHenSeedPayload> type() {
        return TYPE;
    }
}

