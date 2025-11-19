package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.calculator.resource;

import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.tuning.HunDaoTuning;

/**
 * 魂魄泄露计算器（纯计算模块）.
 *
 * <p>职责：
 *
 * <ul>
 *   <li>计算魂兽化状态下的被动魂魄泄露速率
 *   <li>计算攻击时的魂魄消耗
 *   <li>判断魂魄是否足够支付消耗
 * </ul>
 *
 * <p>所有计算不依赖 Minecraft 对象，仅根据输入参数返回数值结果。
 *
 * <p>Phase 4: Combat & Calculator
 */
public final class HunPoDrainCalculator {

  private HunPoDrainCalculator() {}

  /**
   * 计算魂兽化状态下每秒泄露的魂魄量.
   *
   * @return 每秒泄露量
   */
  public static double calculateLeakPerSecond() {
    return HunDaoTuning.SoulBeast.HUNPO_LEAK_PER_SEC;
  }

  /**
   * 计算魂兽化状态下每 tick 泄露的魂魄量.
   *
   * <p>公式：HUNPO_LEAK_PER_SEC / 20
   *
   * @return 每 tick 泄露量
   */
  public static double calculateLeakPerTick() {
    return HunDaoTuning.SoulBeast.HUNPO_LEAK_PER_SEC / 20.0;
  }

  /**
   * 计算魂兽化攻击时的魂魄消耗.
   *
   * @return 攻击消耗
   */
  public static double calculateAttackCost() {
    return HunDaoTuning.SoulBeast.ON_HIT_COST;
  }

  /**
   * 判断当前魂魄是否足够支付消耗.
   *
   * @param currentHunpo 当前魂魄值
   * @param cost 需要的消耗
   * @return true 如果足够
   */
  public static boolean canAfford(double currentHunpo, double cost) {
    return currentHunpo >= cost;
  }

  /**
   * 计算魂魄耗尽前的剩余秒数（魂兽化状态）.
   *
   * <p>公式：currentHunpo / HUNPO_LEAK_PER_SEC
   *
   * @param currentHunpo 当前魂魄值
   * @return 剩余秒数
   */
  public static double calculateRemainingSeconds(double currentHunpo) {
    double leakPerSec = calculateLeakPerSecond();
    if (leakPerSec <= 0.0) {
      return Double.POSITIVE_INFINITY;
    }
    return Math.max(0.0, currentHunpo / leakPerSec);
  }

  /**
   * 计算魂魄耗尽前的剩余 ticks（魂兽化状态）.
   *
   * <p>公式：remainingSeconds × 20
   *
   * @param currentHunpo 当前魂魄值
   * @return 剩余 ticks
   */
  public static long calculateRemainingTicks(double currentHunpo) {
    double remainingSeconds = calculateRemainingSeconds(currentHunpo);
    if (Double.isInfinite(remainingSeconds)) {
      return Long.MAX_VALUE;
    }
    return (long) (remainingSeconds * 20);
  }
}
