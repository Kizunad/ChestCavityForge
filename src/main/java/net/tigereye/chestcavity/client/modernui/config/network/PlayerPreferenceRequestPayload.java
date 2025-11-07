package net.tigereye.chestcavity.client.modernui.config.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.playerprefs.PlayerPreferenceOps;
import net.tigereye.chestcavity.registration.CCAttachments;

public record PlayerPreferenceRequestPayload() implements CustomPacketPayload {

  public static final Type<PlayerPreferenceRequestPayload> TYPE =
      new Type<>(ChestCavity.id("player_preferences_request"));

  public static final StreamCodec<FriendlyByteBuf, PlayerPreferenceRequestPayload> STREAM_CODEC =
      StreamCodec.of((buf, payload) -> {}, buf -> new PlayerPreferenceRequestPayload());

  @Override
  public Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }

  public static void handle(PlayerPreferenceRequestPayload payload, IPayloadContext context) {
    context.enqueueWork(
        () -> {
          if (!(context.player() instanceof net.minecraft.server.level.ServerPlayer player)) {
            return;
          }
          PlayerPreferenceOps.ensureBootstrapped(player);
          var prefs = CCAttachments.getPlayerPreferences(player);
          player.connection.send(new PlayerPreferenceSyncPayload(prefs.export()));
        });
  }
}
