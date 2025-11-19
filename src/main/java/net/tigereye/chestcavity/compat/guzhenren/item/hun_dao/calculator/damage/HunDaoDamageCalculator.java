package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.calculator.damage;

import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.calculator.common.HunDaoCalcContext;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.tuning.HunDaoTuning;

/**
 * 魂道伤害计算器（纯计算模块）.
 *
 * <p>职责：
 *
 * <ul>
 *   <li>计算真实伤害（鬼气蛊、体魄蛊基于最大魂魄的附加伤害）
 *   <li>计算魂兽化攻击消耗
 *   <li>计算护盾数值
 * </ul>
 *
 * <p>所有计算不依赖 Minecraft 对象，仅根据输入参数返回数值结果。
 *
 * <p>Phase 4: Combat & Calculator
 */
public final class HunDaoDamageCalculator {

  private HunDaoDamageCalculator() {}

  /**
   * 计算鬼气蛊的真实伤害.
   *
   * <p>公式：maxHunpo × TRUE_DAMAGE_RATIO
   *
   * @param context 计算上下文
   * @return 真实伤害值
   */
  public static float calculateGuiQiGuTrueDamage(HunDaoCalcContext context) {
    if (context == null) {
      return 0.0F;
    }
    double maxHunpo = context.getMaxHunpo();
    if (maxHunpo <= 0.0) {
      return 0.0F;
    }
    return (float) (maxHunpo * HunDaoTuning.GuiQiGu.TRUE_DAMAGE_RATIO);
  }

  /**
   * 计算体魄蛊魂兽模式的附加伤害.
   *
   * <p>公式：maxHunpo × SOUL_BEAST_DAMAGE_PERCENT
   *
   * @param context 计算上下文
   * @return 附加伤害值
   */
  public static float calculateTiPoGuSoulBeastDamage(HunDaoCalcContext context) {
    if (context == null) {
      return 0.0F;
    }
    double maxHunpo = context.getMaxHunpo();
    if (maxHunpo <= 0.0) {
      return 0.0F;
    }
    return (float) (maxHunpo * HunDaoTuning.TiPoGu.SOUL_BEAST_DAMAGE_PERCENT);
  }

  /**
   * 计算体魄蛊的魂兽攻击魂魄消耗.
   *
   * <p>公式：maxHunpo × SOUL_BEAST_HUNPO_COST_PERCENT
   *
   * @param maxHunpo 最大魂魄值
   * @return 消耗的魂魄值
   */
  public static double calculateTiPoGuHunpoCost(double maxHunpo) {
    if (maxHunpo <= 0.0) {
      return 0.0;
    }
    return maxHunpo * HunDaoTuning.TiPoGu.SOUL_BEAST_HUNPO_COST_PERCENT;
  }

  /**
   * 计算体魄蛊的护盾数值.
   *
   * <p>公式：maxHunpo × SHIELD_PERCENT × stackCount
   *
   * @param context 计算上下文
   * @return 护盾数值
   */
  public static float calculateTiPoGuShield(HunDaoCalcContext context) {
    if (context == null) {
      return 0.0F;
    }
    double maxHunpo = context.getMaxHunpo();
    int stackCount = context.getStackCount();
    if (maxHunpo <= 0.0 || stackCount <= 0) {
      return 0.0F;
    }
    double shield = maxHunpo * HunDaoTuning.TiPoGu.SHIELD_PERCENT * stackCount;
    return (float) Math.max(0.0, shield);
  }

  /**
   * 计算调整后的魂兽化攻击消耗（考虑大魂蛊减免）.
   *
   * <p>公式：max(0, baseCost - reduction)
   *
   * @param baseCost 基础消耗
   * @param reduction 减免量
   * @return 调整后的消耗
   */
  public static double calculateAdjustedAttackCost(double baseCost, double reduction) {
    return Math.max(0.0, baseCost - Math.max(0.0, reduction));
  }
}
