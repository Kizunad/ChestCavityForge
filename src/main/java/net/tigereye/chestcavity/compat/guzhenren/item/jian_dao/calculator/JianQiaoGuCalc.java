package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.calculator;

import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.tuning.JianQiaoGuTuning;

/** 纯计算逻辑：剑鞘蛊。 */
public final class JianQiaoGuCalc {

  private JianQiaoGuCalc() {}

  /** 根据剑道道痕计算额外容量。 */
  public static int extraSlotsFromDaoHen(double daoHen) {
    if (!(daoHen > 0.0) || !Double.isFinite(daoHen)) {
      return 0;
    }
    return (int) Math.floor(Math.max(0.0, daoHen) / JianQiaoGuTuning.DAO_HEN_PER_EXTRA_SLOT);
  }

  /**
   * 判断是否满足“道痕、流派经验均≥2x目标”的压制条件。
   *
   * @param ourDaoHen 我方剑道道痕
   * @param targetDaoHen 对方剑道道痕
   * @param ourExp 我方剑道流派经验
   * @param targetExp 对方剑道流派经验
   */
  public static boolean canOverwhelm(
      double ourDaoHen, double targetDaoHen, double ourExp, double targetExp) {
    boolean daoHenAdvantage =
        targetDaoHen <= 0.0 ? ourDaoHen > 0.0 : ourDaoHen >= targetDaoHen * 2.0;
    boolean expAdvantage =
        targetExp <= 0.0 ? ourExp > 0.0 : ourExp >= targetExp * 2.0;
    return daoHenAdvantage && expAdvantage;
  }

  /**
   * 计算收剑令成功概率。
   *
   * <p>按照规范：5 × (1 + 双方剑道流派经验差)% ，保底 5%，封顶 75%。
   */
  public static double seizeChance(double ourExp, double targetExp) {
    double diffRatio;
    if (targetExp <= 0.0) {
      diffRatio = ourExp > 0.0 ? 1.0 : 0.0;
    } else {
      diffRatio = (ourExp - targetExp) / Math.max(1.0, targetExp);
    }
    diffRatio = Math.max(0.0, diffRatio);
    double chance = 0.05 * (1.0 + diffRatio);
    return Math.max(0.05, Math.min(0.75, chance));
  }

  /** 计算最终容量。 */
  public static int computeCapacity(boolean equipped, double daoHen) {
    int base = JianQiaoGuTuning.computeCapacity(equipped, daoHen);
    return Math.max(JianQiaoGuTuning.BASE_CAPACITY, base);
  }
}
