package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.network;

import java.util.UUID;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Payload for clearing Hun Dao state for a specific entity on the client.
 *
 * <p>Sent when entity dies or state should be reset.
 *
 * <p>Phase 6: Network synchronization for HUD/FX.
 */
public record HunDaoClearEntityPayload(UUID entityId) implements CustomPacketPayload {

  public static final Type<HunDaoClearEntityPayload> TYPE =
      new Type<>(ResourceLocation.fromNamespaceAndPath("guzhenren", "hun_dao/clear_entity"));

  public static final StreamCodec<FriendlyByteBuf, HunDaoClearEntityPayload> STREAM_CODEC =
      StreamCodec.of(HunDaoClearEntityPayload::write, HunDaoClearEntityPayload::read);

  private static void write(FriendlyByteBuf buf, HunDaoClearEntityPayload payload) {
    buf.writeUUID(payload.entityId);
  }

  private static HunDaoClearEntityPayload read(FriendlyByteBuf buf) {
    UUID entityId = buf.readUUID();
    return new HunDaoClearEntityPayload(entityId);
  }

  @Override
  public Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }
}
