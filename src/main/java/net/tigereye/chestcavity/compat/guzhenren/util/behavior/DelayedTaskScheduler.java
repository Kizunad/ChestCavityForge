package net.tigereye.chestcavity.compat.guzhenren.util.behavior;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.tigereye.chestcavity.ChestCavity;

import java.util.*;

/**
 * Minimal per-server tick scheduler keyed by absolute game time.
 */
@EventBusSubscriber(modid = ChestCavity.MODID)
final class DelayedTaskScheduler {
    private static final Map<MinecraftServer, Map<Long, List<Runnable>>> QUEUES = new WeakHashMap<>();

    private DelayedTaskScheduler() {}

    static void schedule(ServerLevel level, int delayTicks, Runnable task) {
        if (level == null || task == null || delayTicks <= 0) return;
        MinecraftServer server = level.getServer();
        long runAt = level.getGameTime() + delayTicks;
        Map<Long, List<Runnable>> queue = QUEUES.computeIfAbsent(server, s -> new HashMap<>());
        queue.computeIfAbsent(runAt, t -> new ArrayList<>()).add(task);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        Map<Long, List<Runnable>> queue = QUEUES.get(server);
        if (queue == null || queue.isEmpty()) return;
        ServerLevel overworld = server.overworld();
        if (overworld == null) return;
        long now = overworld.getGameTime();
        List<Runnable> list = queue.remove(now);
        if (list == null || list.isEmpty()) return;
        for (Runnable r : list) {
            try { r.run(); } catch (Throwable ignored) {}
        }
    }
}

