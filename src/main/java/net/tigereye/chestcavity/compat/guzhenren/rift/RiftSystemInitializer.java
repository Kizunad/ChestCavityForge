package net.tigereye.chestcavity.compat.guzhenren.rift;

import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.events.FlyingSwordEventRegistry;

/**
 * 裂剑蛊系统初始化器
 *
 * <p>负责在游戏启动时注册所有裂隙相关的组件：
 * <ul>
 *   <li>飞剑事件钩子</li>
 *   <li>剑气技能监听器</li>
 *   <li>领域协同系统</li>
 * </ul>
 *
 * <p>使用方式：在mod初始化阶段调用 {@link #initialize()}
 */
public final class RiftSystemInitializer {

  private static boolean initialized = false;

  private RiftSystemInitializer() {}

  /**
   * 初始化裂剑蛊系统
   *
   * <p>应该在服务端启动时调用一次
   */
  public static synchronized void initialize() {
    if (initialized) {
      ChestCavity.LOGGER.warn("[RiftSystem] Already initialized, skipping");
      return;
    }

    ChestCavity.LOGGER.info("[RiftSystem] Initializing Rift System...");

    // 1. 注册飞剑事件钩子
    FlyingSwordEventRegistry.register(FlyingSwordRiftHook.getInstance());
    ChestCavity.LOGGER.info("[RiftSystem] Registered FlyingSwordRiftHook");

    // 2. 剑气技能过滤由 ActivationHookRegistry 的正则表达式自动处理
    // 不需要手动注册技能ID

    // 3. 其他初始化...

    initialized = true;
    ChestCavity.LOGGER.info("[RiftSystem] Rift System initialized successfully");
  }

  /**
   * 检查系统是否已初始化
   */
  public static boolean isInitialized() {
    return initialized;
  }

  /**
   * 重置初始化状态（仅用于测试）
   */
  public static synchronized void reset() {
    if (!initialized) {
      return;
    }

    FlyingSwordEventRegistry.unregister(FlyingSwordRiftHook.getInstance());
    initialized = false;
    ChestCavity.LOGGER.info("[RiftSystem] Reset completed");
  }
}
