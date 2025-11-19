package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.network;

import java.util.UUID;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Payload for syncing soul beast transformation state from server to client.
 *
 * <p>Contains player UUID, active flag, and remaining duration.
 *
 * <p>Phase 6: Network synchronization for HUD/FX.
 */
public record SoulBeastSyncPayload(UUID playerId, boolean active, int durationTicks)
    implements CustomPacketPayload {

  public static final Type<SoulBeastSyncPayload> TYPE =
      new Type<>(ResourceLocation.fromNamespaceAndPath("guzhenren", "hun_dao/soul_beast_sync"));

  public static final StreamCodec<FriendlyByteBuf, SoulBeastSyncPayload> STREAM_CODEC =
      StreamCodec.of(SoulBeastSyncPayload::write, SoulBeastSyncPayload::read);

  private static void write(FriendlyByteBuf buf, SoulBeastSyncPayload payload) {
    buf.writeUUID(payload.playerId);
    buf.writeBoolean(payload.active);
    buf.writeVarInt(payload.durationTicks);
  }

  private static SoulBeastSyncPayload read(FriendlyByteBuf buf) {
    UUID playerId = buf.readUUID();
    boolean active = buf.readBoolean();
    int durationTicks = buf.readVarInt();
    return new SoulBeastSyncPayload(playerId, active, durationTicks);
  }

  @Override
  public Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }
}
