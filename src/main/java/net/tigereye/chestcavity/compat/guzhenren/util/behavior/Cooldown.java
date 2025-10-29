package net.tigereye.chestcavity.compat.guzhenren.util.behavior;

import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;

/** Encapsulates per-organ cooldown read/write logic backed by OrganState (NBT). */
public final class Cooldown {

  private final java.util.function.BiConsumer<Long, Long> onChange;

  private final OrganState state;
  private final String key;

  private Cooldown(
      OrganState state, String key, java.util.function.BiConsumer<Long, Long> onChange) {
    this.state = state;
    this.key = key;
    this.onChange = onChange;
  }

  /** Bind to an existing OrganState and cooldown key. */
  public static Cooldown of(OrganState state, String key) {
    return new Cooldown(state, key, null);
  }

  public Cooldown withOnChange(java.util.function.BiConsumer<Long, Long> onChange) {
    return new Cooldown(this.state, this.key, onChange);
  }

  /** Convenience binder from ItemStack and root key. */
  public static Cooldown bind(ItemStack stack, String rootKey, String cooldownKey) {
    return of(OrganState.of(stack, rootKey), cooldownKey);
  }

  /** Next allowed game tick; 0 when unset. */
  public long getReadyTick() {
    return state.getLong(key, 0L);
  }

  /** Set the next allowed game tick (clamped to >= 0). */
  public void setReadyAt(long readyTick) {
    long prev = getReadyTick();
    state.setLong(key, Math.max(0L, readyTick), v -> Math.max(0L, v), 0L);
    long curr = getReadyTick();
    if (onChange != null && prev != curr) {
      onChange.accept(prev, curr);
    }
  }

  /** Clear cooldown (ready immediately). */
  public void clear() {
    setReadyAt(0L);
  }

  /** Whether cooldown is ready for the given game time. */
  public boolean isReady(long gameTime) {
    return gameTime >= getReadyTick();
  }

  /** Try to start cooldown; returns true if started. */
  public boolean tryStart(long gameTime, long durationTicks) {
    if (!isReady(gameTime)) {
      return false;
    }
    setReadyAt(gameTime + Math.max(0L, durationTicks));
    return true;
  }

  /**
   * Register a one-shot runnable when this timestamp-style cooldown becomes ready (relative to
   * {@code now}).
   */
  public Cooldown onReady(net.minecraft.server.level.ServerLevel level, long now, Runnable task) {
    if (level == null || task == null) return this;
    long remaining = Math.max(0L, getReadyTick() - now);
    int delay = remaining > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) remaining;
    net.tigereye.chestcavity.compat.guzhenren.util.behavior.TickOps.schedule(level, task, delay);
    return this;
  }

  /** Remaining ticks until ready; 0 if already ready. */
  public long remaining(long gameTime) {
    long next = getReadyTick();
    return Math.max(0L, next - gameTime);
  }

  public static final class Int {
    private final OrganState state;
    private final String key;
    private final java.util.function.BiConsumer<Integer, Integer> onChange;

    private Int(
        OrganState state, String key, java.util.function.BiConsumer<Integer, Integer> onChange) {
      this.state = state;
      this.key = key;
      this.onChange = onChange;
    }

    public static Int of(OrganState state, String key) {
      return new Int(state, key, null);
    }

    public static Int bind(ItemStack stack, String rootKey, String key) {
      return of(OrganState.of(stack, rootKey), key);
    }

    public Int withOnChange(java.util.function.BiConsumer<Integer, Integer> onChange) {
      return new Int(this.state, this.key, onChange);
    }

    public int getTicks() {
      return Math.max(0, state.getInt(key, 0));
    }

    public void setTicks(int ticks) {
      int prev = getTicks();
      state.setInt(key, Math.max(0, ticks));
      int curr = getTicks();
      if (onChange != null && prev != curr) {
        onChange.accept(prev, curr);
      }
    }

    public void clear() {
      setTicks(0);
    }

    public boolean isReady() {
      return getTicks() <= 0;
    }

    public void start(int durationTicks) {
      setTicks(Math.max(0, durationTicks));
    }

    public boolean tickDown() {
      int cur = getTicks();
      if (cur <= 0) {
        return false;
      }
      setTicks(cur - 1);
      return true;
    }

    /** Attach a callback invoked when the countdown crosses from >0 to 0 (no polling). */
    public Int onReady(Runnable task) {
      return new Int(
          this.state,
          this.key,
          (prev, curr) -> {
            if (prev != null && prev > 0 && curr != null && curr == 0) {
              task.run();
            }
          });
    }
  }
}
