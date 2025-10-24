package net.tigereye.chestcavity.client.modernui.network;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.client.modernui.skill.SkillHotbarKey;
import net.tigereye.chestcavity.skill.SkillHotbarServerData;

public record SkillHotbarUpdatePayload(Map<String, List<ResourceLocation>> bindings)
    implements CustomPacketPayload {

  public static final CustomPacketPayload.Type<SkillHotbarUpdatePayload> TYPE =
      new CustomPacketPayload.Type<>(ChestCavity.id("modernui/skill_hotbar_update"));

  public static final StreamCodec<FriendlyByteBuf, SkillHotbarUpdatePayload> STREAM_CODEC =
      StreamCodec.of(SkillHotbarUpdatePayload::write, SkillHotbarUpdatePayload::read);

  private static void write(FriendlyByteBuf buf, SkillHotbarUpdatePayload payload) {
    Map<String, List<ResourceLocation>> map = payload.bindings();
    buf.writeVarInt(map.size());
    for (Map.Entry<String, List<ResourceLocation>> entry : map.entrySet()) {
      buf.writeUtf(entry.getKey());
      List<ResourceLocation> skills = entry.getValue();
      buf.writeVarInt(skills.size());
      for (ResourceLocation id : skills) {
        buf.writeResourceLocation(id);
      }
    }
  }

  private static SkillHotbarUpdatePayload read(FriendlyByteBuf buf) {
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
    return new SkillHotbarUpdatePayload(map);
  }

  @Override
  public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }

  public static void handle(SkillHotbarUpdatePayload payload, IPayloadContext context) {
    context.enqueueWork(
        () -> {
          if (!(context.player() instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) {
            return;
          }
          Map<SkillHotbarKey, List<ResourceLocation>> map =
              SkillHotbarServerData.fromWire(payload.bindings());
          SkillHotbarServerData.save(serverPlayer, map);
          SkillHotbarServerData.sendSnapshot(serverPlayer);
        });
  }
}
