package net.tigereye.chestcavity.guscript.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.guscript.runtime.flow.FlowControllerManager;
import net.tigereye.chestcavity.guscript.runtime.flow.FlowInput;

/** Client-to-server input for flow release/cancel actions. */
public record FlowInputPayload(FlowInput input) implements CustomPacketPayload {

  public static final Type<FlowInputPayload> TYPE =
      new Type<>(ChestCavity.id("guscript_flow_input"));

  public static final StreamCodec<FriendlyByteBuf, FlowInputPayload> STREAM_CODEC =
      StreamCodec.of(
          (buf, payload) -> buf.writeEnum(payload.input),
          buf -> new FlowInputPayload(buf.readEnum(FlowInput.class)));

  @Override
  public Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }

  /**
   * Handles the received payload.
   *
   * @param context the network context
   */
  public static void handle(FlowInputPayload payload, IPayloadContext context) {
    context.enqueueWork(
        () -> {
          if (context.player() instanceof ServerPlayer player) {
            FlowControllerManager.get(player)
                .handleInput(payload.input, player.level().getGameTime());
          }
        });
  }
}
