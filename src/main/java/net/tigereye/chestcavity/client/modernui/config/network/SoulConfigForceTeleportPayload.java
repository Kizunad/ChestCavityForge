package net.tigereye.chestcavity.client.modernui.config.network;

import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.tigereye.chestcavity.ChestCavity;

public record SoulConfigForceTeleportPayload(UUID soulId) implements CustomPacketPayload {

  public static final Type<SoulConfigForceTeleportPayload> TYPE =
      new Type<>(ChestCavity.id("soul_config_force_teleport"));

  public static final StreamCodec<FriendlyByteBuf, SoulConfigForceTeleportPayload> STREAM_CODEC =
      StreamCodec.of(SoulConfigForceTeleportPayload::write, SoulConfigForceTeleportPayload::read);

  private static void write(FriendlyByteBuf buf, SoulConfigForceTeleportPayload payload) {
    buf.writeUUID(payload.soulId);
  }

  private static SoulConfigForceTeleportPayload read(FriendlyByteBuf buf) {
    UUID id = buf.readUUID();
    return new SoulConfigForceTeleportPayload(id);
  }

  @Override
  public Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }

  public static void handle(
      SoulConfigForceTeleportPayload payload,
      net.neoforged.neoforge.network.handling.IPayloadContext context) {
    context.enqueueWork(
        () -> {
          if (!(context.player() instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) {
            return;
          }
          UUID soulId = payload.soulId();
          if (soulId == null) {
            return;
          }
          net.tigereye.chestcavity.soul.fakeplayer.SoulFakePlayerSpawner.forceTeleportToOwner(
              serverPlayer, soulId);
        });
  }
}
