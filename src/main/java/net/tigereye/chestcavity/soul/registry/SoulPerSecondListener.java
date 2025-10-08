package net.tigereye.chestcavity.soul.registry;

import net.tigereye.chestcavity.soul.fakeplayer.SoulPlayer;

/**
 * Listener invoked once per second (every 20 game ticks) for each active SoulPlayer.
 */
@FunctionalInterface
public interface SoulPerSecondListener {
    void onSecond(SoulPlayer player, long gameTime);
}

