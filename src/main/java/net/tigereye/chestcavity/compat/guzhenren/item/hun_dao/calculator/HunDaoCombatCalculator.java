package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.calculator;

import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.calculator.common.HunDaoCalcContext;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.calculator.damage.HunDaoDamageCalculator;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.calculator.damage.HunDaoDotCalculator;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.calculator.resource.HunPoDrainCalculator;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.calculator.resource.HunPoRecoveryCalculator;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.calculator.skill.GuiWuCalculator;

/**
 * 魂道战斗计算器门面（Facade）.
 *
 * <p>职责：
 *
 * <ul>
 *   <li>为行为层提供统一的计算接口
 *   <li>组合各子计算器（damage/resource/skill）
 *   <li>简化依赖注入和调用
 * </ul>
 *
 * <p>使用示例：
 *
 * <pre>{@code
 * HunDaoCalcContext context =
 *     HunDaoCalcContext.create(currentHunpo, maxHunpo, efficiency, stackCount);
 * double soulFlameDps =
 *     HunDaoCombatCalculator.damage().calculateSoulFlameDps(context);
 * float trueDamage =
 *     HunDaoCombatCalculator.damage().calculateGuiQiGuTrueDamage(context);
 * double leakPerSec =
 *     HunDaoCombatCalculator.resource().calculateLeakPerSecond();
 * }</pre>
 *
 * <p>Phase 4: Combat & Calculator
 */
public final class HunDaoCombatCalculator {

  private HunDaoCombatCalculator() {}

  /**
   * 获取伤害计算器（真实伤害、附加伤害、护盾）.
   *
   * @return 伤害计算器
   */
  public static DamageOps damage() {
    return DamageOps.INSTANCE;
  }

  /**
   * 获取 DoT 计算器（魂焰持续伤害）.
   *
   * @return DoT 计算器
   */
  public static DotOps dot() {
    return DotOps.INSTANCE;
  }

  /**
   * 获取资源计算器（魂魄泄露、回复）.
   *
   * @return 资源计算器
   */
  public static ResourceOps resource() {
    return ResourceOps.INSTANCE;
  }

  /**
   * 获取技能计算器（鬼雾范围、效果）.
   *
   * @return 技能计算器
   */
  public static SkillOps skill() {
    return SkillOps.INSTANCE;
  }

  // ===== Nested Operation Classes =====

  /** 伤害计算操作. */
  public static final class DamageOps {
    private static final DamageOps INSTANCE = new DamageOps();

    private DamageOps() {}

    /**
     * 计算鬼气蛊真实伤害.
     *
     * @param context 计算上下文
     * @return 真实伤害值
     */
    public float calculateGuiQiGuTrueDamage(HunDaoCalcContext context) {
      return HunDaoDamageCalculator.calculateGuiQiGuTrueDamage(context);
    }

    /**
     * 计算体魄蛊魂兽模式附加伤害.
     *
     * @param context 计算上下文
     * @return 附加伤害值
     */
    public float calculateTiPoGuSoulBeastDamage(HunDaoCalcContext context) {
      return HunDaoDamageCalculator.calculateTiPoGuSoulBeastDamage(context);
    }

    /**
     * 计算体魄蛊魂兽攻击魂魄消耗.
     *
     * @param maxHunpo 最大魂魄值
     * @return 消耗的魂魄值
     */
    public double calculateTiPoGuHunpoCost(double maxHunpo) {
      return HunDaoDamageCalculator.calculateTiPoGuHunpoCost(maxHunpo);
    }

    /**
     * 计算体魄蛊护盾数值.
     *
     * @param context 计算上下文
     * @return 护盾数值
     */
    public float calculateTiPoGuShield(HunDaoCalcContext context) {
      return HunDaoDamageCalculator.calculateTiPoGuShield(context);
    }

    /**
     * 计算调整后的魂兽化攻击消耗.
     *
     * @param baseCost 基础消耗
     * @param reduction 减免量
     * @return 调整后的消耗
     */
    public double calculateAdjustedAttackCost(double baseCost, double reduction) {
      return HunDaoDamageCalculator.calculateAdjustedAttackCost(baseCost, reduction);
    }
  }

  /** DoT 计算操作. */
  public static final class DotOps {
    private static final DotOps INSTANCE = new DotOps();

    private DotOps() {}

    /**
     * 计算魂焰每秒伤害.
     *
     * @param context 计算上下文
     * @return 每秒伤害值
     */
    public double calculateSoulFlameDps(HunDaoCalcContext context) {
      return HunDaoDotCalculator.calculateSoulFlameDps(context);
    }

    /**
     * 获取魂焰持续秒数.
     *
     * @return 持续秒数
     */
    public int getSoulFlameDurationSeconds() {
      return HunDaoDotCalculator.getSoulFlameDurationSeconds();
    }

    /**
     * 获取魂焰持续 tick 数.
     *
     * @return 持续 tick 数
     */
    public int getSoulFlameDurationTicks() {
      return HunDaoDotCalculator.getSoulFlameDurationTicks();
    }

    /**
     * 计算魂焰总伤害.
     *
     * @param context 计算上下文
     * @return 总伤害值
     */
    public double calculateSoulFlameTotalDamage(HunDaoCalcContext context) {
      return HunDaoDotCalculator.calculateSoulFlameTotalDamage(context);
    }

    /**
     * 计算每 tick 伤害.
     *
     * @param dps 每秒伤害
     * @return 每 tick 伤害
     */
    public double calculateDamagePerTick(double dps) {
      return HunDaoDotCalculator.calculateDamagePerTick(dps);
    }
  }

  /** 资源计算操作. */
  public static final class ResourceOps {
    private static final ResourceOps INSTANCE = new ResourceOps();

    private ResourceOps() {}

    // Drain operations

    /**
     * 计算魂兽化状态下每秒泄露的魂魄量.
     *
     * @return 每秒泄露量
     */
    public double calculateLeakPerSecond() {
      return HunPoDrainCalculator.calculateLeakPerSecond();
    }

    /**
     * 计算魂兽化状态下每 tick 泄露的魂魄量.
     *
     * @return 每 tick 泄露量
     */
    public double calculateLeakPerTick() {
      return HunPoDrainCalculator.calculateLeakPerTick();
    }

    /**
     * 计算魂兽化攻击时的魂魄消耗.
     *
     * @return 攻击消耗
     */
    public double calculateAttackCost() {
      return HunPoDrainCalculator.calculateAttackCost();
    }

    /**
     * 判断当前魂魄是否足够支付消耗.
     *
     * @param currentHunpo 当前魂魄值
     * @param cost 需要的消耗
     * @return true 如果足够
     */
    public boolean canAfford(double currentHunpo, double cost) {
      return HunPoDrainCalculator.canAfford(currentHunpo, cost);
    }

    /**
     * 计算魂魄耗尽前的剩余秒数（魂兽化状态）.
     *
     * @param currentHunpo 当前魂魄值
     * @return 剩余秒数
     */
    public double calculateRemainingSeconds(double currentHunpo) {
      return HunPoDrainCalculator.calculateRemainingSeconds(currentHunpo);
    }

    /**
     * 计算魂魄耗尽前的剩余 tick 数（魂兽化状态）.
     *
     * @param currentHunpo 当前魂魄值
     * @return 剩余 tick 数
     */
    public long calculateRemainingTicks(double currentHunpo) {
      return HunPoDrainCalculator.calculateRemainingTicks(currentHunpo);
    }

    // Recovery operations

    /**
     * 计算小魂蛊的基础魂魄回复量.
     *
     * @return 基础回复量
     */
    public double calculateXiaoHunGuBaseRecovery() {
      return HunPoRecoveryCalculator.calculateXiaoHunGuBaseRecovery();
    }

    /**
     * 计算小魂蛊的额外回复量.
     *
     * @return 额外回复量
     */
    public double calculateXiaoHunGuBonusRecovery() {
      return HunPoRecoveryCalculator.calculateXiaoHunGuBonusRecovery();
    }

    /**
     * 计算小魂蛊的总回复量.
     *
     * @return 总回复量
     */
    public double calculateXiaoHunGuTotalRecovery() {
      return HunPoRecoveryCalculator.calculateXiaoHunGuTotalRecovery();
    }

    /**
     * 计算大魂蛊的魂魄回复量.
     *
     * @return 魂魄回复量
     */
    public double calculateDaHunGuHunpoRecovery() {
      return HunPoRecoveryCalculator.calculateDaHunGuHunpoRecovery();
    }

    /**
     * 计算大魂蛊的念头生成量.
     *
     * @return 念头生成量
     */
    public double calculateDaHunGuNiantouGeneration() {
      return HunPoRecoveryCalculator.calculateDaHunGuNiantouGeneration();
    }

    /**
     * 计算鬼气蛊每秒的魂魄被动回复.
     *
     * @param stackCount 堆叠数
     * @return 每秒魂魄回复量
     */
    public double calculateGuiQiGuHunpoRecoveryPerSecond(int stackCount) {
      return HunPoRecoveryCalculator.calculateGuiQiGuHunpoRecoveryPerSecond(stackCount);
    }

    /**
     * 计算鬼气蛊每秒的精力被动回复.
     *
     * @param stackCount 堆叠数
     * @return 每秒精力回复量
     */
    public double calculateGuiQiGuJingliRecoveryPerSecond(int stackCount) {
      return HunPoRecoveryCalculator.calculateGuiQiGuJingliRecoveryPerSecond(stackCount);
    }

    /**
     * 计算体魄蛊每秒的魂魄被动回复.
     *
     * @param stackCount 堆叠数
     * @return 每秒魂魄回复量
     */
    public double calculateTiPoGuHunpoRecoveryPerSecond(int stackCount) {
      return HunPoRecoveryCalculator.calculateTiPoGuHunpoRecoveryPerSecond(stackCount);
    }

    /**
     * 计算体魄蛊每秒的精力被动回复.
     *
     * @param stackCount 堆叠数
     * @return 每秒精力回复量
     */
    public double calculateTiPoGuJingliRecoveryPerSecond(int stackCount) {
      return HunPoRecoveryCalculator.calculateTiPoGuJingliRecoveryPerSecond(stackCount);
    }

    /**
     * 计算体魄蛊的子魂增益加成.
     *
     * @return 增益加成（10% = 0.10）
     */
    public double calculateZiHunIncreaseBonus() {
      return HunPoRecoveryCalculator.calculateZiHunIncreaseBonus();
    }
  }

  /** 技能计算操作. */
  public static final class SkillOps {
    private static final SkillOps INSTANCE = new SkillOps();

    private SkillOps() {}

    /**
     * 计算鬼雾的影响范围.
     *
     * @return 影响范围（方块）
     */
    public double calculateGuiWuRadius() {
      return GuiWuCalculator.calculateRadius();
    }

    /**
     * 计算鬼雾的影响范围平方.
     *
     * @return 影响范围平方
     */
    public double calculateGuiWuRadiusSquared() {
      return GuiWuCalculator.calculateRadiusSquared();
    }

    /**
     * 判断目标是否在鬼雾范围内.
     *
     * @param distanceSquared 距离平方
     * @return true 如果在范围内
     */
    public boolean isWithinGuiWuRange(double distanceSquared) {
      return GuiWuCalculator.isWithinRange(distanceSquared);
    }

    /**
     * 计算基于范围的衰减系数.
     *
     * @param distance 实际距离
     * @return 衰减系数（0.0 ~ 1.0）
     */
    public double calculateGuiWuFalloff(double distance) {
      return GuiWuCalculator.calculateFalloff(distance);
    }
  }
}
