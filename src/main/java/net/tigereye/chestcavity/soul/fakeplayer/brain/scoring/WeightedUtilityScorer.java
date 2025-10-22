package net.tigereye.chestcavity.soul.fakeplayer.brain.scoring;

/**
 * Simple weighted utility scorer. Produces a value in [0, 1] for each behaviour candidate based on
 * normalized inputs.
 */
public final class WeightedUtilityScorer {

  private final double wThreat;
  private final double wDistance;
  private final double wHealth;
  private final double wRegen;
  private final double wDanger;

  public WeightedUtilityScorer(
      double wThreat, double wDistance, double wHealth, double wRegen, double wDanger) {
    this.wThreat = wThreat;
    this.wDistance = wDistance;
    this.wHealth = wHealth;
    this.wRegen = wRegen;
    this.wDanger = wDanger;
  }

  /**
   * @param threat [0,1] Enemy presence/intensity
   * @param distance [0,1] Closeness to target (1 near)
   * @param health [0,1] Health ratio (1 healthy)
   * @param hasRegen whether regeneration is active
   * @param inDanger environment risk flag
   */
  public double score(
      double threat, double distance, double health, boolean hasRegen, boolean inDanger) {
    double regen = hasRegen ? 1.0 : 0.0;
    double danger = inDanger ? 1.0 : 0.0;
    double raw =
        wThreat * clamp01(threat)
            + wDistance * clamp01(distance)
            + wHealth * clamp01(1.0 - health)
            + wRegen * regen
            + wDanger * danger;
    // Normalize by total positive weights to keep within [0,1]
    double denom =
        Math.max(
            1e-6,
            Math.abs(wThreat)
                + Math.abs(wDistance)
                + Math.abs(wHealth)
                + Math.abs(wRegen)
                + Math.abs(wDanger));
    return clamp01(raw / denom);
  }

  private static double clamp01(double v) {
    return v < 0 ? 0 : (v > 1 ? 1 : v);
  }
}
