package net.tigereye.chestcavity.engine.fx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FX 调度器门面：对外提供 FX Track 的调度接口。
 *
 * <p>Stage 1 最小实现：
 *
 * <ul>
 *   <li>schedule(track) - 提交一个 FX Track
 *   <li>cancel(trackId) - 取消一个 Track
 *   <li>find(trackId) - 查找活跃的 Track
 * </ul>
 *
 * <p>后续阶段将添加预算控制、合并策略、配置读取等功能。
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
   * 提交一个 FX Track 到时间线引擎。
   *
   * @param track FX Track 实例
   * @return Track ID
   */
  public String schedule(FxTrack track) {
    if (track == null) {
      throw new IllegalArgumentException("FxTrack cannot be null");
    }

    // Stage 1: 直接注册，无预算控制与合并
    // TODO Stage 2: 添加预算检查、合并策略、配置读取
    String trackId = engine.register(track);
    LOGGER.debug("[FxScheduler] Scheduled track: {}", trackId);
    return trackId;
  }

  /**
   * 取消一个 Track（手动停止）。
   *
   * @param trackId Track ID
   * @return 是否成功取消
   */
  public boolean cancel(String trackId) {
    if (trackId == null || trackId.isEmpty()) {
      return false;
    }

    boolean cancelled = engine.cancel(trackId);
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
}
