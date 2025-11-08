package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.calculator;

import java.util.List;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.tuning.SuiRenGuBalance;

/**
 * 碎刃蛊计算器：道痕增幅计算与持续时间计算。
 *
 * <p>核心公式：
 * <pre>
 *   单剑道痕增幅 Δ = ALPHA_EXP * clamp(exp, 0, E_CAP) + BETA_ATTR * W
 *   其中 W = W_D * ^D + W_A * ^A + W_S * ^S
 *   ^D = min(maxDur / D_REF, 1.0)，其他同理
 * </pre>
 *
 * <p>单次施放总增幅 = min(Σ min(Δ_i, PER_SWORD_CAP), CAST_TOTAL_CAP)
 * <p>持续时间 = clamp(BASE + count * DURATION_PER_SWORD, 0, DURATION_CAP)
 */
public final class SuiRenGuCalc {

  private SuiRenGuCalc() {}

  /**
   * 飞剑属性统计（用于传递参数）。
   */
  public static class SwordStats {
    public final int experience;
    public final double maxDurability;
    public final double maxAttack;
    public final double maxSpeed;

    public SwordStats(int experience, double maxDurability, double maxAttack, double maxSpeed) {
      this.experience = experience;
      this.maxDurability = maxDurability;
      this.maxAttack = maxAttack;
      this.maxSpeed = maxSpeed;
    }
  }

  /**
   * 计算单把飞剑能提供的道痕增幅（Δ）。
   *
   * @param exp 飞剑经验
   * @param maxDur 飞剑最大耐久
   * @param maxAtk 飞剑最大攻击
   * @param maxSpeed 飞剑最大速度
   * @return 道痕增幅值（未 cap）
   */
  public static int deltaForSword(int exp, double maxDur, double maxAtk, double maxSpeed) {
    // 1. 经验部分：clamp 到 E_CAP
    int e = Math.max(0, Math.min(exp, SuiRenGuBalance.E_CAP));
    double expTerm = SuiRenGuBalance.ALPHA_EXP * e;

    // 2. 属性归一化
    double normD = Math.min(maxDur / SuiRenGuBalance.D_REF, 1.0);
    double normA = Math.min(maxAtk / SuiRenGuBalance.A_REF, 1.0);
    double normS = Math.min(maxSpeed / SuiRenGuBalance.S_REF, 1.0);

    // 3. 加权属性评分
    double W = SuiRenGuBalance.W_D * normD
             + SuiRenGuBalance.W_A * normA
             + SuiRenGuBalance.W_S * normS;
    double attrTerm = SuiRenGuBalance.BETA_ATTR * W;

    // 4. 总增幅
    double delta = expTerm + attrTerm;
    return (int) Math.round(delta);
  }

  /**
   * 计算单把飞剑能提供的道痕增幅（使用 SwordStats）。
   */
  public static int deltaForSword(SwordStats stats) {
    return deltaForSword(stats.experience, stats.maxDurability, stats.maxAttack, stats.maxSpeed);
  }

  /**
   * 计算本次施放的总道痕增幅。
   *
   * @param statsList 参与牺牲的所有飞剑属性列表
   * @return 总增幅（应用单剑上限与总上限）
   */
  public static int totalForCast(List<SwordStats> statsList) {
    int total = 0;
    for (SwordStats stats : statsList) {
      int delta = deltaForSword(stats);
      // 单把剑上限
      int capped = Math.min(delta, SuiRenGuBalance.PER_SWORD_CAP);
      total += capped;
    }
    // 总上限
    return Math.min(total, SuiRenGuBalance.CAST_TOTAL_CAP);
  }

  /**
   * 计算本次施放的 buff 持续时间（ticks）。
   *
   * @param swordCount 牺牲的飞剑数量
   * @return 持续时间（ticks）
   */
  public static int durationForCast(int swordCount) {
    int duration = SuiRenGuBalance.BASE_DURATION_TICKS
                 + swordCount * SuiRenGuBalance.DURATION_PER_SWORD;
    return Math.max(0, Math.min(duration, SuiRenGuBalance.DURATION_CAP));
  }
}
