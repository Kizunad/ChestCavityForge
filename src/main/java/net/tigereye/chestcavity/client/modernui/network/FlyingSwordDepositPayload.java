package net.tigereye.chestcavity.client.modernui.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.tigereye.chestcavity.ChestCavity;

public record FlyingSwordDepositPayload(int index1) implements CustomPacketPayload {

  public static final CustomPacketPayload.Type<FlyingSwordDepositPayload> TYPE =
      new CustomPacketPayload.Type<>(ChestCavity.id("flyingsword_deposit"));

  public static final StreamCodec<FriendlyByteBuf, FlyingSwordDepositPayload> STREAM_CODEC =
      StreamCodec.of(
          (buf, payload) -> buf.writeVarInt(payload.index1),
          buf -> new FlyingSwordDepositPayload(buf.readVarInt()));

  @Override
  public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }

  public static void handle(FlyingSwordDepositPayload payload, IPayloadContext context) {
    context.enqueueWork(
        () -> {
          if (context.player() instanceof net.minecraft.server.level.ServerPlayer sp) {
            net.tigereye.chestcavity.compat.guzhenren.flyingsword.ui.FlyingSwordTUIOps
                .depositMainHand(sp.serverLevel(), sp, payload.index1);
          }
        });
  }
}
