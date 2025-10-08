package net.tigereye.chestcavity.soul.runtime;

import net.tigereye.chestcavity.soul.fakeplayer.SoulPlayer;
import net.tigereye.chestcavity.soul.registry.SoulPeriodicRegistry;
import net.tigereye.chestcavity.soul.registry.SoulRuntimeHandler;

/**
 * Bridges the generic SoulRuntime tick pipeline to periodic (1s/1m) callbacks.
 */
public final class SoulPeriodicDispatcher implements SoulRuntimeHandler {

    private static final int TICKS_PER_SECOND = 20;
    private static final int TICKS_PER_MINUTE = 20 * 60;

    @Override
    public void onTickEnd(SoulPlayer player) {
        long time = player.level().getGameTime();
        if (time % TICKS_PER_SECOND == 0) {
            SoulPeriodicRegistry.dispatchSecond(player, time);
        }
        if (time % TICKS_PER_MINUTE == 0) {
            SoulPeriodicRegistry.dispatchMinute(player, time);
        }
    }
}

