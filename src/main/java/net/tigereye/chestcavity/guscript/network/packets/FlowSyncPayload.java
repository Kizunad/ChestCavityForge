package net.tigereye.chestcavity.guscript.network.packets;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.guscript.runtime.flow.FlowState;
import net.tigereye.chestcavity.guscript.runtime.flow.client.GuScriptClientFlows;

/**
 * Mirrors flow state changes to the client.
 */
public record FlowSyncPayload(
    int entityId,
    ResourceLocation programId,
    FlowState state,
    long stateGameTime,
    int ticksInState,
    List<ResourceLocation> enterFx)
    implements CustomPacketPayload {

  public FlowSyncPayload {
    enterFx = enterFx == null ? List.of() : List.copyOf(enterFx);
  }

  public static final Type<FlowSyncPayload> TYPE = new Type<>(ChestCavity.id("guscript_flow_sync"));

  public static final StreamCodec<FriendlyByteBuf, FlowSyncPayload> STREAM_CODEC =
      StreamCodec.of(
          (buf, payload) -> {
            buf.writeVarInt(payload.entityId);
            buf.writeResourceLocation(payload.programId);
            buf.writeEnum(payload.state);
            buf.writeVarLong(payload.stateGameTime);
            buf.writeVarInt(payload.ticksInState);
            buf.writeVarInt(payload.enterFx.size());
            for (ResourceLocation fx : payload.enterFx) {
              buf.writeResourceLocation(fx);
            }
          },
          buf ->
              new FlowSyncPayload(
                  buf.readVarInt(),
                  buf.readResourceLocation(),
                  buf.readEnum(FlowState.class),
                  buf.readVarLong(),
                  buf.readVarInt(),
                  readFx(buf)));

  @Override
  public Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }

  /**
   * Handles the payload.
   *
   * @param payload the payload
   * @param context the context
   */
  public static void handle(FlowSyncPayload payload, IPayloadContext context) {
    context.enqueueWork(() -> GuScriptClientFlows.handleSync(payload));
  }

  private static List<ResourceLocation> readFx(FriendlyByteBuf buf) {
    int size = buf.readVarInt();
    if (size <= 0) {
      return List.of();
    }
    List<ResourceLocation> ids = new ArrayList<>(size);
    for (int i = 0; i < size; i++) {
      ids.add(buf.readResourceLocation());
    }
    return ids;
  }
}
