package net.tigereye.chestcavity.engine.fx;

import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FX 调度器门面：对外提供 FX Track 的调度接口。
 *
 * <p>Stage 2 完整实现：
 *
 * <ul>
 *   <li>schedule(track) / schedule(spec) - 提交一个 FX Track
 *   <li>cancel(trackId, level) - 取消一个 Track（触发 onStop 回调）
 *   <li>find(trackId) - 查找活跃的 Track
 *   <li>getStats() - 获取统计信息（活跃数、合并数、丢弃数等）
 * </ul>
 *
 * <p>支持预算控制、合并策略、配置读取等高级功能。
 */
public final class FxScheduler {

  private static final Logger LOGGER = LoggerFactory.getLogger(FxScheduler.class);

  private static final FxScheduler INSTANCE = new FxScheduler();

  private final FxTimelineEngine engine;

  private FxScheduler() {
    this.engine = FxTimelineEngine.getInstance();
  }

  public static FxScheduler getInstance() {
    return INSTANCE;
  }

  /**
   * 提交一个 FX Track 到时间线引擎（基于 Spec）。
   *
   * @param spec FxTrackSpec 实例
   * @return Track ID，如果被合并/丢弃则返回 null
   */
  public String schedule(FxTrackSpec spec) {
    if (spec == null) {
      throw new IllegalArgumentException("FxTrackSpec cannot be null");
    }

    FxTrack track = spec.toTrack();
    String trackId = engine.register(track, spec);

    if (trackId != null) {
      LOGGER.debug("[FxScheduler] Scheduled track: {}", trackId);
    } else {
      LOGGER.debug("[FxScheduler] Track was merged/dropped: {}", spec.getId());
    }

    return trackId;
  }

  /**
   * 提交一个 FX Track 到时间线引擎（直接使用 Track 实例）。
   *
   * @param track FX Track 实例
   * @return Track ID，如果被丢弃则返回 null
   */
  public String schedule(FxTrack track) {
    if (track == null) {
      throw new IllegalArgumentException("FxTrack cannot be null");
    }

    String trackId = engine.register(track);

    if (trackId != null) {
      LOGGER.debug("[FxScheduler] Scheduled track: {}", trackId);
    } else {
      LOGGER.debug("[FxScheduler] Track was dropped: {}", track.getId());
    }

    return trackId;
  }

  /**
   * 取消一个 Track（手动停止）。
   *
   * @param trackId Track ID
   * @param level 服务器世界（用于触发 onStop 回调）
   * @return 是否成功取消
   */
  public boolean cancel(String trackId, ServerLevel level) {
    if (trackId == null || trackId.isEmpty()) {
      return false;
    }

    boolean cancelled = engine.cancel(trackId, level);
    if (cancelled) {
      LOGGER.debug("[FxScheduler] Cancelled track: {}", trackId);
    }
    return cancelled;
  }

  /**
   * 查找活跃的 Track。
   *
   * @param trackId Track ID
   * @return Track 实例，如果不存在则返回 null
   */
  public FxTrack find(String trackId) {
    if (trackId == null || trackId.isEmpty()) {
      return null;
    }
    return engine.find(trackId);
  }

  /** 获取当前活跃 Track 数量。 */
  public int getActiveCount() {
    return engine.getActiveCount();
  }

  /** 获取统计信息。 */
  public Stats getStats() {
    return new Stats(
        engine.getActiveCount(),
        engine.getMergeCount(),
        engine.getDropCount(),
        engine.getReplaceCount());
  }

  /** FxScheduler 统计信息。 */
  public static final class Stats {
    public final int activeCount;
    public final int mergeCount;
    public final int dropCount;
    public final int replaceCount;

    Stats(int activeCount, int mergeCount, int dropCount, int replaceCount) {
      this.activeCount = activeCount;
      this.mergeCount = mergeCount;
      this.dropCount = dropCount;
      this.replaceCount = replaceCount;
    }

    @Override
    public String toString() {
      return "Stats{"
          + "active="
          + activeCount
          + ", merge="
          + mergeCount
          + ", drop="
          + dropCount
          + ", replace="
          + replaceCount
          + '}';
    }
  }
}
