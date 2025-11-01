package net.tigereye.chestcavity.compat.guzhenren.domain.network;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 领域网络处理工具
 *
 * <p>用于发送领域同步包到客户端
 */
public final class DomainNetworkHandler {

  private DomainNetworkHandler() {}

  /** 同步范围（方块） */
  private static final double SYNC_RANGE = 128.0;

  /**
   * 向附近玩家发送领域同步
   *
   * @param payload 同步包
   * @param center 领域中心
   * @param level 世界
   */
  public static void sendDomainSync(
      DomainSyncPayload payload, Vec3 center, ServerLevel level) {
    // 发送给范围内的所有玩家
    for (ServerPlayer player : level.players()) {
      if (player.position().distanceToSqr(center) <= SYNC_RANGE * SYNC_RANGE) {
        PacketDistributor.sendToPlayer(player, payload);
      }
    }
  }

  /**
   * 向附近玩家发送领域移除
   *
   * @param payload 移除包
   * @param center 领域中心
   * @param level 世界
   */
  public static void sendDomainRemove(
      DomainRemovePayload payload, Vec3 center, ServerLevel level) {
    // 发送给范围内的所有玩家
    for (ServerPlayer player : level.players()) {
      if (player.position().distanceToSqr(center) <= SYNC_RANGE * SYNC_RANGE) {
        PacketDistributor.sendToPlayer(player, payload);
      }
    }
  }
}
