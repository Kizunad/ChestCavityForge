package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.calculator.resource;

import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.calculator.common.CalcMath;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.calculator.common.HunDaoCalcContext;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.tuning.HunDaoTuning;

/**
 * 魂魄/精力回复计算器（纯计算模块）。
 *
 * <p>职责：
 *
 * <ul>
 *   <li>计算小魂蛊的魂魄回复量
 *   <li>计算大魂蛊的魂魄和念头回复量
 *   <li>计算鬼气蛊/体魄蛊的被动回复
 * </ul>
 *
 * <p>所有计算不依赖 Minecraft 对象，仅根据输入参数返回数值结果。
 *
 * <p>Phase 4: Combat & Calculator
 */
public final class HunPoRecoveryCalculator {

  private HunPoRecoveryCalculator() {}

  /**
   * 计算小魂蛊的基础魂魄回复量。
   *
   * @return 基础回复量
   */
  public static double calculateXiaoHunGuBaseRecovery() {
    return HunDaoTuning.XiaoHunGu.RECOVER;
  }

  /**
   * 计算小魂蛊的额外回复量。
   *
   * @return 额外回复量
   */
  public static double calculateXiaoHunGuBonusRecovery() {
    return HunDaoTuning.XiaoHunGu.RECOVER_BONUS;
  }

  /**
   * 计算小魂蛊的总回复量。
   *
   * <p>公式：RECOVER + RECOVER_BONUS
   *
   * @return 总回复量
   */
  public static double calculateXiaoHunGuTotalRecovery() {
    return calculateXiaoHunGuBaseRecovery() + calculateXiaoHunGuBonusRecovery();
  }

  /**
   * 计算大魂蛊的魂魄回复量。
   *
   * @return 魂魄回复量
   */
  public static double calculateDaHunGuHunpoRecovery() {
    return HunDaoTuning.DaHunGu.RECOVER;
  }

  /**
   * 计算大魂蛊的念头生成量。
   *
   * @return 念头生成量
   */
  public static double calculateDaHunGuNiantouGeneration() {
    return HunDaoTuning.DaHunGu.NIANTOU;
  }

  /**
   * 计算鬼气蛊每秒的魂魄被动回复（基于堆叠数）。
   *
   * <p>公式：PASSIVE_HUNPO_PER_SECOND × stackCount
   *
   * @param stackCount 堆叠数
   * @return 每秒魂魄回复量
   */
  public static double calculateGuiQiGuHunpoRecoveryPerSecond(int stackCount) {
    return HunDaoTuning.GuiQiGu.PASSIVE_HUNPO_PER_SECOND * Math.max(1, stackCount);
  }

  /**
   * 计算鬼气蛊每秒的精力被动回复（基于堆叠数）。
   *
   * <p>公式：PASSIVE_JINGLI_PER_SECOND × stackCount
   *
   * @param stackCount 堆叠数
   * @return 每秒精力回复量
   */
  public static double calculateGuiQiGuJingliRecoveryPerSecond(int stackCount) {
    return HunDaoTuning.GuiQiGu.PASSIVE_JINGLI_PER_SECOND * Math.max(1, stackCount);
  }

  /**
   * 计算体魄蛊每秒的魂魄被动回复（基于堆叠数）。
   *
   * <p>公式：PASSIVE_HUNPO_PER_SECOND × stackCount
   *
   * @param stackCount 堆叠数
   * @return 每秒魂魄回复量
   */
  public static double calculateTiPoGuHunpoRecoveryPerSecond(int stackCount) {
    return HunDaoTuning.TiPoGu.PASSIVE_HUNPO_PER_SECOND * Math.max(1, stackCount);
  }

  /**
   * 计算体魄蛊每秒的精力被动回复（基于堆叠数）。
   *
   * <p>公式：PASSIVE_JINGLI_PER_SECOND × stackCount
   *
   * @param stackCount 堆叠数
   * @return 每秒精力回复量
   */
  public static double calculateTiPoGuJingliRecoveryPerSecond(int stackCount) {
    return HunDaoTuning.TiPoGu.PASSIVE_JINGLI_PER_SECOND * Math.max(1, stackCount);
  }

  /**
   * 计算体魄蛊的子魂增益加成。
   *
   * @return 增益加成（10% = 0.10）
   */
  public static double calculateZiHunIncreaseBonus() {
    return HunDaoTuning.TiPoGu.ZI_HUN_INCREASE_BONUS;
  }
}
