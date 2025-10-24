package net.tigereye.chestcavity.soul.fakeplayer.actions.api;

/**
 * Minimal Action contract for FakePlayer autonomy. Stateless implementations preferred; persist run
 * state via ActionStateManager.
 */
public interface Action {
  ActionId id();

  ActionPriority priority();

  /** Whether this action may run concurrently with other actions. */
  default boolean allowConcurrent() {
    return false;
  }

  /** Quick pre-check before starting. Keep O(1). */
  boolean canRun(ActionContext ctx);

  /** Called once when the action is accepted by the planner. */
  void start(ActionContext ctx);

  /**
   * Execute a small step. Must be cheap and non-blocking. Return RUNNING to continue later,
   * SUCCESS/FAILED/CANCELLED to finish.
   */
  ActionResult tick(ActionContext ctx);

  /** Planner calls when preempted or owner requested cancel. */
  void cancel(ActionContext ctx);

  /** Optional per-action cooldown key; planner may arm on completion. */
  default String cooldownKey() {
    return null;
  }

  /**
   * Next time to run in server ticks. Use to avoid per-tick busy loops. Return now or now+N;
   * planner will schedule the next tick accordingly.
   */
  long nextReadyAt(ActionContext ctx, long now);
}
