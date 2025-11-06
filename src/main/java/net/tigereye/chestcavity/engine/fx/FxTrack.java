package net.tigereye.chestcavity.engine.fx;

import java.util.UUID;
import net.minecraft.server.level.ServerLevel;

/**
 * FX Track 接口：定义一个 FX 时间线轨道的生命周期回调与基本属性。
 *
 * <p>每个 Track 在创建后会经历以下生命周期：
 *
 * <ul>
 *   <li>CREATED → STARTED（首 tick 前触发 {@link #onStart}）
 *   <li>STARTED → RUNNING（每 tickInterval 触发 {@link #onTick}）
 *   <li>RUNNING → STOPPED（TTL 到期或手动停止时触发 {@link #onStop}）
 * </ul>
 */
public interface FxTrack {

  /** 获取 Track 唯一标识符。 */
  String getId();

  /** 获取 TTL（总生命周期，单位：tick）。 */
  int getTtlTicks();

  /** 获取 Tick 间隔（单位：tick）。默认为 1，即每 tick 执行一次。 */
  default int getTickInterval() {
    return 1;
  }

  /** 获取 Owner ID（可选）。用于预算控制与限流。 */
  default UUID getOwnerId() {
    return null;
  }

  /**
   * Track 启动回调（首 tick 前调用一次）。
   *
   * @param level 服务器世界
   */
  void onStart(ServerLevel level);

  /**
   * Track Tick 回调（每 tickInterval 调用一次）。
   *
   * @param level 服务器世界
   * @param elapsedTicks 已运行的 tick 数
   */
  void onTick(ServerLevel level, int elapsedTicks);

  /**
   * Track 停止回调（TTL 到期或手动停止时调用一次）。
   *
   * @param level 服务器世界
   * @param reason 停止原因
   */
  void onStop(ServerLevel level, StopReason reason);
}
