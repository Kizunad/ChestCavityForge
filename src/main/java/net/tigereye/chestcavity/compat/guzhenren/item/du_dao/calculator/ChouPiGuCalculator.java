package net.tigereye.chestcavity.compat.guzhenren.item.du_dao.calculator;

import net.tigereye.chestcavity.compat.guzhenren.item.du_dao.tuning.ChouPiTuning;

/**
 * 臭屁蛊 纯计算器。
 *
 * 只包含纯函数：不读写世界/实体，仅做数值决策，便于单元测试。
 */
public final class ChouPiGuCalculator {

  private ChouPiGuCalculator() {}

  /**
   * 根据“毒增益”计算触发几率（如受伤触发）。
   *
   * chance = base * (1 + max(0, increase))，并钳制到 [0, 1]。
   */
  public static double triggerChanceWithIncrease(double base, double increase) {
    double inc = Math.max(0.0, increase);
    return clamp01(base * (1.0 + inc));
  }

  /** 基于食物类型的基础几率与乘数，返回最终几率（已钳制）。 */
  public static double foodTriggerChance(boolean isRotten, double base) {
    double mult = isRotten ? ChouPiTuning.ROTTEN_FOOD_MULTIPLIER : 1.0;
    return clamp01(base * mult);
  }

  /**
   * 返回“是否触发”。传入一个 [0,1) 的随机数以保持纯函数。
   */
  public static boolean shouldTrigger(double chance, double random01) {
    return random01 < clamp01(chance);
  }

  /** 按堆叠计算药水时长：max(MIN, stacks * PER_STACK)。 */
  public static int effectDurationTicks(int stackCount) {
    int stacks = Math.max(1, stackCount);
    int dur = Math.max(ChouPiTuning.MIN_EFFECT_DURATION_TICKS, stacks * ChouPiTuning.DURATION_PER_STACK_TICKS);
    return dur;
  }

  /** 按毒增益计算中毒等级：floor(max(0, increase))。 */
  public static int poisonAmplifier(double increase) {
    return (int) Math.floor(Math.max(0.0, increase));
  }

  /** 计算残留半径：max(minRadius, effectRadius * scale)。 */
  public static float residueRadius(float effectRadius) {
    return Math.max(ChouPiTuning.RESIDUE_RADIUS_MIN, effectRadius * ChouPiTuning.RESIDUE_RADIUS_SCALE);
  }

  /** 计算残留持续：max(minDuration, effectDuration/2)。 */
  public static int residueDurationTicks(int effectDurationTicks) {
    return Math.max(ChouPiTuning.RESIDUE_MIN_DURATION_TICKS, effectDurationTicks / 2);
  }

  /** 在 [min,max] 上取等概率离散值（传入 [0,1) 随机）。 */
  public static int randomIntervalTicks(int min, int max, double random01) {
    if (max <= min) {
      return min;
    }
    int range = max - min + 1;
    int step = (int) Math.floor(clamp01(random01) * range);
    if (step >= range) step = range - 1; // 保护上界
    return min + step;
  }

  private static double clamp01(double v) {
    if (v <= 0.0) return 0.0;
    if (v >= 1.0) return 1.0;
    return v;
  }
}

