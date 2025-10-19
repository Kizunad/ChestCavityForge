package net.tigereye.chestcavity.soul.fakeplayer.brain.debug;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Central registry that fans out brain debug events to registered sinks.
 */
public final class BrainTelemetryHub {

    private static final List<BrainTelemetrySink> SINKS = new CopyOnWriteArrayList<>();
    private static volatile boolean enabled = false;

    private BrainTelemetryHub() {}

    public static void enable() {
        enabled = true;
    }

    public static void disable() {
        enabled = false;
    }

    public static boolean isEnabled() {
        return enabled && !SINKS.isEmpty();
    }

    public static void registerSink(BrainTelemetrySink sink) {
        Objects.requireNonNull(sink, "sink");
        SINKS.add(sink);
    }

    public static void unregisterSink(BrainTelemetrySink sink) {
        if (sink != null) {
            SINKS.remove(sink);
        }
    }

    public static void clearSinks() {
        SINKS.clear();
    }

    public static void publish(BrainDebugEvent event) {
        if (!isEnabled() || event == null) {
            return;
        }
        for (BrainTelemetrySink sink : SINKS) {
            try {
                sink.publish(event);
            } catch (RuntimeException ex) {
                // Debug sinks must never crash the brain loop.
            }
        }
    }
}
