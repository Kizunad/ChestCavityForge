package net.tigereye.chestcavity.client.modernui.config.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.tigereye.chestcavity.ChestCavity;

public record SoulConfigSetVacuumPayload(boolean enabled, double radius)
    implements CustomPacketPayload {

  public static final Type<SoulConfigSetVacuumPayload> TYPE =
      new Type<>(ChestCavity.id("soul_config_set_vacuum"));

  public static final StreamCodec<FriendlyByteBuf, SoulConfigSetVacuumPayload> STREAM_CODEC =
      StreamCodec.of(SoulConfigSetVacuumPayload::write, SoulConfigSetVacuumPayload::read);

  private static void write(FriendlyByteBuf buf, SoulConfigSetVacuumPayload payload) {
    buf.writeBoolean(payload.enabled);
    buf.writeDouble(payload.radius);
  }

  private static SoulConfigSetVacuumPayload read(FriendlyByteBuf buf) {
    boolean enabled = buf.readBoolean();
    double radius = buf.readDouble();
    return new SoulConfigSetVacuumPayload(enabled, radius);
  }

  @Override
  public Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }

  public static void handle(
      SoulConfigSetVacuumPayload payload,
      net.neoforged.neoforge.network.handling.IPayloadContext context) {
    context.enqueueWork(
        () -> {
          if (!(context.player() instanceof net.minecraft.server.level.ServerPlayer sp)) return;
          // Server-side toggle of global vacuum behaviour
          net.tigereye.chestcavity.soul.runtime.ItemVacuumHandler.setEnabled(payload.enabled());
          net.tigereye.chestcavity.soul.runtime.ItemVacuumHandler.setRadius(payload.radius());
          // 回发一次同步，确保 UI 立即反映服务器最终值
          sp.connection.send(
              new net.tigereye.chestcavity.client.modernui.config.network
                  .SoulConfigVacuumSyncPayload(
                  net.tigereye.chestcavity.soul.runtime.ItemVacuumHandler.isEnabled(),
                  net.tigereye.chestcavity.soul.runtime.ItemVacuumHandler.getRadius()));
        });
  }
}
