package net.tigereye.chestcavity.soul.fakeplayer.brain.debug;

import java.util.Map;

/**
 * Helper for emitting structured telemetry without leaking implementation details.
 */
public final class BrainDebugLogger {

    private BrainDebugLogger() {}

    public static void logDecision(long gameTime, String brainId, String message, Map<String, ?> metadata) {
        emit(gameTime, brainId, "decision", message, metadata);
    }

    public static void logAction(long gameTime, String brainId, String message, Map<String, ?> metadata) {
        emit(gameTime, brainId, "action", message, metadata);
    }

    public static void logWarning(long gameTime, String brainId, String message, Map<String, ?> metadata) {
        emit(gameTime, brainId, "warning", message, metadata);
    }

    public static void logError(long gameTime, String brainId, String message, Map<String, ?> metadata) {
        emit(gameTime, brainId, "error", message, metadata);
    }

    public static void logState(long gameTime, String brainId, String message, Map<String, ?> metadata) {
        emit(gameTime, brainId, "state", message, metadata);
    }

    private static void emit(long gameTime, String brainId, String category, String message, Map<String, ?> metadata) {
        if (!BrainTelemetryHub.isEnabled()) {
            return;
        }
        BrainDebugEvent.Builder builder = BrainDebugEvent.builder(gameTime, category)
                .brainId(brainId)
                .message(message);
        if (metadata != null && !metadata.isEmpty()) {
            builder.putAllMetadata(metadata);
        }
        BrainTelemetryHub.publish(builder.build());
    }
}
