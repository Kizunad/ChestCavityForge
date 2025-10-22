package net.tigereye.chestcavity.client.modernui.config.network;

import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.soul.engine.SoulFeatureToggle;
import net.tigereye.chestcavity.soul.fakeplayer.SoulFakePlayerSpawner;
import net.tigereye.chestcavity.soul.util.SoulLog;

public record SoulConfigActivatePayload(UUID soulId) implements CustomPacketPayload {

  public static final Type<SoulConfigActivatePayload> TYPE =
      new Type<>(ChestCavity.id("soul_config_activate"));

  public static final StreamCodec<FriendlyByteBuf, SoulConfigActivatePayload> STREAM_CODEC =
      StreamCodec.of(SoulConfigActivatePayload::write, SoulConfigActivatePayload::read);

  private static void write(FriendlyByteBuf buf, SoulConfigActivatePayload payload) {
    buf.writeUUID(payload.soulId);
  }

  private static SoulConfigActivatePayload read(FriendlyByteBuf buf) {
    return new SoulConfigActivatePayload(buf.readUUID());
  }

  @Override
  public Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }

  public static void handle(
      SoulConfigActivatePayload payload,
      net.neoforged.neoforge.network.handling.IPayloadContext context) {
    context.enqueueWork(
        () -> {
          if (!(context.player() instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) {
            return;
          }
          if (!SoulFeatureToggle.isEnabled()) {
            serverPlayer.displayClientMessage(
                net.minecraft.network.chat.Component.literal("[soul] 功能未启用，无法切换。"), false);
            return;
          }
          UUID target = payload.soulId();
          boolean success;
          try {
            success = SoulFakePlayerSpawner.switchTo(serverPlayer, target);
          } catch (Exception e) {
            SoulLog.error(
                "[soul] config-activate failed owner={} target={} ",
                e,
                serverPlayer.getUUID(),
                target);
            success = false;
          }
          if (!success) {
            serverPlayer.displayClientMessage(
                net.minecraft.network.chat.Component.literal("[soul] 未能切换到指定分魂。"), false);
          }
          // Send updated snapshot regardless to reflect actual state
          var entries = SoulConfigNetworkHelper.buildEntries(serverPlayer);
          serverPlayer.connection.send(new SoulConfigSyncPayload(entries));
        });
  }
}
