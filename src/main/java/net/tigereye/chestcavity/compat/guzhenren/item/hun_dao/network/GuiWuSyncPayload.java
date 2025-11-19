package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.network;

import java.util.UUID;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Payload for syncing gui wu (ghost mist) state from server to client.
 *
 * <p>Contains player UUID, active flag, and remaining duration.
 *
 * <p>Phase 6: Network synchronization for HUD/FX.
 */
public record GuiWuSyncPayload(UUID playerId, boolean active, int durationTicks)
    implements CustomPacketPayload {

  public static final Type<GuiWuSyncPayload> TYPE =
      new Type<>(ResourceLocation.fromNamespaceAndPath("guzhenren", "hun_dao/gui_wu_sync"));

  public static final StreamCodec<FriendlyByteBuf, GuiWuSyncPayload> STREAM_CODEC =
      StreamCodec.of(GuiWuSyncPayload::write, GuiWuSyncPayload::read);

  private static void write(FriendlyByteBuf buf, GuiWuSyncPayload payload) {
    buf.writeUUID(payload.playerId);
    buf.writeBoolean(payload.active);
    buf.writeVarInt(payload.durationTicks);
  }

  private static GuiWuSyncPayload read(FriendlyByteBuf buf) {
    UUID playerId = buf.readUUID();
    boolean active = buf.readBoolean();
    int durationTicks = buf.readVarInt();
    return new GuiWuSyncPayload(playerId, active, durationTicks);
  }

  @Override
  public Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }
}
