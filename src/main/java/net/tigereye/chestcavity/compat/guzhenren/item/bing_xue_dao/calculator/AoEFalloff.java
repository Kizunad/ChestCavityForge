package net.tigereye.chestcavity.compat.guzhenren.item.bing_xue_dao.calculator;

/** AoE伤害衰减计算工具 */
public final class AoEFalloff {
  private AoEFalloff() {}

  /** 线性衰减：d=0时1.0；d>=r时0.0；中间为 1 - d/r。 */
  public static double linear(double distance, double radius) {
    if (radius <= 0.0) return 0.0;
    double ratio = 1.0 - (Math.max(0.0, distance) / radius);
    if (ratio < 0.0) return 0.0;
    if (ratio > 1.0) return 1.0;
    return ratio;
  }
}

