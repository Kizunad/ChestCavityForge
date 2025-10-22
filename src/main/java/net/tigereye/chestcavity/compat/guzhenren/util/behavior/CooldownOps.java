package net.tigereye.chestcavity.compat.guzhenren.util.behavior;

import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;

/** Utilities to manage per-organ cooldowns stored in OrganState. */
public final class CooldownOps {

  private CooldownOps() {}

  public static long read(OrganState state, String key) {
    if (state == null || key == null) {
      return 0L;
    }
    return state.getLong(key, 0L);
  }

  public static void set(OrganState state, String key, long readyTick) {
    if (state == null || key == null) {
      return;
    }
    state.setLong(key, Math.max(0L, readyTick), v -> Math.max(0L, v), 0L);
  }

  public static boolean isReady(OrganState state, String key, long gameTime) {
    return gameTime >= read(state, key);
  }

  public static boolean tryStart(OrganState state, String key, long gameTime, long durationTicks) {
    if (!isReady(state, key, gameTime)) {
      return false;
    }
    set(state, key, gameTime + Math.max(0L, durationTicks));
    return true;
  }
}
