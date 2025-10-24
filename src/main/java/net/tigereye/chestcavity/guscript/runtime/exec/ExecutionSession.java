package net.tigereye.chestcavity.guscript.runtime.exec;

import net.tigereye.chestcavity.ChestCavity;

/**
 * Tracks cumulative damage modifiers exported by GuScript roots within a single trigger dispatch.
 */
public final class ExecutionSession {

  private final double multiplierCap;
  private final double flatCap;
  private double cumulativeMultiplier;
  private double cumulativeFlat;
  private double currentTimeScaleMultiplier = 1.0D;
  private double currentTimeScaleFlat = 0.0D;

  public ExecutionSession(double multiplierCap, double flatCap) {
    this.multiplierCap = sanitizeCap(multiplierCap);
    this.flatCap = sanitizeCap(flatCap);
  }

  private static double sanitizeCap(double value) {
    if (Double.isNaN(value) || Double.isInfinite(value) || value <= 0.0D) {
      return Double.POSITIVE_INFINITY;
    }
    return Math.abs(value);
  }

  public double currentMultiplier() {
    return cumulativeMultiplier;
  }

  public double currentFlat() {
    return cumulativeFlat;
  }

  public void exportMultiplier(double delta) {
    if (Double.isNaN(delta) || delta == 0.0D) {
      return;
    }
    double before = cumulativeMultiplier;
    cumulativeMultiplier = clamp(before + delta, multiplierCap);
    if (cumulativeMultiplier != before + delta) {
      ChestCavity.LOGGER.info(
          "[GuScript] Execution session clamped multiplier export {} -> {} (cap {})",
          before + delta,
          cumulativeMultiplier,
          multiplierCap == Double.POSITIVE_INFINITY
              ? "unlimited"
              : String.format("%.3f", multiplierCap));
    }
  }

  public void exportFlat(double delta) {
    if (Double.isNaN(delta) || delta == 0.0D) {
      return;
    }
    double before = cumulativeFlat;
    cumulativeFlat = clamp(before + delta, flatCap);
    if (cumulativeFlat != before + delta) {
      ChestCavity.LOGGER.info(
          "[GuScript] Execution session clamped flat export {} -> {} (cap {})",
          before + delta,
          cumulativeFlat,
          flatCap == Double.POSITIVE_INFINITY ? "unlimited" : String.format("%.3f", flatCap));
    }
  }

  public void exportTimeScaleMultiplier(double multiplier) {
    if (Double.isNaN(multiplier) || Double.isInfinite(multiplier) || multiplier <= 0.0D) {
      return;
    }
    currentTimeScaleMultiplier *= multiplier;
  }

  public void exportTimeScaleFlat(double amount) {
    if (Double.isNaN(amount) || Double.isInfinite(amount) || amount == 0.0D) {
      return;
    }
    currentTimeScaleFlat += amount;
  }

  public double currentTimeScaleMultiplier() {
    return currentTimeScaleMultiplier;
  }

  public double currentTimeScaleFlat() {
    return currentTimeScaleFlat;
  }

  public double currentTimeScale() {
    double combined = currentTimeScaleMultiplier + currentTimeScaleFlat;
    if (Double.isNaN(combined) || Double.isInfinite(combined)) {
      return 1.0D;
    }
    return combined;
  }

  private static double clamp(double value, double cap) {
    if (cap == Double.POSITIVE_INFINITY) {
      return value;
    }
    double clamped = Math.max(-cap, Math.min(cap, value));
    return clamped;
  }
}
