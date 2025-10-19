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
    }
}
