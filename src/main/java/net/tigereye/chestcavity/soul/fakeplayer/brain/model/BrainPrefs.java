package net.tigereye.chestcavity.soul.fakeplayer.brain.model;

/**
 * Volatile (non-persisted) preferences for the current session. Persistence,
 * if needed, should be handled by SoulContainer with a compact NBT form.
 */
public final class BrainPrefs {
    private boolean allowExploration = true;
    private boolean allowLooting = false;
    private double followDistance = 8.0;

    public boolean allowExploration() { return allowExploration; }
    public boolean allowLooting() { return allowLooting; }
    public double followDistance() { return followDistance; }

    public BrainPrefs setAllowExploration(boolean v) { this.allowExploration = v; return this; }
    public BrainPrefs setAllowLooting(boolean v) { this.allowLooting = v; return this; }
    public BrainPrefs setFollowDistance(double v) { this.followDistance = Math.max(1.0, v); return this; }
}

