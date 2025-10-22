package net.tigereye.chestcavity.soul.fakeplayer.brain.policy;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** 针对探索大脑的轻量预算跟踪，限制每 tick 的计划与状态切换次数。 */
public final class ExplorationBudgetTracker {

  private static final class Entry {
    long lastTick;
    int plans;
    int stateChanges;
  }

  private final BudgetPolicy policy;
  private final Map<UUID, Entry> entries = new ConcurrentHashMap<>();

  public ExplorationBudgetTracker(BudgetPolicy policy) {
    this.policy = policy;
  }

  public boolean tryConsumePlan(UUID soulId, long gameTime) {
    Entry entry = entries.computeIfAbsent(soulId, unused -> new Entry());
    resetIfNeeded(entry, gameTime);
    if (entry.plans >= policy.maxPlansPerTick()) {
      return false;
    }
    entry.plans += 1;
    return true;
  }

  public boolean tryConsumeStateChange(UUID soulId, long gameTime) {
    Entry entry = entries.computeIfAbsent(soulId, unused -> new Entry());
    resetIfNeeded(entry, gameTime);
    if (entry.stateChanges >= policy.maxStateChangesPerTick()) {
      return false;
    }
    entry.stateChanges += 1;
    return true;
  }

  public void clear(UUID soulId) {
    if (soulId != null) {
      entries.remove(soulId);
    }
  }

  private void resetIfNeeded(Entry entry, long gameTime) {
    if (entry.lastTick != gameTime) {
      entry.lastTick = gameTime;
      entry.plans = 0;
      entry.stateChanges = 0;
    }
  }
}
