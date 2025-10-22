package net.tigereye.chestcavity.soul.fakeplayer.brain.scoring;

import java.util.Comparator;
import java.util.Objects;
import net.tigereye.chestcavity.soul.fakeplayer.brain.BrainMode;

/** Captures the utility evaluation for a {@link BrainMode} candidate. */
public record ModeScore(
    BrainMode mode,
    double utility,
    double rawUtility,
    double threat,
    double proximity,
    double effectiveHealth,
    boolean hasRegen,
    boolean inDanger,
    double bias) {
  public ModeScore {
    Objects.requireNonNull(mode, "mode");
    utility = clamp01(utility);
    rawUtility = clamp01(rawUtility);
    threat = clamp01(threat);
    proximity = clamp01(proximity);
    effectiveHealth = clamp01(effectiveHealth);
  }

  /** Returns whether the score suggests the soul currently has a threat target. */
  public boolean hasTargetLock() {
    return threat > 0.25 && proximity > 0.1;
  }

  /** Convenience accessor for the bias contribution (utility - raw). */
  public double biasContribution() {
    return clamp01(utility) - clamp01(rawUtility);
  }

  public static ModeScore zero(BrainMode mode, ScoreInputs inputs) {
    Objects.requireNonNull(inputs, "inputs");
    double health = clamp01(inputs.healthRatio());
    return new ModeScore(
        mode, 0.0, 0.0, 0.0, 0.0, health, inputs.hasRegen(), inputs.inDanger(), 0.0);
  }

  public static Comparator<ModeScore> byUtilityDescending() {
    return Comparator.comparingDouble(ModeScore::utility).reversed();
  }

  private static double clamp01(double value) {
    if (Double.isNaN(value)) {
      return 0.0;
    }
    return value < 0.0 ? 0.0 : Math.min(1.0, value);
  }
}
