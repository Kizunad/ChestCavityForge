package net.tigereye.chestcavity.compat.guzhenren.flyingsword.events;

import java.util.ArrayList;
import java.util.List;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.events.context.*;

/**
 * 飞剑事件钩子注册表
 *
 * <p>线程安全的钩子管理器，支持动态注册和注销。
 *
 * <p>使用示例：
 * <pre>{@code
 * // 注册钩子
 * FlyingSwordEventRegistry.register(new MyCustomHook());
 *
 * // 注销钩子
 * FlyingSwordEventRegistry.unregister(myHook);
 * }</pre>
 */
public final class FlyingSwordEventRegistry {

  private static final List<FlyingSwordEventHook> hooks = new ArrayList<>();
  private static final Object lock = new Object();

  private FlyingSwordEventRegistry() {}

  /**
   * 注册事件钩子
   *
   * @param hook 钩子实例
   * @return 是否成功注册（如果已存在则返回false）
   */
  public static boolean register(FlyingSwordEventHook hook) {
    if (hook == null) {
      ChestCavity.LOGGER.warn("[FlyingSwordEvent] Attempted to register null hook");
      return false;
    }

    synchronized (lock) {
      if (hooks.contains(hook)) {
        ChestCavity.LOGGER.warn(
            "[FlyingSwordEvent] Hook already registered: {}", hook.getClass().getName());
        return false;
      }
      hooks.add(hook);
      ChestCavity.LOGGER.info("[FlyingSwordEvent] Registered hook: {}", hook.getClass().getName());
      return true;
    }
  }

  /**
   * 注销事件钩子
   *
   * @param hook 钩子实例
   * @return 是否成功注销
   */
  public static boolean unregister(FlyingSwordEventHook hook) {
    synchronized (lock) {
      boolean removed = hooks.remove(hook);
      if (removed) {
        ChestCavity.LOGGER.info(
            "[FlyingSwordEvent] Unregistered hook: {}", hook.getClass().getName());
      }
      return removed;
    }
  }

  /**
   * 获取所有已注册的钩子（只读副本）
   */
  public static List<FlyingSwordEventHook> getHooks() {
    synchronized (lock) {
      return new ArrayList<>(hooks);
    }
  }

  /**
   * 清空所有钩子（谨慎使用）
   */
  public static void clear() {
    synchronized (lock) {
      hooks.clear();
      ChestCavity.LOGGER.info("[FlyingSwordEvent] Cleared all hooks");
    }
  }

  // ========== 事件触发方法 ==========

  public static void fireSpawn(SpawnContext ctx) {
    for (FlyingSwordEventHook hook : getHooks()) {
      try {
        hook.onSpawn(ctx);
        if (ctx.cancelled) break;
      } catch (Exception e) {
        ChestCavity.LOGGER.error(
            "[FlyingSwordEvent] Error in onSpawn hook: {}", hook.getClass().getName(), e);
      }
    }
  }

  public static void fireTick(TickContext ctx) {
    for (FlyingSwordEventHook hook : getHooks()) {
      try {
        hook.onTick(ctx);
      } catch (Exception e) {
        ChestCavity.LOGGER.error(
            "[FlyingSwordEvent] Error in onTick hook: {}", hook.getClass().getName(), e);
      }
    }
  }

  public static void fireHitEntity(HitEntityContext ctx) {
    for (FlyingSwordEventHook hook : getHooks()) {
      try {
        hook.onHitEntity(ctx);
        if (ctx.cancelled) break;
      } catch (Exception e) {
        ChestCavity.LOGGER.error(
            "[FlyingSwordEvent] Error in onHitEntity hook: {}", hook.getClass().getName(), e);
      }
    }
  }

  public static void fireBlockBreak(BlockBreakContext ctx) {
    for (FlyingSwordEventHook hook : getHooks()) {
      try {
        hook.onBlockBreak(ctx);
      } catch (Exception e) {
        ChestCavity.LOGGER.error(
            "[FlyingSwordEvent] Error in onBlockBreak hook: {}", hook.getClass().getName(), e);
      }
    }
  }

  public static void fireHurt(HurtContext ctx) {
    for (FlyingSwordEventHook hook : getHooks()) {
      try {
        hook.onHurt(ctx);
        if (ctx.cancelled) break;
      } catch (Exception e) {
        ChestCavity.LOGGER.error(
            "[FlyingSwordEvent] Error in onHurt hook: {}", hook.getClass().getName(), e);
      }
    }
  }

  public static void fireInteract(InteractContext ctx) {
    for (FlyingSwordEventHook hook : getHooks()) {
      try {
        hook.onInteract(ctx);
        if (ctx.cancelDefault) break;
      } catch (Exception e) {
        ChestCavity.LOGGER.error(
            "[FlyingSwordEvent] Error in onInteract hook: {}", hook.getClass().getName(), e);
      }
    }
  }

  public static void fireDespawnOrRecall(DespawnContext ctx) {
    for (FlyingSwordEventHook hook : getHooks()) {
      try {
        hook.onDespawnOrRecall(ctx);
        if (ctx.preventDespawn) break;
      } catch (Exception e) {
        ChestCavity.LOGGER.error(
            "[FlyingSwordEvent] Error in onDespawnOrRecall hook: {}",
            hook.getClass().getName(),
            e);
      }
    }
  }
}
