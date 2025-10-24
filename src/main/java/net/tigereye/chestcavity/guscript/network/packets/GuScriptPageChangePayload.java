package net.tigereye.chestcavity.guscript.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.guscript.data.GuScriptAttachment;
import net.tigereye.chestcavity.registration.CCAttachments;

public record GuScriptPageChangePayload(Operation operation, int value)
    implements CustomPacketPayload {

  public static final Type<GuScriptPageChangePayload> TYPE =
      new Type<>(ChestCavity.id("guscript_page_change"));

  public static final StreamCodec<FriendlyByteBuf, GuScriptPageChangePayload> STREAM_CODEC =
      StreamCodec.of(
          (buf, payload) -> {
            buf.writeEnum(payload.operation);
            buf.writeVarInt(payload.value);
          },
          buf -> new GuScriptPageChangePayload(buf.readEnum(Operation.class), buf.readVarInt()));

  @Override
  public Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }

  public static void handle(GuScriptPageChangePayload payload, IPayloadContext context) {
    context.enqueueWork(
        () -> {
          if (!(context.player() instanceof ServerPlayer player)) {
            return;
          }
          GuScriptAttachment attachment = CCAttachments.getGuScript(player);
          switch (payload.operation) {
            case SET -> attachment.setCurrentPage(payload.value);
            case ADD -> {
              attachment.addPage();
              attachment.setCurrentPage(attachment.getPageCount() - 1);
            }
          }
          attachment.setChanged();
          if (player.containerMenu
              instanceof net.tigereye.chestcavity.guscript.ui.GuScriptMenu menu) {
            menu.slotsChanged(attachment);
            menu.syncFromAttachment(attachment);
          }
        });
  }

  public enum Operation {
    SET,
    ADD
  }
}
