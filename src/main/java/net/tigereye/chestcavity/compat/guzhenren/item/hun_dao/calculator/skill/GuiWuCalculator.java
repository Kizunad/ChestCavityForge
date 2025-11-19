package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.calculator.skill;

import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.calculator.common.CalcMath;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.tuning.HunDaoTuning;

/**
 * 鬼雾（Gui Wu）技能计算器（纯计算模块）.
 *
 * <p>职责：
 *
 * <ul>
 *   <li>计算鬼雾影响范围
 *   <li>计算鬼雾效果持续时间
 *   <li>计算鬼雾魂魄消耗
 * </ul>
 *
 * <p>所有计算不依赖 Minecraft 对象，仅根据输入参数返回数值结果。
 *
 * <p>Phase 4: Combat & Calculator
 */
public final class GuiWuCalculator {

  private GuiWuCalculator() {}

  /**
   * 计算鬼雾的影响范围.
   *
   * @return 影响范围（方块）
   */
  public static double calculateRadius() {
    return HunDaoTuning.GuiQiGu.GUI_WU_RADIUS;
  }

  /**
   * 计算鬼雾的影响范围平方（用于距离比较优化）.
   *
   * <p>用于避免开平方根的性能开销。
   *
   * @return 影响范围平方
   */
  public static double calculateRadiusSquared() {
    double radius = calculateRadius();
    return radius * radius;
  }

  /**
   * 判断目标是否在鬼雾范围内.
   *
   * @param distanceSquared 距离平方
   * @return true 如果在范围内
   */
  public static boolean isWithinRange(double distanceSquared) {
    return distanceSquared <= calculateRadiusSquared();
  }

  /**
   * 计算基于范围的衰减系数（中心最强，边缘最弱）.
   *
   * <p>公式：1.0 - (distance / radius)
   *
   * @param distance 实际距离
   * @return 衰减系数（0.0 ~ 1.0）
   */
  public static double calculateFalloff(double distance) {
    double radius = calculateRadius();
    if (distance >= radius) {
      return 0.0;
    }
    if (distance <= 0.0) {
      return 1.0;
    }
    return CalcMath.clamp(1.0 - (distance / radius), 0.0, 1.0);
  }
}
