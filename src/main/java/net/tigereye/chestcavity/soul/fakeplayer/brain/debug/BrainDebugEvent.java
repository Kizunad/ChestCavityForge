package net.tigereye.chestcavity.soul.fakeplayer.brain.debug;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable debug/telemetry event emitted by the Soul Brain runtime.
 */
public final class BrainDebugEvent {

    private final long gameTime;
    private final String brainId;
    private final String category;
    private final String message;
    private final Map<String, String> metadata;

    private BrainDebugEvent(long gameTime, String brainId, String category, String message, Map<String, String> metadata) {
        this.gameTime = gameTime;
        this.brainId = brainId;
        this.category = category;
        this.message = message;
        this.metadata = metadata;
    }

    public long getGameTime() {
        return gameTime;
    }

    public String getBrainId() {
        return brainId;
    }

    public String getCategory() {
        return category;
    }

    public String getMessage() {
        return message;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public static Builder builder(long gameTime, String category) {
        return new Builder(gameTime, category);
    }

    @Override
    public String toString() {
        return "BrainDebugEvent{" +
                "gameTime=" + gameTime +
                ", brainId='" + brainId + '\'' +
                ", category='" + category + '\'' +
                ", message='" + message + '\'' +
                ", metadata=" + metadata +
                '}';
    }

    /**
     * Simple builder for debug events allowing optional metadata.
     */
    public static final class Builder {
        private final long gameTime;
        private final String category;
        private String brainId = "";
        private String message = "";
        private final Map<String, String> metadata = new LinkedHashMap<>();

        private Builder(long gameTime, String category) {
            this.gameTime = gameTime;
            this.category = Objects.requireNonNull(category, "category");
        }

        public Builder brainId(String brainId) {
            this.brainId = brainId == null ? "" : brainId;
            return this;
        }

        public Builder message(String message) {
            this.message = message == null ? "" : message;
            return this;
        }

        public Builder putMetadata(String key, Object value) {
            if (key != null && value != null) {
                metadata.put(key, String.valueOf(value));
            }
            return this;
        }

        public Builder putAllMetadata(Map<String, ?> map) {
            if (map != null) {
                for (Map.Entry<String, ?> entry : map.entrySet()) {
                    if (entry.getKey() != null && entry.getValue() != null) {
                        metadata.put(entry.getKey(), String.valueOf(entry.getValue()));
                    }
                }
            }
            return this;
        }

        public BrainDebugEvent build() {
            Map<String, String> immutableMeta = Collections.unmodifiableMap(new LinkedHashMap<>(metadata));
            return new BrainDebugEvent(gameTime, brainId, category, message, immutableMeta);
        }
    }
}
