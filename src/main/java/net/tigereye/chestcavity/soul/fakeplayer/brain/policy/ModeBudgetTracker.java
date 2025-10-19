package net.tigereye.chestcavity.soul.fakeplayer.brain.policy;

import java.util.Objects;

/**
 * Tracks per-tick budgets for mode switching and planning as defined by
 * {@link BudgetPolicy}. A zero or negative limit is treated as unlimited.
 */
public final class ModeBudgetTracker {

    private final BudgetPolicy policy;
    private long trackedTick = Long.MIN_VALUE;
    private int stateChangesThisTick;
    private int plansThisTick;

    public ModeBudgetTracker(BudgetPolicy policy) {
        this.policy = Objects.requireNonNull(policy, "policy");
    }

    public void beginTick(long gameTime) {
        if (gameTime != trackedTick) {
            trackedTick = gameTime;
            stateChangesThisTick = 0;
            plansThisTick = 0;
        }
    }

    public boolean canSwitch() {
        int max = policy.maxStateChangesPerTick();
        return max <= 0 || stateChangesThisTick < max;
    }

    public void recordSwitch() {
        stateChangesThisTick++;
    }

    public boolean canPlan() {
        int max = policy.maxPlansPerTick();
        return max <= 0 || plansThisTick < max;
    }

    public void recordPlan() {
        plansThisTick++;
    }

    public int stateChangesThisTick() {
        return stateChangesThisTick;
    }

    public int plansThisTick() {
        return plansThisTick;
    }

    public BudgetPolicy policy() {
        return policy;
    }

    public void reset() {
        trackedTick = Long.MIN_VALUE;
        stateChangesThisTick = 0;
        plansThisTick = 0;
    }
}
