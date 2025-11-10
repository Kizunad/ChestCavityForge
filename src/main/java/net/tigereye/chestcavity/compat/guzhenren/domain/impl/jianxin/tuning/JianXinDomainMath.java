package net.tigereye.chestcavity.compat.guzhenren.domain.impl.jianxin.tuning;

/** 剑心域“域控系数”核心数学，纯函数实现，便于单元测试与复用。 */
public final class JianXinDomainMath {

  private JianXinDomainMath() {}

  public static final double R_MIN = 1.0; // 最小半径（一格）
  public static final double ALPHA = 1.3; // 输出幂指数
  public static final double BETA = 1.0; // 防御幂指数

  /** Rmax = 5 + floor(sqrt(道痕/200)) + floor(经验/3) （上限11）。 */
  public static int computeRmax(int jiandaoDaohen, int schoolExperience) {
    int base = 5;
    int term1 = (int) Math.floor(Math.sqrt(Math.max(0.0, jiandaoDaohen) / 200.0));
    int term2 = (int) Math.floor(Math.max(0, schoolExperience) / 3.0);
    int rmax = base + term1 + term2;
    return Math.max(1, Math.min(11, rmax));
  }

  /** 归一化收缩度：s = (Rmax - (R - Rmin)) / (Rmax - Rmin)，s∈[0,1]。 */
  public static double computeS(double R, double Rmax) {
    double r = Math.max(R_MIN, R);
    double rmax = Math.max(R_MIN, Rmax);
    if (rmax <= R_MIN) return 1.0; // 退化为最小域
    double s = (rmax - (r - R_MIN)) / (rmax - R_MIN);
    return clamp01(s);
  }

  /** P_out = 0.1 + (5.0 - 0.1) * s^alpha （强制地板0.1）。 */
  public static double computePout(double s) {
    double v = 0.1 + (5.0 - 0.1) * Math.pow(clamp01(s), ALPHA);
    return Math.max(0.1, v);
  }

  /** P_in = 1 - min(0.6, 0.15 + 0.45 * s^beta) → 最多 -60% 实伤。 */
  public static double computePin(double s) {
    double cut = 0.15 + 0.45 * Math.pow(clamp01(s), BETA);
    double capped = Math.min(0.6, cut);
    return 1.0 - capped;
  }

  /** P_move = 1 + 0.10 * s。 */
  public static double computePmove(double s) {
    return 1.0 + 0.10 * clamp01(s);
  }

  /** 实体门控 E = clamp(s - 0.2, 0, 1)。 */
  public static double computeEntityGate(double s) {
    return clamp01(clamp(s - 0.2, 0.0, 1.0));
  }

  /** 实体输出倍率：P_out_entity = min(1 + (P_out - 1) * E, 1.8)。 */
  public static double computePoutEntity(double pOut, double E) {
    double v = 1.0 + (pOut - 1.0) * clamp01(E);
    return Math.min(1.8, Math.max(0.0, v));
  }

  /** 被动扣费（每2秒）：drain = 2 + 0.08 * (πR^2) + 0.8 * s。 */
  public static double computePassiveDrainPer2s(double R, double s) {
    double area = Math.PI * R * R;
    return 2.0 + 0.08 * area + 0.8 * clamp01(s);
  }

  /** 主动期间每秒扣费：drain_active = 0.06 * (πR^2) + 0.5 * s。 */
  public static double computeActiveDrainPerSec(double R, double s) {
    double area = Math.PI * R * R;
    return 0.06 * area + 0.5 * clamp01(s);
  }

  /** 持续 = 8 + 1.2 * 层数（最大14s）。 */
  public static double computeActiveDurationSec(int layers) {
    double sec = 8.0 + 1.2 * Math.max(0, layers);
    return Math.min(14.0, sec);
  }

  /** 冷却 = 25 - 1.5 * 层数（最小16s）。 */
  public static double computeActiveCooldownSec(int layers) {
    double sec = 25.0 - 1.5 * Math.max(0, layers);
    return Math.max(16.0, sec);
  }

  /** 小域偏置：当 R≤2 时，P_out *= (1 + 0.03*层数)（上限 +15%）。 */
  public static double applySmallDomainBias(double pOut, double R, int layers) {
    if (!(R <= 2.0)) return pOut;
    double factor = 1.0 + 0.03 * Math.max(0, layers);
    factor = Math.min(1.15, factor);
    return Math.max(0.0, pOut * factor);
  }

  public static double clamp01(double v) {
    return clamp(v, 0.0, 1.0);
  }

  public static double clamp(double v, double lo, double hi) {
    return Math.max(lo, Math.min(hi, v));
  }
}
