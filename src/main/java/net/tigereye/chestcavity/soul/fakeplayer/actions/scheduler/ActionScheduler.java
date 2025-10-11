package net.tigereye.chestcavity.soul.fakeplayer.actions.scheduler;

import net.minecraft.server.level.ServerLevel;
import net.tigereye.chestcavity.soul.fakeplayer.SoulPlayer;
import net.tigereye.chestcavity.soul.fakeplayer.actions.api.ActionId;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.TickOps;

/** Wrapper over TickOps for actions; deduplicates keying concerns in one place. */
public final class ActionScheduler {
    private ActionScheduler() {}

    public static void schedule(ServerLevel level, SoulPlayer soul, ActionId id, Runnable task, int delayTicks) {
        // For now we just delegate; if we later need per-soul bucketing or dedup, add here.
        TickOps.schedule(level, task, delayTicks);
    }
}

