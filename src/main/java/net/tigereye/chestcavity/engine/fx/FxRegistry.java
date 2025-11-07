package net.tigereye.chestcavity.engine.fx;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FX 注册中心：实现"先注册、后处处可调用"的便捷模式。
 *
 * <p>Stage 3 核心类。
 *
 * <p>使用流程：
 *
 * <ol>
 *   <li>在模组初始化阶段调用 {@link #register(String, FxFactory)} 注册 FX 方案
 *   <li>在任意回调/任意上下文调用 {@link #play(String, FxContext)} 播放 FX
 *   <li>FxRegistry 自动创建 FxTrackSpec 并提交到 FxScheduler
 * </ol>
 *
 * <p>示例：
 *
 * <pre>{@code
 * // 注册阶段（模组初始化）
 * FxRegistry.getInstance().register("shockfield:ring", context -> {
 *   return FxTrackSpec.builder("shockfield-ring-" + UUID.randomUUID())
 *     .ttl(100)
 *     .tickInterval(2)
 *     .owner(context.getOwnerId())
 *     .mergeKey("shockfield:ring@" + context.getOwnerId())
 *     .onTick((level, elapsed) -> {
 *       // 环纹扩张逻辑
 *     })
 *     .build();
 * });
 *
 * // 播放阶段（任意回调）
 * FxContext context = FxContext.builder(level)
 *   .owner(player.getUUID())
 *   .position(player.position())
 *   .build();
 * FxRegistry.getInstance().play("shockfield:ring", context);
 * }</pre>
 */
public final class FxRegistry {

  private static final Logger LOGGER = LoggerFactory.getLogger(FxRegistry.class);

  /** 单例实例。 */
  private static final FxRegistry INSTANCE = new FxRegistry();

  /** 已注册的 FX 方案：fxId -> factory。 */
  private final Map<String, FxFactory> factories = new ConcurrentHashMap<>();

  private FxRegistry() {}

  public static FxRegistry getInstance() {
    return INSTANCE;
  }

  /**
   * 注册一个 FX 方案。
   *
   * @param fxId FX 唯一标识符（建议格式：namespace:fx/category/name）
   * @param factory FX 工厂函数
   * @return 是否成功注册（false 表示 ID 已存在）
   */
  public boolean register(String fxId, FxFactory factory) {
    if (fxId == null || fxId.isEmpty()) {
      throw new IllegalArgumentException("FX ID cannot be null or empty");
    }
    if (factory == null) {
      throw new IllegalArgumentException("FxFactory cannot be null");
    }

    FxFactory existing = factories.putIfAbsent(fxId, factory);
    if (existing != null) {
      LOGGER.warn("[FxRegistry] FX ID already registered: {}", fxId);
      return false;
    }

    LOGGER.debug("[FxRegistry] Registered FX: {}", fxId);
    return true;
  }

  /**
   * 注销一个 FX 方案。
   *
   * @param fxId FX 唯一标识符
   * @return 是否成功注销
   */
  public boolean unregister(String fxId) {
    if (fxId == null || fxId.isEmpty()) {
      return false;
    }

    FxFactory removed = factories.remove(fxId);
    if (removed != null) {
      LOGGER.debug("[FxRegistry] Unregistered FX: {}", fxId);
      return true;
    }

    return false;
  }

  /**
   * 播放一个 FX（根据上下文创建并提交 Track）。
   *
   * @param fxId FX 唯一标识符
   * @param context FX 上下文
   * @return Track ID，如果失败则返回 null
   */
  public String play(String fxId, FxContext context) {
    if (fxId == null || fxId.isEmpty()) {
      LOGGER.warn("[FxRegistry] FX ID is null or empty");
      return null;
    }
    if (context == null) {
      throw new IllegalArgumentException("FxContext cannot be null");
    }

    FxFactory factory = factories.get(fxId);
    if (factory == null) {
      LOGGER.warn("[FxRegistry] FX ID not registered: {}", fxId);
      return null;
    }

    try {
      // 调用工厂函数创建 Spec
      FxTrackSpec spec = factory.apply(context);
      if (spec == null) {
        LOGGER.error("[FxRegistry] Factory returned null spec for FX: {}", fxId);
        return null;
      }

      // 提交到 Scheduler
      String trackId = FxEngine.scheduler().schedule(spec);

      if (trackId != null) {
        LOGGER.debug("[FxRegistry] Played FX: {} -> {}", fxId, trackId);
      } else {
        LOGGER.debug("[FxRegistry] FX was merged/dropped: {}", fxId);
      }

      return trackId;

    } catch (Exception e) {
      LOGGER.error("[FxRegistry] Failed to play FX: {}", fxId, e);
      return null;
    }
  }

  /**
   * 检查 FX 是否已注册。
   *
   * @param fxId FX 唯一标识符
   * @return 是否已注册
   */
  public boolean isRegistered(String fxId) {
    return fxId != null && factories.containsKey(fxId);
  }

  /**
   * 获取所有已注册的 FX ID。
   *
   * @return FX ID 集合
   */
  public java.util.Set<String> getRegisteredIds() {
    return java.util.Collections.unmodifiableSet(factories.keySet());
  }

  /**
   * 清空所有已注册的 FX 方案。
   *
   * <p>警告：此方法会清空所有注册，谨慎使用（通常用于测试或重载）。
   */
  public void clear() {
    int count = factories.size();
    factories.clear();
    LOGGER.info("[FxRegistry] Cleared {} FX registrations", count);
  }
}
