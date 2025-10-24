package net.tigereye.chestcavity.client.modernui.container.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.client.modernui.container.TestModernUIContainerDebug;

public record TestModernUIContainerRequestPayload() implements CustomPacketPayload {

  public static final CustomPacketPayload.Type<TestModernUIContainerRequestPayload> TYPE =
      new CustomPacketPayload.Type<>(ChestCavity.id("testmodernui_container_request"));

  public static final StreamCodec<FriendlyByteBuf, TestModernUIContainerRequestPayload>
      STREAM_CODEC =
          StreamCodec.of(
              (buf, payload) -> {
                // no fields
              },
              buf -> new TestModernUIContainerRequestPayload());

  @Override
  public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }

  public static void handle(TestModernUIContainerRequestPayload payload, IPayloadContext context) {
    context.enqueueWork(
        () -> {
          if (context.player() instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            TestModernUIContainerDebug.openFor(serverPlayer);
          } else {
            ChestCavity.LOGGER.warn(
                "[ModernUI] Received container request without server player context");
          }
        });
  }
}
