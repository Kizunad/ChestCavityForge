package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.calculator;

import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.calculator.common.HunDaoCalcContext;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.calculator.damage.HunDaoDamageCalculator;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.calculator.damage.HunDaoDotCalculator;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.calculator.resource.HunPoDrainCalculator;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.calculator.resource.HunPoRecoveryCalculator;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.calculator.skill.GuiWuCalculator;

/**
 * 魂道战斗计算器门面（Facade）。
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
 * HunDaoCalcContext context = HunDaoCalcContext.create(currentHunpo, maxHunpo, efficiency, stackCount);
 * double soulFlameDps = HunDaoCombatCalculator.damage().calculateSoulFlameDps(context);
 * float trueDamage = HunDaoCombatCalculator.damage().calculateGuiQiGuTrueDamage(context);
 * double leakPerSec = HunDaoCombatCalculator.resource().calculateLeakPerSecond();
 * }</pre>
 *
 * <p>Phase 4: Combat & Calculator
 */
public final class HunDaoCombatCalculator {

  private HunDaoCombatCalculator() {}

  /**
   * 获取伤害计算器（真实伤害、附加伤害、护盾）。
   *
   * @return 伤害计算器
   */
  public static DamageOps damage() {
    return DamageOps.INSTANCE;
  }

  /**
   * 获取 DoT 计算器（魂焰持续伤害）。
   *
   * @return DoT 计算器
   */
  public static DotOps dot() {
    return DotOps.INSTANCE;
  }

  /**
   * 获取资源计算器（魂魄泄露、回复）。
   *
   * @return 资源计算器
   */
  public static ResourceOps resource() {
    return ResourceOps.INSTANCE;
  }

  /**
   * 获取技能计算器（鬼雾范围、效果）。
   *
   * @return 技能计算器
   */
  public static SkillOps skill() {
    return SkillOps.INSTANCE;
  }

  // ===== Nested Operation Classes =====

  /** 伤害计算操作。 */
  public static final class DamageOps {
    private static final DamageOps INSTANCE = new DamageOps();

    private DamageOps() {}

    public float calculateGuiQiGuTrueDamage(HunDaoCalcContext context) {
      return HunDaoDamageCalculator.calculateGuiQiGuTrueDamage(context);
    }

    public float calculateTiPoGuSoulBeastDamage(HunDaoCalcContext context) {
      return HunDaoDamageCalculator.calculateTiPoGuSoulBeastDamage(context);
    }

    public double calculateTiPoGuHunpoCost(double maxHunpo) {
      return HunDaoDamageCalculator.calculateTiPoGuHunpoCost(maxHunpo);
    }

    public float calculateTiPoGuShield(HunDaoCalcContext context) {
      return HunDaoDamageCalculator.calculateTiPoGuShield(context);
    }

    public double calculateAdjustedAttackCost(double baseCost, double reduction) {
      return HunDaoDamageCalculator.calculateAdjustedAttackCost(baseCost, reduction);
    }
  }

  /** DoT 计算操作。 */
  public static final class DotOps {
    private static final DotOps INSTANCE = new DotOps();

    private DotOps() {}

    public double calculateSoulFlameDps(HunDaoCalcContext context) {
      return HunDaoDotCalculator.calculateSoulFlameDps(context);
    }

    public int getSoulFlameDurationSeconds() {
      return HunDaoDotCalculator.getSoulFlameDurationSeconds();
    }

    public int getSoulFlameDurationTicks() {
      return HunDaoDotCalculator.getSoulFlameDurationTicks();
    }

    public double calculateSoulFlameTotalDamage(HunDaoCalcContext context) {
      return HunDaoDotCalculator.calculateSoulFlameTotalDamage(context);
    }

    public double calculateDamagePerTick(double dps) {
      return HunDaoDotCalculator.calculateDamagePerTick(dps);
    }
  }

  /** 资源计算操作。 */
  public static final class ResourceOps {
    private static final ResourceOps INSTANCE = new ResourceOps();

    private ResourceOps() {}

    // Drain operations
    public double calculateLeakPerSecond() {
      return HunPoDrainCalculator.calculateLeakPerSecond();
    }

    public double calculateLeakPerTick() {
      return HunPoDrainCalculator.calculateLeakPerTick();
    }

    public double calculateAttackCost() {
      return HunPoDrainCalculator.calculateAttackCost();
    }

    public boolean canAfford(double currentHunpo, double cost) {
      return HunPoDrainCalculator.canAfford(currentHunpo, cost);
    }

    public double calculateRemainingSeconds(double currentHunpo) {
      return HunPoDrainCalculator.calculateRemainingSeconds(currentHunpo);
    }

    public long calculateRemainingTicks(double currentHunpo) {
      return HunPoDrainCalculator.calculateRemainingTicks(currentHunpo);
    }

    // Recovery operations
    public double calculateXiaoHunGuBaseRecovery() {
      return HunPoRecoveryCalculator.calculateXiaoHunGuBaseRecovery();
    }

    public double calculateXiaoHunGuBonusRecovery() {
      return HunPoRecoveryCalculator.calculateXiaoHunGuBonusRecovery();
    }

    public double calculateXiaoHunGuTotalRecovery() {
      return HunPoRecoveryCalculator.calculateXiaoHunGuTotalRecovery();
    }

    public double calculateDaHunGuHunpoRecovery() {
      return HunPoRecoveryCalculator.calculateDaHunGuHunpoRecovery();
    }

    public double calculateDaHunGuNiantouGeneration() {
      return HunPoRecoveryCalculator.calculateDaHunGuNiantouGeneration();
    }

    public double calculateGuiQiGuHunpoRecoveryPerSecond(int stackCount) {
      return HunPoRecoveryCalculator.calculateGuiQiGuHunpoRecoveryPerSecond(stackCount);
    }

    public double calculateGuiQiGuJingliRecoveryPerSecond(int stackCount) {
      return HunPoRecoveryCalculator.calculateGuiQiGuJingliRecoveryPerSecond(stackCount);
    }

    public double calculateTiPoGuHunpoRecoveryPerSecond(int stackCount) {
      return HunPoRecoveryCalculator.calculateTiPoGuHunpoRecoveryPerSecond(stackCount);
    }

    public double calculateTiPoGuJingliRecoveryPerSecond(int stackCount) {
      return HunPoRecoveryCalculator.calculateTiPoGuJingliRecoveryPerSecond(stackCount);
    }

    public double calculateZiHunIncreaseBonus() {
      return HunPoRecoveryCalculator.calculateZiHunIncreaseBonus();
    }
  }

  /** 技能计算操作。 */
  public static final class SkillOps {
    private static final SkillOps INSTANCE = new SkillOps();

    private SkillOps() {}

    public double calculateGuiWuRadius() {
      return GuiWuCalculator.calculateRadius();
    }

    public double calculateGuiWuRadiusSquared() {
      return GuiWuCalculator.calculateRadiusSquared();
    }

    public boolean isWithinGuiWuRange(double distanceSquared) {
      return GuiWuCalculator.isWithinRange(distanceSquared);
    }

    public double calculateGuiWuFalloff(double distance) {
      return GuiWuCalculator.calculateFalloff(distance);
    }
  }
}
