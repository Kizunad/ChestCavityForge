package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.calculator.common;

/**
 * 魂道战斗计算的通用数学工具.
 *
 * <p>提供：
 *
 * <ul>
 *   <li>夹紧（clamp）
 *   <li>软上限（soft cap）
 *   <li>比例缩放（scale）
 * </ul>
 *
 * <p>Phase 4: Combat & Calculator
 */
public final class CalcMath {

  private CalcMath() {}

  /**
   * 将值夹紧在 [min, max] 范围内.
   *
   * @param value 输入值
   * @param min 最小值
   * @param max 最大值
   * @return 夹紧后的值
   */
  public static double clamp(double value, double min, double max) {
    if (value < min) {
      return min;
    }
    if (value > max) {
      return max;
    }
    return value;
  }

  /**
   * 应用软上限：超过阈值后收益递减.
   *
   * <p>公式：
   *
   * <ul>
   *   <li>value ≤ threshold: 返回 value
   *   <li>value > threshold: threshold + (value - threshold) * diminish
   * </ul>
   *
   * @param value 输入值
   * @param threshold 阈值
   * @param diminish 超出阈值部分的衰减系数（0.0 ~ 1.0）
   * @return 应用软上限后的值
   */
  public static double softCap(double value, double threshold, double diminish) {
    if (value <= threshold) {
      return value;
    }
    double excess = value - threshold;
    return threshold + excess * clamp(diminish, 0.0, 1.0);
  }

  /**
   * 线性缩放：将输入值从 [inMin, inMax] 映射到 [outMin, outMax].
   *
   * @param value 输入值
   * @param inMin 输入范围最小值
   * @param inMax 输入范围最大值
   * @param outMin 输出范围最小值
   * @param outMax 输出范围最大值
   * @return 缩放后的值
   */
  public static double scale(
      double value, double inMin, double inMax, double outMin, double outMax) {
    if (Math.abs(inMax - inMin) < 1e-6) {
      return outMin;
    }
    double normalized = (value - inMin) / (inMax - inMin);
    return outMin + normalized * (outMax - outMin);
  }

  /**
   * 计算百分比增益：base * (1 + bonusPercent).
   *
   * @param base 基础值
   * @param bonusPercent 增益百分比（0.1 表示 10%）
   * @return 应用增益后的值
   */
  public static double applyBonus(double base, double bonusPercent) {
    return base * (1.0 + bonusPercent);
  }

  /**
   * 计算乘法叠加：base * multiplier.
   *
   * @param base 基础值
   * @param multiplier 乘数
   * @return 乘法结果
   */
  public static double multiply(double base, double multiplier) {
    return base * Math.max(0.0, multiplier);
  }

  /**
   * 计算加法叠加：base + flat.
   *
   * @param base 基础值
   * @param flat 平坦加成
   * @return 加法结果
   */
  public static double add(double base, double flat) {
    return base + flat;
  }
}
