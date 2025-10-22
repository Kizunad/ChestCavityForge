package net.tigereye.chestcavity.guscript.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.guscript.ui.GuScriptMenu;

public record GuScriptOpenPayload() implements CustomPacketPayload {
  public static final CustomPacketPayload.Type<GuScriptOpenPayload> TYPE =
      new CustomPacketPayload.Type<>(ChestCavity.id("guscript_open"));

  public static final StreamCodec<FriendlyByteBuf, GuScriptOpenPayload> STREAM_CODEC =
      StreamCodec.of(
          (buf, payload) -> {
            // no state to write
          },
          buf -> new GuScriptOpenPayload());

  @Override
  public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }

  public static void handle(GuScriptOpenPayload payload, IPayloadContext context) {
    context.enqueueWork(
        () -> {
          if (!(context.player() instanceof ServerPlayer player)) {
            ChestCavity.LOGGER.warn(
                "[GuScript] Received open payload without server player context");
            return;
          }
          ChestCavity.LOGGER.info(
              "[GuScript] {} requested GuScript UI", player.getGameProfile().getName());
          player.openMenu(
              new SimpleMenuProvider(
                  (syncId, inventory, p) -> new GuScriptMenu(syncId, inventory),
                  Component.translatable("gui.chestcavity.guscript")));
        });
  }
}
