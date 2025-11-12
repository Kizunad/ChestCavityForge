package net.tigereye.chestcavity.compat.guzhenren.util;

import net.minecraft.util.Mth;

/**
 * 通用道系统冷却计算工具类。
 *
 * <p>提供基于流派经验(liupai)的冷却减免计算,确保冷却时间不低于最小值(1秒=20ticks)。
 *
 * <p>计算公式:
 * <pre>
 * progress = clamp(liupaiExp, 0, 10001) / 10001
 * reduction = (baseTicks - minTicks) * progress
 * finalCooldown = max(minTicks, baseTicks - reduction)
 * </pre>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 在技能冷却计算中使用
 * long baseCooldown = 200L; // 10秒基础冷却
 * int liupaiExp = cc.getOrganScore(CCOrganScores.LIUPAI_FENGDAO);
 * long actualCooldown = DaoCooldownCalculator.calculateCooldown(baseCooldown, liupaiExp);
 * // actualCooldown 将在 [20, 200] 范围内,根据流派经验线性减免
 * }</pre>
 */
public final class DaoCooldownCalculator {

  private DaoCooldownCalculator() {}

  /** 最大流派经验值。 */
  public static final int MAX_LIUPAI_EXP = 10001;

  /** 最低冷却时间(1秒 = 20 ticks)。 */
  public static final long MIN_COOLDOWN_TICKS = 20L;

  /**
   * 根据流派经验计算冷却时间,使用默认最小值(20 ticks)。
   *
   * @param baseTicks 基础冷却时间(ticks)
   * @param liupaiExp 流派经验值(0-10001),会自动clamp到有效范围
   * @return 计算后的冷却时间,最低20ticks(1秒)
   */
  public static long calculateCooldown(long baseTicks, int liupaiExp) {
    return calculateCooldown(baseTicks, liupaiExp, MIN_COOLDOWN_TICKS);
  }

  /**
   * 根据流派经验计算冷却时间(自定义最小值)。
   *
   * @param baseTicks 基础冷却时间(ticks)
   * @param liupaiExp 流派经验值(0-10001),会自动clamp到有效范围
   * @param minTicks 最低冷却时间(ticks)
   * @return 计算后的冷却时间,不低于minTicks
   */
  public static long calculateCooldown(long baseTicks, int liupaiExp, long minTicks) {
    // 如果基础冷却已经 <= 最小值,直接返回
    if (baseTicks <= minTicks) {
      return baseTicks;
    }

    // Clamp流派经验到有效范围
    int clampedExp = Mth.clamp(liupaiExp, 0, MAX_LIUPAI_EXP);

    // 计算减免进度 [0.0, 1.0]
    double progress = clampedExp / (double) MAX_LIUPAI_EXP;

    // 计算可减免的冷却量
    long reduction = Math.round((baseTicks - minTicks) * progress);

    // 应用减免,确保不低于最小值
    return Math.max(minTicks, baseTicks - reduction);
  }

  /**
   * 计算冷却减免百分比。
   *
   * @param liupaiExp 流派经验值(0-10001)
   * @return 减免百分比(0.0-1.0), 0.0表示无减免, 1.0表示最大减免
   */
  public static double getReductionRatio(int liupaiExp) {
    int clampedExp = Mth.clamp(liupaiExp, 0, MAX_LIUPAI_EXP);
    return clampedExp / (double) MAX_LIUPAI_EXP;
  }

  /**
   * 计算实际减免的冷却时间。
   *
   * @param baseTicks 基础冷却时间(ticks)
   * @param liupaiExp 流派经验值(0-10001)
   * @param minTicks 最低冷却时间(ticks)
   * @return 实际减免的ticks数量
   */
  public static long getReductionAmount(long baseTicks, int liupaiExp, long minTicks) {
    if (baseTicks <= minTicks) {
      return 0L;
    }
    long finalCooldown = calculateCooldown(baseTicks, liupaiExp, minTicks);
    return baseTicks - finalCooldown;
  }
}

