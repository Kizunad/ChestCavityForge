package net.tigereye.chestcavity.engine.fx;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.tigereye.chestcavity.engine.ServerTickEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FX 时间线引擎：负责维护活跃的 {@link FxTrack}，处理生命周期回调、TTL 管理、预算控制与合并策略。
 *
 * <p>Stage 2 扩展：
 *
 * <ul>
 *   <li>预算控制：per-level / per-owner 上限（可配置）
 *   <li>合并策略：extendTtl / drop / replace（根据 mergeKey）
 *   <li>索引管理：per-owner 和 per-mergeKey 索引
 * </ul>
 */
public final class FxTimelineEngine implements ServerTickEngine {

  private static final Logger LOGGER = LoggerFactory.getLogger(FxTimelineEngine.class);

  /** 单例实例。 */
  private static final FxTimelineEngine INSTANCE = new FxTimelineEngine();

  /** 活跃 Track 表：trackId -> TrackContext。 */
  private final Map<String, TrackContext> activeTracks = new ConcurrentHashMap<>();

  /** per-owner 索引：ownerId -> Set<trackId>。 */
  private final Map<UUID, Set<String>> ownerIndex = new ConcurrentHashMap<>();

  /** per-mergeKey 索引：mergeKey -> trackId。 */
  private final Map<String, String> mergeKeyIndex = new ConcurrentHashMap<>();

  // 统计计数器
  private int mergeCount = 0;
  private int dropCount = 0;
  private int replaceCount = 0;

  private FxTimelineEngine() {}

  public static FxTimelineEngine getInstance() {
    return INSTANCE;
  }

  /**
   * 注册一个新的 Track（支持预算控制与合并策略）。
   *
   * @param track FX Track 实例
   * @param spec FxTrackSpec（可选，用于合并策略）
   * @return Track ID，如果被合并/丢弃则返回 null
   */
  public String register(FxTrack track, FxTrackSpec spec) {
    if (track == null) {
      throw new IllegalArgumentException("FxTrack cannot be null");
    }
    String id = track.getId();
    if (id == null || id.isEmpty()) {
      throw new IllegalArgumentException("FxTrack ID cannot be null or empty");
    }

    FxEngineConfig config = FxEngine.getConfig();

    // 如果引擎未启用，静默丢弃
    if (!config.enabled) {
      LOGGER.debug("[FxEngine] Engine disabled, dropping track: {}", id);
      dropCount++;
      return null;
    }

    // 合并策略处理（如果有 mergeKey）
    String mergeKey = spec != null ? spec.getMergeKey() : null;
    if (mergeKey != null && !mergeKey.isEmpty()) {
      String existingTrackId = mergeKeyIndex.get(mergeKey);
      if (existingTrackId != null) {
        return handleMerge(existingTrackId, track, spec, config);
      }
    }

    // 预算控制检查
    if (config.budgetEnabled) {
      // per-level 上限检查
      if (activeTracks.size() >= config.perLevelCap) {
        LOGGER.warn(
            "[FxEngine] Per-level cap reached ({}), dropping track: {}", config.perLevelCap, id);
        dropCount++;
        return null;
      }

      // per-owner 上限检查
      UUID ownerId = track.getOwnerId();
      if (ownerId != null) {
        Set<String> ownerTracks = ownerIndex.get(ownerId);
        if (ownerTracks != null && ownerTracks.size() >= config.perOwnerCap) {
          LOGGER.warn(
              "[FxEngine] Per-owner cap reached ({}) for owner {}, dropping track: {}",
              config.perOwnerCap,
              ownerId,
              id);
          dropCount++;
          return null;
        }
      }
    }

    // 检查 ID 冲突
    if (activeTracks.containsKey(id)) {
      LOGGER.warn("[FxEngine] Track {} already exists, replacing", id);
      unregisterInternal(id, null, StopReason.CANCELLED);
    }

    // 注册 Track
    TrackContext ctx = new TrackContext(track, spec);
    activeTracks.put(id, ctx);

    // 更新 per-owner 索引
    UUID ownerId = track.getOwnerId();
    if (ownerId != null) {
      ownerIndex.computeIfAbsent(ownerId, k -> ConcurrentHashMap.newKeySet()).add(id);
    }

    // 更新 per-mergeKey 索引
    if (mergeKey != null && !mergeKey.isEmpty()) {
      mergeKeyIndex.put(mergeKey, id);
    }

    LOGGER.debug("[FxEngine] Registered track: {}", id);
    return id;
  }

  /**
   * 注册一个新的 Track（简化版，无 Spec）。
   *
   * @param track FX Track 实例
   * @return Track ID
   */
  public String register(FxTrack track) {
    return register(track, null);
  }

  /**
   * 处理合并策略。
   *
   * @param existingTrackId 已存在的 Track ID
   * @param newTrack 新的 Track
   * @param newSpec 新的 Spec
   * @param config 配置
   * @return Track ID（如果被丢弃则返回 null）
   */
  private String handleMerge(
      String existingTrackId, FxTrack newTrack, FxTrackSpec newSpec, FxEngineConfig config) {
    MergeStrategy strategy =
        newSpec != null && newSpec.getMergeStrategy() != null
            ? newSpec.getMergeStrategy()
            : config.defaultMergeStrategy;

    switch (strategy) {
      case EXTEND_TTL:
        // 延长现有 Track 的 TTL
        TrackContext existing = activeTracks.get(existingTrackId);
        if (existing != null) {
          int additionalTicks = newTrack.getTtlTicks();
          existing.extendTtl(additionalTicks);
          LOGGER.debug(
              "[FxEngine] Extended TTL for track {} by {} ticks",
              existingTrackId,
              additionalTicks);
          mergeCount++;
          return existingTrackId;
        }
        break;

      case DROP:
        // 丢弃新的 Track 请求
        LOGGER.debug("[FxEngine] Dropping new track due to merge policy: {}", newTrack.getId());
        dropCount++;
        return null;

      case REPLACE:
        // 替换现有 Track
        LOGGER.debug("[FxEngine] Replacing existing track: {}", existingTrackId);
        unregisterInternal(existingTrackId, null, StopReason.CANCELLED);
        replaceCount++;
        // 继续注册新 Track（通过返回 null 让调用方重试）
        String mergeKey = newSpec != null ? newSpec.getMergeKey() : null;
        if (mergeKey != null) {
          mergeKeyIndex.remove(mergeKey);
        }
        return register(newTrack, newSpec);

      default:
        LOGGER.warn("[FxEngine] Unknown merge strategy: {}", strategy);
        break;
    }

    return null;
  }

  /**
   * 取消一个 Track（手动停止）。
   *
   * @param trackId Track ID
   * @param level 服务器世界（用于触发 onStop 回调）
   * @return 是否成功取消
   */
  public boolean cancel(String trackId, ServerLevel level) {
    return unregisterInternal(trackId, level, StopReason.CANCELLED);
  }

  /**
   * 内部注销方法：移除 Track 并清理索引。
   *
   * @param trackId Track ID
   * @param level 服务器世界（可为 null，在非 tick 上下文中可能为 null）
   * @param reason 停止原因
   * @return 是否成功注销
   */
  private boolean unregisterInternal(String trackId, ServerLevel level, StopReason reason) {
    TrackContext ctx = activeTracks.remove(trackId);
    if (ctx == null) {
      return false;
    }

    FxTrack track = ctx.track;

    // 清理 per-owner 索引
    UUID ownerId = track.getOwnerId();
    if (ownerId != null) {
      Set<String> ownerTracks = ownerIndex.get(ownerId);
      if (ownerTracks != null) {
        ownerTracks.remove(trackId);
        if (ownerTracks.isEmpty()) {
          ownerIndex.remove(ownerId);
        }
      }
    }

    // 清理 per-mergeKey 索引
    if (ctx.spec != null && ctx.spec.getMergeKey() != null) {
      mergeKeyIndex.remove(ctx.spec.getMergeKey());
    }

    // 触发 onStop 回调（即使 level 为 null 也要调用）
    // Track 实现需要处理 level 为 null 的情况（在替换/冲突场景下）
    safeOnStop(track, level, reason);

    LOGGER.debug("[FxEngine] Unregistered track: {} (reason: {})", trackId, reason);
    return true;
  }

  /**
   * 查找活跃的 Track。
   *
   * @param trackId Track ID
   * @return Track 实例，如果不存在则返回 null
   */
  public FxTrack find(String trackId) {
    TrackContext ctx = activeTracks.get(trackId);
    return ctx != null ? ctx.track : null;
  }

  /** 获取当前活跃 Track 数量。 */
  public int getActiveCount() {
    return activeTracks.size();
  }

  /** 获取合并计数。 */
  public int getMergeCount() {
    return mergeCount;
  }

  /** 获取丢弃计数。 */
  public int getDropCount() {
    return dropCount;
  }

  /** 获取替换计数。 */
  public int getReplaceCount() {
    return replaceCount;
  }

  @Override
  public void onServerTick(ServerTickEvent.Post event) {
    if (activeTracks.isEmpty()) {
      return;
    }

    var server = event.getServer();
    if (server == null) {
      return;
    }

    // Stage 1: 使用主世界（overworld）作为默认 Level
    // TODO Stage 3: 支持 Track 关联到特定 Level
    ServerLevel defaultLevel = server.overworld();
    if (defaultLevel == null) {
      return;
    }

    List<String> toRemove = new ArrayList<>();

    for (Map.Entry<String, TrackContext> entry : activeTracks.entrySet()) {
      String trackId = entry.getKey();
      TrackContext ctx = entry.getValue();
      FxTrack track = ctx.track;

      try {
        processTrack(defaultLevel, trackId, ctx, track, toRemove);
      } catch (Throwable t) {
        // 外层异常捕获：processTrack 本身抛出的异常（理论上不应发生）
        LOGGER.error("[FxEngine] Unexpected error processing track {}", trackId, t);
        // 直接调用 unregisterInternal，避免重复调用 onStop
        unregisterInternal(trackId, defaultLevel, StopReason.EXCEPTION);
      }
    }

    // 移除已停止的 Track（TTL 到期）
    for (String id : toRemove) {
      unregisterInternal(id, defaultLevel, StopReason.TTL_EXPIRED);
    }
  }

  private void processTrack(
      ServerLevel level, String trackId, TrackContext ctx, FxTrack track, List<String> toRemove) {
    try {
      // 首次启动：触发 onStart
      if (!ctx.started) {
        safeOnStart(track, level);
        ctx.started = true;
        return;
      }

      // 检查 TTL 是否到期
      if (ctx.elapsedTicks >= ctx.currentTtl) {
        toRemove.add(trackId);
        return;
      }

      // 按 tickInterval 执行 onTick
      int interval = track.getTickInterval();
      if (interval <= 0) {
        interval = 1;
      }
      if (ctx.elapsedTicks % interval == 0) {
        safeOnTick(track, level, ctx.elapsedTicks);
      }

      // 递增已运行 tick 数
      ctx.elapsedTicks++;

    } catch (Throwable t) {
      // 回调异常：不要在这里调用 onStop，避免重复调用
      // 外层会通过 unregisterInternal 统一处理
      LOGGER.error("[FxEngine] Error processing track {}", trackId, t);
      // 直接调用 unregisterInternal，确保只调用一次 onStop
      unregisterInternal(trackId, level, StopReason.EXCEPTION);
    }
  }

  private void safeOnStart(FxTrack track, ServerLevel level) {
    try {
      track.onStart(level);
    } catch (Throwable t) {
      LOGGER.error("[FxEngine] onStart failed for track {}", track.getId(), t);
    }
  }

  private void safeOnTick(FxTrack track, ServerLevel level, int elapsedTicks) {
    try {
      track.onTick(level, elapsedTicks);
    } catch (Throwable t) {
      LOGGER.error("[FxEngine] onTick failed for track {}", track.getId(), t);
    }
  }

  private void safeOnStop(FxTrack track, ServerLevel level, StopReason reason) {
    try {
      track.onStop(level, reason);
    } catch (Throwable t) {
      LOGGER.error("[FxEngine] onStop failed for track {}", track.getId(), t);
    }
  }

  /** Track 上下文：包装 Track 实例与运行时状态。 */
  private static class TrackContext {
    final FxTrack track;
    final FxTrackSpec spec;
    boolean started = false;
    int elapsedTicks = 0;
    int currentTtl; // 当前 TTL（可被 extendTtl 修改）

    TrackContext(FxTrack track, FxTrackSpec spec) {
      this.track = track;
      this.spec = spec;
      this.currentTtl = track.getTtlTicks();
    }

    void extendTtl(int additionalTicks) {
      this.currentTtl += additionalTicks;
    }
  }
}
