package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.network;

import java.util.UUID;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Payload for syncing soul flame DoT state from server to client.
 *
 * <p>Contains entity UUID, stack count, and remaining duration.
 *
 * <p>Phase 6: Network synchronization for HUD/FX.
 */
public record SoulFlameSyncPayload(UUID entityId, int stacks, int durationTicks)
    implements CustomPacketPayload {

  public static final Type<SoulFlameSyncPayload> TYPE =
      new Type<>(ResourceLocation.fromNamespaceAndPath("guzhenren", "hun_dao/soul_flame_sync"));

  public static final StreamCodec<FriendlyByteBuf, SoulFlameSyncPayload> STREAM_CODEC =
      StreamCodec.of(SoulFlameSyncPayload::write, SoulFlameSyncPayload::read);

  private static void write(FriendlyByteBuf buf, SoulFlameSyncPayload payload) {
    buf.writeUUID(payload.entityId);
    buf.writeVarInt(payload.stacks);
    buf.writeVarInt(payload.durationTicks);
  }

  private static SoulFlameSyncPayload read(FriendlyByteBuf buf) {
    UUID entityId = buf.readUUID();
    int stacks = buf.readVarInt();
    int durationTicks = buf.readVarInt();
    return new SoulFlameSyncPayload(entityId, stacks, durationTicks);
  }

  @Override
  public Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }
}
