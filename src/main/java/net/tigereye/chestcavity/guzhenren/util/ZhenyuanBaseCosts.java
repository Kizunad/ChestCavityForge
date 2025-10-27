package net.tigereye.chestcavity.guzhenren.util;

/**
 * 真元基础消耗常量与工具。
 *
 * <p>用途：为 {@code consumeScaledZhenyuan(baseCost)} 提供统一、可复用的 baseCost 取值来源。
 * 通过“设计基准阶段（转/阶段）× 单位用量（units）→ baseCost”的方式，便于开发为不同强度的技能定义一致的真元开销。
 *
 * <p>缩放公式由 GuzhenrenResourceBridge 统一实现： scaled = baseCost / D，其中
 * D = 2^(jieduan + zhuanshu*4) * zhuanshu * 3 / 96。
 * 因此若希望“在某一设计阶段消耗 units 点真元”，可以取 baseCost = units * D(designZhuanshu, designJieduan)。
 */
public final class ZhenyuanBaseCosts {

  private ZhenyuanBaseCosts() {}

  /** 每转包含的小阶段数。根据缩放实现，取 4（例如 初/中/高/圆满）。 */
  public static final int PHASES_PER_TURN = 4;

  // 预计算表（由 Python 生成），避免运行期 pow 计算：
  // D(r,j) = 2^(j + r*4) * r * 3 / 96 = r * 2^(4r + j - 5)
  static final int MAX_PRECOMP_R = 10; // r = 1..MAX_PRECOMP_R
  static final int MAX_PRECOMP_J = 4;  // j = 1..MAX_PRECOMP_J（阶段从1开始）
  private static final double[][] UNIT_FACTOR = new double[][] {
      /* r=1 */ new double[] {1.000000d, 2.000000d, 4.000000d, 8.000000d},
      /* r=2 */ new double[] {32.000000d, 64.000000d, 128.000000d, 256.000000d},
      /* r=3 */ new double[] {768.000000d, 1536.000000d, 3072.000000d, 6144.000000d},
      /* r=4 */ new double[] {16384.000000d, 32768.000000d, 65536.000000d, 131072.000000d},
      /* r=5 */ new double[] {327680.000000d, 655360.000000d, 1310720.000000d, 2621440.000000d},
      /* r=6 */ new double[] {6291456.000000d, 12582912.000000d, 25165824.000000d, 50331648.000000d},
      /* r=7 */ new double[] {117440512.000000d, 234881024.000000d, 469762048.000000d, 939524096.000000d},
      /* r=8 */ new double[] {2147483648.000000d, 4294967296.000000d, 8589934592.000000d, 17179869184.000000d},
      /* r=9 */ new double[] {38654705664.000000d, 77309411328.000000d, 154618822656.000000d, 309237645312.000000d},
      /* r=10 */ new double[] {687194767360.000000d, 1374389534720.000000d, 2748779069440.000000d, 5497558138880.000000d},
  };

  /**
   * 返回给定“转/阶段”的缩放分母 D。
   *
   * <p>scaled = baseCost / D。开发若想在该阶段消耗 {@code units} 真元，应使用
   * {@code baseCost = units * D}。
   */
  public static double unitFactor(int zhuanshu, int jieduan) {
    int r = Math.max(1, zhuanshu);
    int j = Math.max(1, jieduan);
    if (r <= MAX_PRECOMP_R && j <= MAX_PRECOMP_J) {
      return UNIT_FACTOR[r - 1][j - 1];
    }
    // 回退：极端高转/阶段时仍使用公式（不会频繁触发）
    double power = Math.pow(2.0, j + r * PHASES_PER_TURN);
    return power * r * 3.0 / 96.0;
  }

  /**
   * 计算在“设计转/阶段”下，为消耗 {@code units} 点真元所需的 baseCost 常量。
   *
   * <p>用法：将返回值直接传给 {@code consumeScaledZhenyuan(baseCost)}。
   */
  public static double baseForUnits(int designZhuanshu, int designJieduan, double units) {
    if (!(units > 0.0) || !Double.isFinite(units)) {
      return 0.0;
    }
    return units * unitFactor(designZhuanshu, designJieduan);
  }

  /**
   * 推荐的单位用量分级（可根据需要调整）。单位为“目标阶段的真元点数”。
   *
   * <p>例如：若希望“一转初期消耗 SMALL 等级（3 点）真元”，可以：
   * {@code baseForTier(1, 0, Tier.SMALL)}，并将结果传给 {@code consumeScaledZhenyuan}。
   */
  public enum Tier {
    TINY(1.0),
    SMALL(3.0),
    MEDIUM(6.0),
    HEAVY(12.0),
    BURST(24.0);

    public final double units;

    Tier(double units) {
      this.units = units;
    }
  }

  /** 便捷：按推荐分级返回 baseCost。 */
  public static double baseForTier(int designZhuanshu, int designJieduan, Tier tier) {
    if (tier == null) {
      return 0.0;
    }
    return baseForUnits(designZhuanshu, designJieduan, tier.units);
  }

  // 常用基准（单位 1.0）——各转初期(阶段1)；直接取预计算表值以避免再次计算。
  public static final double UNIT_AT_1Z1 = UNIT_FACTOR[0][0];
  public static final double UNIT_AT_2Z1 = UNIT_FACTOR[1][0];
  public static final double UNIT_AT_3Z1 = UNIT_FACTOR[2][0];
  public static final double UNIT_AT_4Z1 = UNIT_FACTOR[3][0];
  public static final double UNIT_AT_5Z1 = UNIT_FACTOR[4][0];
  public static final double UNIT_AT_6Z1 = UNIT_FACTOR[5][0];

  // 亦可按需使用完整阶段常量（示例：一转四阶段）。
  public static final double UNIT_AT_1Z2 = UNIT_FACTOR[0][1];
  public static final double UNIT_AT_1Z3 = UNIT_FACTOR[0][2];
  public static final double UNIT_AT_1Z4 = UNIT_FACTOR[0][3];
}
