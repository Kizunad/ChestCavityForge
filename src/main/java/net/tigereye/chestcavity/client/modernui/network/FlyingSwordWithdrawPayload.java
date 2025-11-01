package net.tigereye.chestcavity.client.modernui.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.tigereye.chestcavity.ChestCavity;

public record FlyingSwordWithdrawPayload(int index1) implements CustomPacketPayload {

  public static final CustomPacketPayload.Type<FlyingSwordWithdrawPayload> TYPE =
      new CustomPacketPayload.Type<>(ChestCavity.id("flyingsword_withdraw"));

  public static final StreamCodec<FriendlyByteBuf, FlyingSwordWithdrawPayload> STREAM_CODEC =
      StreamCodec.of(
          (buf, payload) -> buf.writeVarInt(payload.index1),
          buf -> new FlyingSwordWithdrawPayload(buf.readVarInt()));

  @Override
  public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }

  public static void handle(FlyingSwordWithdrawPayload payload, IPayloadContext context) {
    context.enqueueWork(
        () -> {
          if (context.player() instanceof net.minecraft.server.level.ServerPlayer sp) {
            net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ui
                    .FlyingSwordTUIOps
                .withdrawDisplayItem(sp.serverLevel(), sp, payload.index1);
          }
        });
  }
}

