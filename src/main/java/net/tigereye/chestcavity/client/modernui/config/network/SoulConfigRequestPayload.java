package net.tigereye.chestcavity.client.modernui.config.network;

import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.client.modernui.config.network.SoulConfigSyncPayload.Entry;

public record SoulConfigRequestPayload() implements CustomPacketPayload {

  public static final Type<SoulConfigRequestPayload> TYPE =
      new Type<>(ChestCavity.id("soul_config_request"));

  public static final StreamCodec<FriendlyByteBuf, SoulConfigRequestPayload> STREAM_CODEC =
      StreamCodec.of((buf, payload) -> {}, buf -> new SoulConfigRequestPayload());

  @Override
  public Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }

  public static void handle(SoulConfigRequestPayload payload, IPayloadContext context) {
    context.enqueueWork(
        () -> {
          if (!(context.player() instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) {
            return;
          }
          // 先同步全局调优到客户端，保证 UI 初始值一致
          boolean vacEnabled = net.tigereye.chestcavity.soul.runtime.ItemVacuumHandler.isEnabled();
          double vacRadius = net.tigereye.chestcavity.soul.runtime.ItemVacuumHandler.getRadius();
          serverPlayer.connection.send(
              new net.tigereye.chestcavity.client.modernui.config.network
                  .SoulConfigVacuumSyncPayload(vacEnabled, vacRadius));
          // 跟随/传送
          boolean tp =
              net.tigereye.chestcavity.soul.util.SoulFollowTeleportTuning.teleportEnabled();
          double follow =
              net.tigereye.chestcavity.soul.util.SoulFollowTeleportTuning.followTriggerDist();
          double tpDist =
              net.tigereye.chestcavity.soul.util.SoulFollowTeleportTuning.teleportDist();
          serverPlayer.connection.send(
              new net.tigereye.chestcavity.client.modernui.config.network
                  .SoulConfigFollowTeleportSyncPayload(tp, follow, tpDist));
          // 最后同步分魂列表，触发 UI 重绘
          List<Entry> entries = SoulConfigNetworkHelper.buildEntries(serverPlayer);
          serverPlayer.connection.send(new SoulConfigSyncPayload(entries));
        });
  }
}
