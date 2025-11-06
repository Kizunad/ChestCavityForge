package net.tigereye.chestcavity.engine.fx;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.tigereye.chestcavity.engine.ServerTickEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FX 时间线引擎：负责维护活跃的 {@link FxTrack}，处理生命周期回调与 TTL 管理。
 *
 * <p>引擎每 tick 执行以下流程：
 *
 * <ul>
 *   <li>对所有活跃 Track 按 tickInterval 触发 onTick
 *   <li>检查 TTL 是否到期，到期则触发 onStop 并移除
 *   <li>异常隔离：任何回调异常不影响其他 Track
 * </ul>
 */
public final class FxTimelineEngine implements ServerTickEngine {

  private static final Logger LOGGER = LoggerFactory.getLogger(FxTimelineEngine.class);

  /** 单例实例。 */
  private static final FxTimelineEngine INSTANCE = new FxTimelineEngine();

  /** 活跃 Track 表：trackId -> TrackContext。 */
  private final Map<String, TrackContext> activeTracks = new ConcurrentHashMap<>();

  private FxTimelineEngine() {}

  public static FxTimelineEngine getInstance() {
    return INSTANCE;
  }

  /**
   * 注册一个新的 Track。
   *
   * @param track FX Track 实例
   * @return Track ID
   */
  public String register(FxTrack track) {
    if (track == null) {
      throw new IllegalArgumentException("FxTrack cannot be null");
    }
    String id = track.getId();
    if (id == null || id.isEmpty()) {
      throw new IllegalArgumentException("FxTrack ID cannot be null or empty");
    }
    if (activeTracks.containsKey(id)) {
      LOGGER.warn("[FxEngine] Track {} already exists, skipping registration", id);
      return id;
    }
    activeTracks.put(id, new TrackContext(track));
    LOGGER.debug("[FxEngine] Registered track: {}", id);
    return id;
  }

  /**
   * 取消一个 Track（手动停止）。
   *
   * @param trackId Track ID
   * @return 是否成功取消
   */
  public boolean cancel(String trackId) {
    TrackContext ctx = activeTracks.remove(trackId);
    if (ctx != null) {
      LOGGER.debug("[FxEngine] Cancelled track: {}", trackId);
      return true;
    }
    return false;
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
    // TODO Stage 2: 支持 Track 关联到特定 Level
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
        LOGGER.error("[FxEngine] Unexpected error processing track {}", trackId, t);
        toRemove.add(trackId);
        safeOnStop(track, defaultLevel, StopReason.EXCEPTION);
      }
    }

    // 移除已停止的 Track
    for (String id : toRemove) {
      activeTracks.remove(id);
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
      if (ctx.elapsedTicks >= track.getTtlTicks()) {
        toRemove.add(trackId);
        safeOnStop(track, level, StopReason.TTL_EXPIRED);
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
      LOGGER.error("[FxEngine] Error processing track {}", trackId, t);
      toRemove.add(trackId);
      safeOnStop(track, level, StopReason.EXCEPTION);
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
    boolean started = false;
    int elapsedTicks = 0;

    TrackContext(FxTrack track) {
      this.track = track;
    }
  }
}
