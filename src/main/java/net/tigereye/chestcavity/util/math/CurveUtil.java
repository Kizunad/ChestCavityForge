package net.tigereye.chestcavity.util.math;

public final class CurveUtil {

  private CurveUtil() {}

  public static double expApproach(double ratio, double alpha) {
    double clampedRatio = Math.max(0.0D, ratio);
    double safeAlpha = Math.max(0.0D, alpha);
    if (safeAlpha == 0.0D) {
      return Math.min(1.0D, clampedRatio);
    }
    return 1.0D - Math.exp(-safeAlpha * clampedRatio);
  }

  public static double clamp(double value, double min, double max) {
    if (value < min) {
      return min;
    }
    if (value > max) {
      return max;
    }
    return value;
  }
}
