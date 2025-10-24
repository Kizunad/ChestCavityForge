package net.tigereye.chestcavity.network.packets;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.registration.CCAttachments;

public record ChestCavityUpdatePayload(boolean open, Map<ResourceLocation, Float> organScores)
    implements CustomPacketPayload {

  public static final CustomPacketPayload.Type<ChestCavityUpdatePayload> TYPE =
      new CustomPacketPayload.Type<>(ChestCavity.id("update"));

  public static final StreamCodec<RegistryFriendlyByteBuf, ChestCavityUpdatePayload> STREAM_CODEC =
      StreamCodec.of(
          (buf, payload) -> {
            buf.writeBoolean(payload.open);
            buf.writeVarInt(payload.organScores.size());
            payload.organScores.forEach(
                (id, value) -> {
                  buf.writeResourceLocation(id);
                  buf.writeFloat(value);
                });
          },
          buf -> {
            boolean open = buf.readBoolean();
            int size = buf.readVarInt();
            Map<ResourceLocation, Float> organScores = new HashMap<>();
            for (int i = 0; i < size; i++) {
              organScores.put(buf.readResourceLocation(), buf.readFloat());
            }
            return new ChestCavityUpdatePayload(open, organScores);
          });

  @Override
  public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }

  public static void handle(ChestCavityUpdatePayload payload, IPayloadContext context) {
    context.enqueueWork(
        () -> {
          var entity = context.player();
          if (!(entity instanceof LivingEntity living)) {
            return;
          }
          var instance = CCAttachments.getChestCavity(living);
          instance.opened = payload.open;
          instance.setOrganScores(payload.organScores);
        });
  }
}
