package net.tigereye.chestcavity.compat.guzhenren.item.tian_dao.calculator;

/**
 * 天道·寿蛊 纯计算模块：只做数值计算，不接触 Minecraft 运行时。
 */
public final class ShouGuCalculator {

  private ShouGuCalculator() {}

  /** 计算当前利息倍率。jinliao（禁疗）下倍率加倍。 */
  public static double interestRate(
      double baseRate, double reductionPerMark, int marks, boolean jinliao) {
    double r = Math.max(0.0, baseRate - Math.max(0, marks) * Math.max(0.0, reductionPerMark));
    if (jinliao) r *= 2.0;
    return r;
  }

  /** 按利息倍率计息。 */
  public static double applyInterest(double currentDebt, double rate) {
    if (!(currentDebt > 0.0) || !(rate > 0.0)) return Math.max(0.0, currentDebt);
    return Math.max(0.0, currentDebt + currentDebt * rate);
  }

  /** 计算寿债阈值（基础 + 每层寿纹增量）。 */
  public static double computeDebtThreshold(int base, int perMark, int marks) {
    long result = (long) base + (long) Math.max(0, marks) * (long) perMark;
    if (result < 0L) result = 0L; // 防溢出保护
    return (double) result;
  }

  /** 主动技期间的单次治疗量。 */
  public static double healPerTick(double basePerSecond, double perConsumed, int consumedMarks) {
    return Math.max(0.0, basePerSecond + Math.max(0, consumedMarks) * Math.max(0.0, perConsumed));
  }

  /** 计算下一次治疗的触发时间（tick）。 */
  public static long nextHealTick(long now, long intervalTicks) {
    long i = Math.max(1L, intervalTicks);
    long n = now + i;
    return n < 0 ? Long.MAX_VALUE : n;
  }

  /** 环境减伤因子：1 - marks * perMark，钳制到 [0,1]。 */
  public static double environmentReductionFactor(double perMark, int marks) {
    double f = 1.0 - Math.max(0, marks) * Math.max(0.0, perMark);
    if (f < 0.0) return 0.0;
    if (f > 1.0) return 1.0;
    return f;
  }

  /** 寿纹下一次生成时间（根据是否在战斗）。 */
  public static long nextMarkTick(long now, boolean inCombat, long intervalCombat, long intervalOut) {
    long interval = Math.max(1L, inCombat ? intervalCombat : intervalOut);
    long n = now + interval;
    return n < 0 ? Long.MAX_VALUE : n;
  }

  /**
   * 计算本次偿债量：
   * repay = clampToDebt( (base + perMark*marks) * (jinliao ? jinliaoMul : 1) , debt )。
   */
  public static double computeRepay(
      double base, double perMark, int marks, boolean jinliao, double jinliaoMul, double currentDebt) {
    double r = Math.max(0.0, base + Math.max(0, marks) * Math.max(0.0, perMark));
    if (jinliao) r *= Math.max(0.0, jinliaoMul);
    if (!(currentDebt > 0.0)) return 0.0;
    if (!Double.isFinite(r)) return 0.0;
    return Math.max(0.0, Math.min(r, currentDebt));
  }
}
