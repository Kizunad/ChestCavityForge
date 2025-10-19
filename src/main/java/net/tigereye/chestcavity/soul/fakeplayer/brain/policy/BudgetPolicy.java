package net.tigereye.chestcavity.soul.fakeplayer.brain.policy;

/**
 * Limits state changes and planning work per tick to avoid spikes.
 */
public final class BudgetPolicy {

    private final int maxStateChangesPerTick;
    private final int maxPlansPerTick;

    public BudgetPolicy(int maxStateChangesPerTick, int maxPlansPerTick) {
        this.maxStateChangesPerTick = Math.max(0, maxStateChangesPerTick);
        this.maxPlansPerTick = Math.max(0, maxPlansPerTick);
    }

    public int maxStateChangesPerTick() { return maxStateChangesPerTick; }
    public int maxPlansPerTick() { return maxPlansPerTick; }
}

