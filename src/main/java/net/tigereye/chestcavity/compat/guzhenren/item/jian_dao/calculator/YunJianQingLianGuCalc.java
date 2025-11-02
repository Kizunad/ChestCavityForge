package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.calculator;

import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.tuning.YunJianQingLianGuTuning;
import net.tigereye.chestcavity.guzhenren.util.ZhenyuanBaseCosts;

/**
 * 蕴剑青莲蛊纯函数计算器
 *
 * <p>所有计算逻辑不依赖Minecraft复杂类型，便于单元测试。
 */
public final class YunJianQingLianGuCalc {

  private YunJianQingLianGuCalc() {}

  /**
   * 计算飞剑数量（基于域控系数）
   *
   * <p>数量范围：{@link YunJianQingLianGuTuning#MIN_SWORD_COUNT} -
   * {@link YunJianQingLianGuTuning#MAX_SWORD_COUNT}
   *
   * <p>计算公式：swordCount = BASE_COUNT × sqrt(pOut × weight)
   *
   * @param pOut 域控系数（sword_domain_p_out标签）
   * @return 飞剑数量
   */
  public static int calculateSwordCount(double pOut) {
    if (!Double.isFinite(pOut) || pOut < 0.0) {
      pOut = YunJianQingLianGuTuning.DEFAULT_P_OUT;
    }

    // 域控系数越高，飞剑越多
    // pOut=5.0 → count=8 (基础)
    // pOut=20.0 → count=16
    // pOut=80.0 → count=32 (上限)
    double rawCount =
        YunJianQingLianGuTuning.BASE_SWORD_COUNT
            * Math.sqrt(pOut * YunJianQingLianGuTuning.SWORD_COUNT_P_OUT_WEIGHT);
    int count = (int) Math.round(rawCount);

    return Math.max(
        YunJianQingLianGuTuning.MIN_SWORD_COUNT,
        Math.min(YunJianQingLianGuTuning.MAX_SWORD_COUNT, count));
  }

  /**
   * 计算飞剑环绕半径（飞剑数量越多，半径越小）
   *
   * <p>维持莲瓣紧凑度：数量翻倍时半径缩小约30%。
   *
   * @param swordCount 飞剑数量
   * @return 环绕半径（方块）
   */
  public static double calculateOrbitRadius(int swordCount) {
    if (swordCount <= 0) {
      swordCount = YunJianQingLianGuTuning.BASE_SWORD_COUNT;
    }

    // 数量越多，越紧凑
    // count=8 → radius=3.0 (基础)
    // count=16 → radius=2.12
    // count=32 → radius=1.5
    double factor =
        Math.sqrt(
            (double) YunJianQingLianGuTuning.BASE_SWORD_COUNT / (double) swordCount);
    return YunJianQingLianGuTuning.ORBIT_RADIUS_BASE * factor;
  }

  /**
   * 计算青莲剑域半径缩放系数（基于域控系数）
   *
   * <p>领域半径 = BASE_RADIUS × radiusScale
   *
   * @param pOut 域控系数
   * @return 半径缩放系数
   */
  public static double calculateDomainRadiusScale(double pOut) {
    if (!Double.isFinite(pOut) || pOut < 0.0) {
      pOut = YunJianQingLianGuTuning.DEFAULT_P_OUT;
    }

    // 域控系数越高，领域越大
    // pOut=5.0 → scale=1.0 (基础20格)
    // pOut=20.0 → scale=1.41 (28格)
    return Math.sqrt(pOut * YunJianQingLianGuTuning.DOMAIN_RADIUS_P_OUT_WEIGHT);
  }

  /**
   * 计算飞剑伤害增幅（基于域控系数）
   *
   * @param pOut 域控系数
   * @return 伤害倍率
   */
  public static double calculateSwordDamageMult(double pOut) {
    if (!Double.isFinite(pOut) || pOut < 0.0) {
      pOut = YunJianQingLianGuTuning.DEFAULT_P_OUT;
    }

    // 直接使用pOut作为伤害倍率
    // pOut=5.0 → 500%伤害
    // pOut=10.0 → 1000%伤害
    return pOut;
  }

  /**
   * 计算持续消耗的真元量（每秒）
   *
   * <p>使用BURST Tier，适合五转大招级别。
   *
   * @return 每秒真元消耗（基础值）
   */
  public static double zhenyuanPerSecond() {
    return ZhenyuanBaseCosts.baseForTier(
        YunJianQingLianGuTuning.DESIGN_ZHUANSHU,
        YunJianQingLianGuTuning.DESIGN_JIEDUAN,
        YunJianQingLianGuTuning.ZHENYUAN_TIER);
  }

  /**
   * 计算青莲护体的真元消耗
   *
   * @param damage 受到的伤害值
   * @return 需要消耗的真元量
   */
  public static double calculateShieldCost(double damage) {
    if (!Double.isFinite(damage) || damage < 0.0) {
      return 0.0;
    }
    return damage * YunJianQingLianGuTuning.SHIELD_COST_MULT;
  }

  /**
   * 判断是否为致命一击
   *
   * @param currentHealth 当前生命值
   * @param incomingDamage 即将受到的伤害
   * @return true 如果受伤后生命值低于阈值
   */
  public static boolean isLethalDamage(float currentHealth, float incomingDamage) {
    return (currentHealth - incomingDamage) <= YunJianQingLianGuTuning.SHIELD_LETHAL_THRESHOLD;
  }
}
