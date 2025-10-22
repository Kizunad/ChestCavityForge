package net.tigereye.chestcavity.client.modernui.config.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.client.modernui.config.data.SoulConfigDataClient;

public record SoulConfigVacuumSyncPayload(boolean enabled, double radius)
    implements CustomPacketPayload {

  public static final Type<SoulConfigVacuumSyncPayload> TYPE =
      new Type<>(ChestCavity.id("soul_config_vacuum_sync"));

  public static final StreamCodec<FriendlyByteBuf, SoulConfigVacuumSyncPayload> STREAM_CODEC =
      StreamCodec.of(SoulConfigVacuumSyncPayload::write, SoulConfigVacuumSyncPayload::read);

  private static void write(FriendlyByteBuf buf, SoulConfigVacuumSyncPayload payload) {
    buf.writeBoolean(payload.enabled);
    buf.writeDouble(payload.radius);
  }

  private static SoulConfigVacuumSyncPayload read(FriendlyByteBuf buf) {
    boolean e = buf.readBoolean();
    double r = buf.readDouble();
    return new SoulConfigVacuumSyncPayload(e, r);
  }

  @Override
  public Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }

  public static void handle(
      SoulConfigVacuumSyncPayload payload,
      net.neoforged.neoforge.network.handling.IPayloadContext context) {
    context.enqueueWork(
        () -> {
          SoulConfigDataClient.INSTANCE.updateVacuum(
              new SoulConfigDataClient.VacuumTuning(payload.enabled(), payload.radius()));
        });
  }
}
