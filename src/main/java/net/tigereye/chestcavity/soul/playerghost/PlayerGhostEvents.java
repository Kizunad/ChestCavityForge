package net.tigereye.chestcavity.soul.playerghost;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.tigereye.chestcavity.ChestCavity;

/**
 * 玩家死亡事件处理器
 *
 * <p>职责：
 * - 监听玩家死亡事件
 * - 捕获玩家死亡快照并保存到世界存档
 */
public final class PlayerGhostEvents {

  private PlayerGhostEvents() {}

  /**
   * 玩家死亡时捕获快照
   *
   * @param event 死亡事件
   */
  public static void onPlayerDeath(LivingDeathEvent event) {
    // 只在服务端处理
    if (event.getEntity().level().isClientSide()) {
      return;
    }

    // 只处理玩家死亡
    if (!(event.getEntity() instanceof ServerPlayer player)) {
      return;
    }

    // 只在非旁观模式下处理
    if (player.isSpectator()) {
      return;
    }

    try {
      // 捕获玩家死亡快照
      PlayerGhostArchive archive = PlayerGhostArchive.capture(player);

      // 保存到世界存档
      ServerLevel level = (ServerLevel) player.level();
      PlayerGhostWorldData data = PlayerGhostWorldData.get(level.getServer());
      data.add(archive);

      ChestCavity.LOGGER.info(
          "[PlayerGhost] 已记录玩家死亡: {} (UUID: {}) 于坐标 ({}, {}, {}) 维度 {}",
          archive.getPlayerName(),
          archive.getPlayerId(),
          String.format("%.2f", archive.getX()),
          String.format("%.2f", archive.getY()),
          String.format("%.2f", archive.getZ()),
          archive.getDimension().location());

    } catch (Exception e) {
      ChestCavity.LOGGER.warn(
          "[PlayerGhost] 记录玩家死亡时出错 - 玩家: {} (UUID: {})",
          player.getGameProfile().getName(),
          player.getUUID(),
          e);
    }
  }
}
