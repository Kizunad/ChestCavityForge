package net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.ward;

import net.minecraft.world.phys.Vec3;

/**
 * 拦截规划的结果
 *
 * <p>此记录包含拦截点预测的所有必要信息，用于护幕飞剑执行拦截任务。
 *
 * <h3>拦截点 P*</h3>
 *
 * 拦截点是预测的最佳拦截位置，通常位于威胁命中轨迹上， 并可能提前一定距离（如 0.3m）以确保有效拦截。
 *
 * <h3>时间窗口</h3>
 *
 * 拦截系统使用时间窗口判定是否可拦截：
 *
 * <ul>
 *   <li>窗口范围：0.1 ~ 1.0 秒
 *   <li>太近（< 0.1s）：反应不及
 *   <li>太远（> 1.0s）：不值得拦截
 * </ul>
 *
 * @param interceptPoint 预计命中轨迹上的拦截点 P*
 * @param tImpact 投射物到达 P* 的预计时刻（秒）
 * @param threat 原始威胁信息（用于后续验证）
 */
public record InterceptQuery(Vec3 interceptPoint, double tImpact, IncomingThreat threat) {
  /**
   * 获取查询创建时的游戏时刻
   *
   * <p>注意：这个方法返回的是 {@link IncomingThreat#worldTime()}， 而不是当前时刻
   *
   * @return 威胁发生的游戏时刻（tick）
   */
  public long getThreatTick() {
    return threat.worldTime();
  }

  /**
   * 计算从当前时刻到预计命中的剩余时间
   *
   * @param currentTick 当前游戏时刻（tick）
   * @return 剩余时间（秒）
   */
  public double getTimeRemaining(long currentTick) {
    double elapsedTicks = currentTick - threat.worldTime();
    double elapsedSeconds = elapsedTicks / 20.0;
    return Math.max(0.0, tImpact - elapsedSeconds);
  }

  /**
   * 检查拦截是否已过期（已超过预计命中时刻）
   *
   * @param currentTick 当前游戏时刻（tick）
   * @return 如果已过期返回 true
   */
  public boolean isExpired(long currentTick) {
    return getTimeRemaining(currentTick) <= 0.0;
  }

  /**
   * 获取简要描述（用于日志和调试）
   *
   * @return 查询描述字符串
   */
  public String describe() {
    return String.format(
        "InterceptQuery[P*=(%.1f, %.1f, %.1f), t=%.2fs, %s]",
        interceptPoint.x, interceptPoint.y, interceptPoint.z, tImpact, threat.describe());
  }
}
