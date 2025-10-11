package net.tigereye.chestcavity.compat.guzhenren.util.behavior;

import net.minecraft.server.level.ServerLevel;

/**
 * Lightweight scheduling helpers used by organ behaviors.
 */
public final class TickOps {

    private TickOps() {}

    /** Schedule a runnable after the specified delay in ticks. */
    public static void schedule(ServerLevel level, Runnable runnable, int delayTicks) {
        if (level == null || runnable == null) return;
        if (delayTicks <= 0) { runnable.run(); return; }
        DelayedTaskScheduler.schedule(level, delayTicks, runnable);
    }
}
