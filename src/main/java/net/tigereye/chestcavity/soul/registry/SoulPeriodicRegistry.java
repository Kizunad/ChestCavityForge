package net.tigereye.chestcavity.soul.registry;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.tigereye.chestcavity.soul.fakeplayer.SoulPlayer;

/**
 * Registry for per-second and per-minute callbacks targeting active SoulPlayers.
 *
 * <p>Usage: - Call {@link #registerPerSecond(SoulPerSecondListener)} or {@link
 * #registerPerMinute(SoulPerMinuteListener)} at mod init to install listeners. - The dispatcher
 * will invoke listeners once per SoulPlayer when the server gameTime crosses a 20-tick (1s) or
 * 1200-tick (60s) boundary.
 */
public final class SoulPeriodicRegistry {

  private static final List<SoulPerSecondListener> SECOND_LISTENERS = new CopyOnWriteArrayList<>();
  private static final List<SoulPerMinuteListener> MINUTE_LISTENERS = new CopyOnWriteArrayList<>();

  private SoulPeriodicRegistry() {}

  public static void registerPerSecond(SoulPerSecondListener l) {
    if (l != null) SECOND_LISTENERS.add(l);
  }

  public static void unregisterPerSecond(SoulPerSecondListener l) {
    SECOND_LISTENERS.remove(l);
  }

  public static void registerPerMinute(SoulPerMinuteListener l) {
    if (l != null) MINUTE_LISTENERS.add(l);
  }

  public static void unregisterPerMinute(SoulPerMinuteListener l) {
    MINUTE_LISTENERS.remove(l);
  }

  public static void dispatchSecond(SoulPlayer player, long gameTime) {
    for (SoulPerSecondListener l : SECOND_LISTENERS) l.onSecond(player, gameTime);
  }

  public static void dispatchMinute(SoulPlayer player, long gameTime) {
    for (SoulPerMinuteListener l : MINUTE_LISTENERS) l.onMinute(player, gameTime);
  }
}
