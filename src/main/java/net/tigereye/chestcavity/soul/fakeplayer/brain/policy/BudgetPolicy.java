package net.tigereye.chestcavity.soul.fakeplayer.brain.policy;

/** Limits state changes and planning work per tick to avoid spikes. */
public final class BudgetPolicy {

  private final int maxStateChangesPerTick;
  private final int maxPlansPerTick;

  private long lastTick = Long.MIN_VALUE;
  private int stateChangesUsed;
  private int plansUsed;

  public BudgetPolicy(int maxStateChangesPerTick, int maxPlansPerTick) {
    this.maxStateChangesPerTick = Math.max(0, maxStateChangesPerTick);
    this.maxPlansPerTick = Math.max(0, maxPlansPerTick);
  }

  /** Ensure counters are reset when the tick advances. */
  private void roll(long tick) {
    if (tick != lastTick) {
      lastTick = tick;
      stateChangesUsed = 0;
      plansUsed = 0;
    }
  }

  /**
   * Attempts to consume a state change budget for the given tick.
   *
   * @param tick current level tick
   * @return true if budget available and consumed
   */
  public boolean tryConsumeStateChange(long tick) {
    roll(tick);
    if (stateChangesUsed >= maxStateChangesPerTick) {
      return false;
    }
    stateChangesUsed++;
    return true;
  }

  /** Attempts to consume a single planning budget entry for the given tick. */
  public boolean tryConsumePlan(long tick) {
    return tryConsumePlans(tick, 1);
  }

  /**
   * Attempts to consume {@code count} plan budget entries for the given tick.
   *
   * @param tick current level tick
   * @param count number of plans required (non-negative)
   * @return true if budget available and consumed
   */
  public boolean tryConsumePlans(long tick, int count) {
    roll(tick);
    if (count <= 0) {
      return true;
    }
    if ((plansUsed + count) > maxPlansPerTick) {
      return false;
    }
    plansUsed += count;
    return true;
  }

  /** Remaining state change capacity for the given tick. */
  public int remainingStateChanges(long tick) {
    roll(tick);
    return maxStateChangesPerTick - stateChangesUsed;
  }

  /** Remaining planning capacity for the given tick. */
  public int remainingPlans(long tick) {
    roll(tick);
    return maxPlansPerTick - plansUsed;
  }

  /** Resets usage counters, e.g. on full brain reset. */
  public void reset() {
    lastTick = Long.MIN_VALUE;
    stateChangesUsed = 0;
    plansUsed = 0;
  }

  public int maxStateChangesPerTick() {
    return maxStateChangesPerTick;
  }

  public int maxPlansPerTick() {
    return maxPlansPerTick;
  }
}
