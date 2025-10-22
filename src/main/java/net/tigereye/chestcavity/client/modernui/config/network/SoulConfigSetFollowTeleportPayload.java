package net.tigereye.chestcavity.client.modernui.config.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.tigereye.chestcavity.ChestCavity;

public record SoulConfigSetFollowTeleportPayload(
    boolean teleportEnabled, double followDist, double teleportDist)
    implements CustomPacketPayload {

  public static final Type<SoulConfigSetFollowTeleportPayload> TYPE =
      new Type<>(ChestCavity.id("soul_config_set_follow_tp"));

  public static final StreamCodec<FriendlyByteBuf, SoulConfigSetFollowTeleportPayload>
      STREAM_CODEC =
          StreamCodec.of(
              SoulConfigSetFollowTeleportPayload::write, SoulConfigSetFollowTeleportPayload::read);

  private static void write(FriendlyByteBuf buf, SoulConfigSetFollowTeleportPayload payload) {
    buf.writeBoolean(payload.teleportEnabled);
    buf.writeDouble(payload.followDist);
    buf.writeDouble(payload.teleportDist);
  }

  private static SoulConfigSetFollowTeleportPayload read(FriendlyByteBuf buf) {
    boolean tp = buf.readBoolean();
    double follow = buf.readDouble();
    double tpDist = buf.readDouble();
    return new SoulConfigSetFollowTeleportPayload(tp, follow, tpDist);
  }

  @Override
  public Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }

  public static void handle(
      SoulConfigSetFollowTeleportPayload payload,
      net.neoforged.neoforge.network.handling.IPayloadContext context) {
    context.enqueueWork(
        () -> {
          // Server-side apply runtime tuning (clamped by setters)
          net.tigereye.chestcavity.soul.util.SoulFollowTeleportTuning.setTeleportEnabled(
              payload.teleportEnabled());
          net.tigereye.chestcavity.soul.util.SoulFollowTeleportTuning.setFollowTriggerDist(
              payload.followDist());
          net.tigereye.chestcavity.soul.util.SoulFollowTeleportTuning.setTeleportDist(
              payload.teleportDist());
        });
  }
}
