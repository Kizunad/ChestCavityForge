package net.tigereye.chestcavity.client.modernui.config.network;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.client.modernui.config.data.PlayerPreferenceClientState;

public record PlayerPreferenceSyncPayload(Map<ResourceLocation, Boolean> entries)
    implements CustomPacketPayload {

  public static final Type<PlayerPreferenceSyncPayload> TYPE =
      new Type<>(ChestCavity.id("player_preferences_sync"));

  public static final StreamCodec<FriendlyByteBuf, PlayerPreferenceSyncPayload> STREAM_CODEC =
      StreamCodec.of(PlayerPreferenceSyncPayload::write, PlayerPreferenceSyncPayload::read);

  private static void write(FriendlyByteBuf buf, PlayerPreferenceSyncPayload payload) {
    buf.writeVarInt(payload.entries.size());
    payload.entries.forEach(
        (key, value) -> {
          buf.writeResourceLocation(key);
          buf.writeBoolean(value);
        });
  }

  private static PlayerPreferenceSyncPayload read(FriendlyByteBuf buf) {
    int size = buf.readVarInt();
    Map<ResourceLocation, Boolean> map = new HashMap<>();
    for (int i = 0; i < size; i++) {
      ResourceLocation key = buf.readResourceLocation();
      boolean value = buf.readBoolean();
      map.put(key, value);
    }
    return new PlayerPreferenceSyncPayload(map);
  }

  @Override
  public Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }

  public static void handle(PlayerPreferenceSyncPayload payload, IPayloadContext context) {
    context.enqueueWork(() -> PlayerPreferenceClientState.applyServerPayload(payload.entries()));
  }
}
