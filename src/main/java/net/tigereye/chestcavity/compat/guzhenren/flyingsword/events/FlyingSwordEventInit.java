package net.tigereye.chestcavity.compat.guzhenren.flyingsword.events;

import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.events.impl.DefaultEventHooks;

/**
 * 飞剑事件系统初始化
 *
 * <p>在模组启动时注册默认钩子。
 */
public final class FlyingSwordEventInit {

  private static boolean initialized = false;

  private FlyingSwordEventInit() {}

  /**
   * 初始化事件系统
   *
   * <p>应该在模组初始化阶段调用一次。
   */
  public static void init() {
    if (initialized) {
      ChestCavity.LOGGER.warn("[FlyingSwordEvent] Already initialized, skipping");
      return;
    }

    ChestCavity.LOGGER.info("[FlyingSwordEvent] Initializing event system...");

    // 注册默认钩子
    FlyingSwordEventRegistry.register(new DefaultEventHooks());

    // 注册飞剑碰撞反推钩子
    FlyingSwordEventRegistry.register(
        new net.tigereye.chestcavity.compat.guzhenren.flyingsword.events.hooks
            .SwordClashHook());

    initialized = true;
    ChestCavity.LOGGER.info(
        "[FlyingSwordEvent] Event system initialized with {} hooks",
        FlyingSwordEventRegistry.getHooks().size());
  }
}
