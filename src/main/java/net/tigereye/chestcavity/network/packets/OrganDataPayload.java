package net.tigereye.chestcavity.network.packets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.chestcavities.organs.OrganData;
import net.tigereye.chestcavity.chestcavities.organs.OrganManager;

public record OrganDataPayload(List<Entry> entries) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<OrganDataPayload> TYPE = new CustomPacketPayload.Type<>(ChestCavity.id("organ_data"));

    public static final StreamCodec<FriendlyByteBuf, OrganDataPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.entries.size());
                payload.entries.forEach(entry -> {
                    buf.writeResourceLocation(entry.id());
                    buf.writeBoolean(entry.pseudo());
                    buf.writeVarInt(entry.scores().size());
                    entry.scores().forEach((ability, value) -> {
                        buf.writeResourceLocation(ability);
                        buf.writeFloat(value);
                    });
                });
            },
            buf -> {
                int count = buf.readVarInt();
                List<Entry> list = new ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                    ResourceLocation id = buf.readResourceLocation();
                    boolean pseudo = buf.readBoolean();
                    int abilityCount = buf.readVarInt();
                    Map<ResourceLocation, Float> scores = new HashMap<>();
                    for (int j = 0; j < abilityCount; j++) {
                        scores.put(buf.readResourceLocation(), buf.readFloat());
                    }
                    list.add(new Entry(id, pseudo, scores));
                }
                return new OrganDataPayload(list);
            }
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OrganDataPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            OrganManager.GeneratedOrganData.clear();
            payload.entries.forEach(entry -> {
                OrganData data = new OrganData();
                data.pseudoOrgan = entry.pseudo();
                data.organScores.putAll(entry.scores());
                OrganManager.GeneratedOrganData.put(entry.id(), data);
            });
            ChestCavity.LOGGER.info("loaded {} organs from server", payload.entries.size());
        });
    }

    public record Entry(ResourceLocation id, boolean pseudo, Map<ResourceLocation, Float> scores) {}
}
