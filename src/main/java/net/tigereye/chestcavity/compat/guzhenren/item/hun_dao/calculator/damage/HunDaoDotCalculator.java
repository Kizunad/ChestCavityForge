package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.calculator.damage;

import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.calculator.common.HunDaoCalcContext;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.tuning.HunDaoTuning;

/**
 * 魂道持续伤害（DoT）计算器（纯计算模块）。
 *
 * <p>职责：
 *
 * <ul>
 *   <li>计算魂焰（Soul Flame）每秒伤害
 *   <li>计算魂焰持续时间
 *   <li>计算魂焰总伤害
 * </ul>
 *
 * <p>所有计算不依赖 Minecraft 对象，仅根据输入参数返回数值结果。
 *
 * <p>Phase 4: Combat & Calculator
 */
public final class HunDaoDotCalculator {

  private HunDaoDotCalculator() {}

  /**
   * 计算魂焰每秒伤害。
   *
   * <p>公式：maxHunpo × DPS_FACTOR × efficiency
   *
   * @param context 计算上下文
   * @return 每秒伤害值
   */
  public static double calculateSoulFlameDps(HunDaoCalcContext context) {
    if (context == null) {
      return 0.0;
    }
    double maxHunpo = context.getMaxHunpo();
    double efficiency = context.getEfficiency();
    if (maxHunpo <= 0.0) {
      return 0.0;
    }
    return Math.max(0.0, maxHunpo * HunDaoTuning.SoulFlame.DPS_FACTOR * efficiency);
  }

  /**
   * 计算魂焰持续秒数。
   *
   * @return 持续秒数
   */
  public static int getSoulFlameDurationSeconds() {
    return HunDaoTuning.SoulFlame.DURATION_SECONDS;
  }

  /**
   * 计算魂焰持续 ticks。
   *
   * @return 持续 ticks (20 ticks = 1 second)
   */
  public static int getSoulFlameDurationTicks() {
    return HunDaoTuning.SoulFlame.DURATION_SECONDS * 20;
  }

  /**
   * 计算魂焰总伤害。
   *
   * <p>公式：dps × duration
   *
   * @param context 计算上下文
   * @return 总伤害值
   */
  public static double calculateSoulFlameTotalDamage(HunDaoCalcContext context) {
    if (context == null) {
      return 0.0;
    }
    double dps = calculateSoulFlameDps(context);
    int duration = getSoulFlameDurationSeconds();
    return dps * duration;
  }

  /**
   * 计算基于魂焰 DPS 的每 tick 伤害。
   *
   * <p>公式：dps / 20 (20 ticks per second)
   *
   * @param dps 每秒伤害
   * @return 每 tick 伤害
   */
  public static double calculateDamagePerTick(double dps) {
    return dps / 20.0;
  }
}
