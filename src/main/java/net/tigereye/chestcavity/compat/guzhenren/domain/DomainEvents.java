package net.tigereye.chestcavity.compat.guzhenren.domain;

import net.minecraft.server.level.ServerLevel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Server tick dispatcher for the Domain system. */
public final class DomainEvents {

  private DomainEvents() {}

  @SubscribeEvent
  public static void onServerTick(ServerTickEvent.Post event) {
    for (ServerLevel level : event.getServer().getAllLevels()) {
      // 更新域系统
      DomainManager.getInstance().tick(level);
      // 衰减玩家上的"无视打断/剑道冻结"标签（如有）
      for (var player : level.players()) {
        net.tigereye.chestcavity.compat.guzhenren.domain.DomainTags.tickUnbreakableFocus(player);
        net.tigereye.chestcavity.compat.guzhenren.domain.DomainTags.tickJiandaoFrozen(player);
        // 确保"冥想减速"与属性保持一致
        net.tigereye.chestcavity.compat.guzhenren.domain.DomainMovementOps
            .tickEnsureMeditationSlow(player);
        // 计算并广播通用"剑域"系数（独立于具体领域）
        net.tigereye.chestcavity.compat.guzhenren.domain.SwordDomainOps.tickPlayer(
            player, level.getGameTime());
      }
    }
  }

  /**
   * 玩家登出时清理该玩家的所有领域（服务端）
   */
  @SubscribeEvent
  public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
    var player = event.getEntity();
    if (player != null) {
      int removed = DomainManager.getInstance().removeAllDomainsByOwner(player.getUUID());
      if (removed > 0) {
        net.tigereye.chestcavity.ChestCavity.LOGGER.info(
            "[DomainEvents] Cleaned up {} domains for player {} on logout",
            removed,
            player.getName().getString());
      }
    }
  }

  /**
   * 客户端世界卸载时清理所有领域渲染数据（客户端）
   */
  @OnlyIn(Dist.CLIENT)
  @SubscribeEvent
  public static void onClientLevelUnload(LevelEvent.Unload event) {
    if (event.getLevel().isClientSide()) {
      net.tigereye.chestcavity.compat.guzhenren.domain.client.DomainRenderer.clearAll();
      net.tigereye.chestcavity.ChestCavity.LOGGER.info(
          "[DomainEvents] Cleared client domain render cache on level unload");
    }
  }
}
