package net.tigereye.chestcavity.guzhenren.resource;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Guzhenren 资源同步相关的玩家事件处理。
 *
 * <p>负责在玩家登录、重生、复制（死亡/回档）、跨维度时，触发一次延迟资源同步，
 * 保证客户端与服务端关于真元/精力/魂魄等状态的一致性。</p>
 */
public final class GuzhenrenResourceEvents {

  private GuzhenrenResourceEvents() {}

  /**
   * 玩家登录事件：请求一次延迟资源同步。
   *
   * @param event 玩家登录事件
   */
  public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
    if (event.getEntity() instanceof ServerPlayer player) {
      GuzhenrenResourceBridge.requestDeferredSync(player);
    }
  }

  /**
   * 玩家重生事件：请求一次延迟资源同步。
   *
   * @param event 玩家重生事件
   */
  public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
    if (event.getEntity() instanceof ServerPlayer player) {
      GuzhenrenResourceBridge.requestDeferredSync(player);
    }
  }

  /**
   * 玩家克隆事件（死亡后重建，或其他复制流程）：请求一次延迟资源同步。
   *
   * @param event 玩家克隆事件
   */
  public static void onPlayerClone(PlayerEvent.Clone event) {
    if (event.getEntity() instanceof ServerPlayer player) {
      GuzhenrenResourceBridge.requestDeferredSync(player);
    }
  }

  /**
   * 玩家跨维度事件：请求一次延迟资源同步。
   *
   * @param event 玩家跨维度事件
   */
  public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
    if (event.getEntity() instanceof ServerPlayer player) {
      GuzhenrenResourceBridge.requestDeferredSync(player);
    }
  }
}
