package net.tigereye.chestcavity.soul.registry;

import net.tigereye.chestcavity.soul.fakeplayer.SoulPlayer;

/** Listener invoked once per minute (every 1200 game ticks) for each active SoulPlayer. */
@FunctionalInterface
public interface SoulPerMinuteListener {
  void onMinute(SoulPlayer player, long gameTime);
}
