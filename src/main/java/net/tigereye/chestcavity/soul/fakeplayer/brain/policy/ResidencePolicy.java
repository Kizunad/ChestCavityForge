package net.tigereye.chestcavity.soul.fakeplayer.brain.policy;

import net.tigereye.chestcavity.soul.fakeplayer.brain.BrainMode;

/**
 * Minimum residence time for a brain mode to avoid thrashing.
 */
public final class ResidencePolicy {

    private final int minTicks;

    public ResidencePolicy(int minTicks) { this.minTicks = Math.max(0, minTicks); }

    /** Whether we can switch away from the current mode given elapsed ticks. */
    public boolean canLeave(BrainMode mode, int elapsedTicks) { return elapsedTicks >= minTicks; }

    public int minTicks() { return minTicks; }
}

