package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.network;

import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Payload for syncing hun po (soul essence) resource from server to client.
 *
 * <p>Contains player UUID, current hun po, and maximum hun po.
 *
 * <p>Phase 6: Network synchronization for HUD/FX.
 */
public record HunPoSyncPayload(UUID playerId, double current, double max)
    implements CustomPacketPayload {

  public static final Type<HunPoSyncPayload> TYPE =
      new Type<>(ResourceLocation.fromNamespaceAndPath("guzhenren", "hun_dao/hun_po_sync"));

  public static final StreamCodec<FriendlyByteBuf, HunPoSyncPayload> STREAM_CODEC =
      StreamCodec.of(HunPoSyncPayload::write, HunPoSyncPayload::read);

  private static void write(FriendlyByteBuf buf, HunPoSyncPayload payload) {
    buf.writeUUID(payload.playerId);
    buf.writeDouble(payload.current);
    buf.writeDouble(payload.max);
  }

  private static HunPoSyncPayload read(FriendlyByteBuf buf) {
    UUID playerId = buf.readUUID();
    double current = buf.readDouble();
    double max = buf.readDouble();
    return new HunPoSyncPayload(playerId, current, max);
  }

  @Override
  public Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }
}
