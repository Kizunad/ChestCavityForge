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
                    multiplierCap == Double.POSITIVE_INFINITY ? "unlimited" : String.format("%.3f", multiplierCap)
            );
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
                    flatCap == Double.POSITIVE_INFINITY ? "unlimited" : String.format("%.3f", flatCap)
            );
        }
    }

    private static double clamp(double value, double cap) {
        if (cap == Double.POSITIVE_INFINITY) {
            return value;
        }
        double clamped = Math.max(-cap, Math.min(cap, value));
        return clamped;
    }
}
