package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.calculator;

import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.tuning.JianQiGuTuning;

/**
 * 剑气蛊威能与衰减计算器。
 *
 * <p>提供纯函数式的威能计算逻辑：
 * <ul>
 *   <li>初始伤害计算（含道痕/流派/断势加成）</li>
 *   <li>命中后衰减计算</li>
 *   <li>终止判定</li>
 * </ul>
 */
public final class JianQiGuCalc {

  private JianQiGuCalc() {}

  /**
   * 计算「一斩开天」的初始威能。
   *
   * <p>公式：Damage = Base × (1 + 道痕系数 + 流派经验系数 + 断势加成)
   *
   * @param daohen 剑道道痕
   * @param liupaiExp 剑道流派经验
   * @param duanshiTriggers 断势触发次数（每次+25%威能）
   * @return 初始威能
   */
  public static double computeInitialDamage(
      double daohen, double liupaiExp, int duanshiTriggers) {

    // 道痕加成：改为对数增长，取消硬上限，避免线性爆炸
    double daohenBonus = Math.log1p(Math.max(0.0, daohen) / JianQiGuTuning.DAOHEN_DAMAGE_DIV);

    // 流派经验加成
    double liupaiBonus =
        Math.min(
            liupaiExp / JianQiGuTuning.LIUPAI_DAMAGE_DIV, JianQiGuTuning.LIUPAI_DAMAGE_MAX);

    // 断势加成（每次触发 +25%）
    double duanshiBonus = duanshiTriggers * JianQiGuTuning.DUANSHI_POWER_BONUS;

    // 总倍率
    double totalMult = 1.0 + daohenBonus + liupaiBonus + duanshiBonus;

    return JianQiGuTuning.BASE_DAMAGE * totalMult;
  }

  /**
   * 计算命中后的威能。
   *
   * <p>衰减公式：
   * <ul>
   *   <li>若 hitIndex ≤ decayGrace：不衰减</li>
   *   <li>否则：damage × (1 - decayRate)^(hitIndex - decayGrace)</li>
   * </ul>
   *
   * @param initialDamage 初始威能
   * @param hitIndex 当前命中次数（从1开始计数）
   * @param decayRate 衰减率（每次命中）
   * @param decayGrace 豁免次数（断势提供）
   * @return 当前威能
   */
  public static double computeDamageAfterHit(
      double initialDamage, int hitIndex, double decayRate, int decayGrace) {

    if (hitIndex <= decayGrace) {
      // 在豁免期内，不衰减
      return initialDamage;
    }

    // 计算有效衰减次数
    int effectiveDecays = hitIndex - decayGrace;

    // 应用衰减
    double decayFactor = Math.pow(1.0 - decayRate, effectiveDecays);
    return initialDamage * decayFactor;
  }

  /**
   * 判断是否应该终止剑气。
   *
   * @param currentDamage 当前威能
   * @param initialDamage 初始威能
   * @param minRatio 最小威能比例阈值
   * @return 是否应该终止
   */
  public static boolean shouldTerminate(
      double currentDamage, double initialDamage, double minRatio) {
    if (initialDamage <= 0.0) {
      return true;
    }
    double ratio = currentDamage / initialDamage;
    return ratio < minRatio;
  }

  /**
   * 计算断势触发次数。
   *
   * @param currentStacks 当前断势层数
   * @return 触发次数（每3层触发1次）
   */
  public static int computeDuanshiTriggers(int currentStacks) {
    return currentStacks / JianQiGuTuning.DUANSHI_STACK_THRESHOLD;
  }

  /**
   * 计算断势提供的衰减豁免次数。
   *
   * @param triggers 断势触发次数
   * @return 豁免次数
   */
  public static int computeDecayGrace(int triggers) {
    return triggers * JianQiGuTuning.DUANSHI_DECAY_GRACE;
  }

  /**
   * 依据道痕提供额外的衰减豁免（“耐久度”）。
   *
   * <p>采用对数增长避免极端爆炸：extra = floor(log1p(daohen / div))。
   */
  public static int computeExtraGraceByDaohen(double daohen) {
    if (!(daohen > 0.0)) {
      return 0;
    }
    double v = Math.log1p(daohen / JianQiGuTuning.DAOHEN_GRACE_DIV);
    if (!Double.isFinite(v) || v <= 0.0) {
      return 0;
    }
    return (int) Math.floor(v);
  }
}
