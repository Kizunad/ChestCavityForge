package net.tigereye.chestcavity.client.modernui.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.client.modernui.skill.SkillHotbarClientData;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record SkillHotbarSnapshotPayload(Map<String, List<ResourceLocation>> bindings) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SkillHotbarSnapshotPayload> TYPE =
            new CustomPacketPayload.Type<>(ChestCavity.id("modernui/skill_hotbar_snapshot"));

    public static final StreamCodec<FriendlyByteBuf, SkillHotbarSnapshotPayload> STREAM_CODEC =
            StreamCodec.of(SkillHotbarSnapshotPayload::write, SkillHotbarSnapshotPayload::read);

    private static void write(FriendlyByteBuf buf, SkillHotbarSnapshotPayload payload) {
        Map<String, List<ResourceLocation>> map = payload.bindings();
        buf.writeVarInt(map.size());
        for (Map.Entry<String, List<ResourceLocation>> entry : map.entrySet()) {
            buf.writeUtf(entry.getKey());
            List<ResourceLocation> values = entry.getValue();
            buf.writeVarInt(values.size());
            for (ResourceLocation id : values) {
                buf.writeResourceLocation(id);
            }
        }
    }

    private static SkillHotbarSnapshotPayload read(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        Map<String, List<ResourceLocation>> map = new LinkedHashMap<>(size);
        for (int i = 0; i < size; i++) {
            String key = buf.readUtf();
            int count = buf.readVarInt();
            List<ResourceLocation> skills = new ArrayList<>(count);
            for (int j = 0; j < count; j++) {
                skills.add(buf.readResourceLocation());
            }
            map.put(key, skills);
        }
        return new SkillHotbarSnapshotPayload(map);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SkillHotbarSnapshotPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> SkillHotbarClientData.applySnapshot(payload.bindings(), true));
    }
}
