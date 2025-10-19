package net.tigereye.chestcavity.soul.fakeplayer.brain.debug;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import net.tigereye.chestcavity.ChestCavity;

/** Simple pub/sub bridge for brain debug instrumentation. */
public final class BrainDebugProbe {

    private static final List<Consumer<BrainDebugEvent>> SINKS = new CopyOnWriteArrayList<>();

    private BrainDebugProbe() {}

    public static void addSink(Consumer<BrainDebugEvent> sink) {
        if (sink != null) {
            SINKS.add(sink);
        }
    }

    public static void removeSink(Consumer<BrainDebugEvent> sink) {
        if (sink != null) {
            SINKS.remove(sink);
        }
    }

    public static void emit(BrainDebugEvent event) {
        if (event == null) {
            return;
        }
        BrainDebugLogger.trace(event.channel(), "%s %s", event.message(), event.attributes());
        for (Consumer<BrainDebugEvent> sink : SINKS) {
            try {
                sink.accept(event);
            } catch (Throwable t) {
                ChestCavity.LOGGER.error("[brain][debug] sink failure", t);
            }
        }
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.tigereye.chestcavity.soul.fakeplayer.SoulPlayer;

/**
 * 收集并缓存子大脑的调试快照，便于命令或测试读取。
 */
public final class BrainDebugProbe {

    private static final Map<UUID, ExplorationTelemetry> EXPLORATION = new ConcurrentHashMap<>();
    private static final Map<UUID, SurvivalTelemetry> SURVIVAL = new ConcurrentHashMap<>();

    private BrainDebugProbe() {}

    public static void recordExploration(SoulPlayer soul, ExplorationTelemetry telemetry) {
        if (soul == null || telemetry == null) {
            return;
        }
        EXPLORATION.put(soul.getUUID(), telemetry);
    }

    public static ExplorationTelemetry lastExploration(UUID soulId) {
        if (soulId == null) {
            return null;
        }
        return EXPLORATION.get(soulId);
    }

    public static void recordSurvival(SoulPlayer soul, SurvivalTelemetry telemetry) {
        if (soul == null || telemetry == null) {
            return;
        }
        SURVIVAL.put(soul.getUUID(), telemetry);
    }

    public static SurvivalTelemetry lastSurvival(UUID soulId) {
        if (soulId == null) {
            return null;
        }
        return SURVIVAL.get(soulId);
    }

    public static void clear(UUID soulId) {
        if (soulId == null) {
            return;
        }
        EXPLORATION.remove(soulId);
        SURVIVAL.remove(soulId);
    }
}
