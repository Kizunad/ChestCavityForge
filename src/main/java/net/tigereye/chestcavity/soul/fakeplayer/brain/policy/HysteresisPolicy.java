package net.tigereye.chestcavity.soul.fakeplayer.brain.policy;

/**
 * Hysteresis policy applies a sticky bonus to the current choice to reduce flip-flopping.
 */
public final class HysteresisPolicy {

    private final double bonus;

    /** @param bonus additive bonus in [0,1] to keep current choice when close. */
    public HysteresisPolicy(double bonus) { this.bonus = Math.max(0.0, Math.min(1.0, bonus)); }

    /** Applies hysteresis: compares candidate score with current + bonus. */
    public boolean shouldSwitch(double currentScore, double candidateScore) {
        return candidateScore > (currentScore + bonus);
    }

    public double bonus() { return bonus; }
}

