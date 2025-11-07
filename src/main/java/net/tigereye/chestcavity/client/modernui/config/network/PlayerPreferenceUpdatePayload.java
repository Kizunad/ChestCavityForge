package net.tigereye.chestcavity.client.modernui.config.network;

import java.util.Map;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.registration.CCAttachments;

public record PlayerPreferenceUpdatePayload(ResourceLocation key, boolean value)
    implements CustomPacketPayload {

  public static final Type<PlayerPreferenceUpdatePayload> TYPE =
      new Type<>(ChestCavity.id("player_preferences_update"));

  public static final StreamCodec<FriendlyByteBuf, PlayerPreferenceUpdatePayload> STREAM_CODEC =
      StreamCodec.of(
          (buf, payload) -> {
            buf.writeResourceLocation(payload.key);
            buf.writeBoolean(payload.value);
          },
          buf -> new PlayerPreferenceUpdatePayload(buf.readResourceLocation(), buf.readBoolean()));

  @Override
  public Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }

  public static void handle(PlayerPreferenceUpdatePayload payload, IPayloadContext context) {
    context.enqueueWork(
        () -> {
          if (!(context.player() instanceof net.minecraft.server.level.ServerPlayer player)) {
            return;
          }
          CCAttachments.getPlayerPreferences(player).setBoolean(payload.key(), payload.value());
          player.connection.send(
              new PlayerPreferenceSyncPayload(Map.of(payload.key(), payload.value())));
        });
  }
}
