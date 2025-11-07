package net.tigereye.chestcavity.engine.fx;

import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.tigereye.chestcavity.ChestCavity;

/**
 * FxEngine 事件监听器。
 *
 * <p>Stage 4：监听服务器停服事件，统一清理所有活跃 Track。
 */
@EventBusSubscriber(modid = ChestCavity.MODID)
public final class FxEngineEvents {

  private FxEngineEvents() {}

  /**
   * 服务器停止：统一收尾所有活跃 Track，触发 onStop(ENGINE_SHUTDOWN)。
   *
   * @param event ServerStoppingEvent
   */
  @SubscribeEvent
  public static void onServerStopping(ServerStoppingEvent event) {
    var server = event.getServer();
    if (server == null) {
      return;
    }

    // 使用主世界作为 Level 参数（Track 实现需要处理 level 为 null 的情况）
    ServerLevel level = server.overworld();
    FxTimelineEngine.getInstance().shutdown(level);
  }
}
