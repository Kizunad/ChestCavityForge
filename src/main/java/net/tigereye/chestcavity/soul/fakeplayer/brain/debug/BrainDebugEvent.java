package net.tigereye.chestcavity.soul.fakeplayer.brain.debug;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** 不可变调试事件载荷（简化版）。 */
public record BrainDebugEvent(String channel, String message, Map<String, Object> attributes) {

    public BrainDebugEvent {
        if (channel == null || channel.isBlank()) channel = "default";
        if (message == null) message = "";
        attributes = attributes == null ? Map.of() : Collections.unmodifiableMap(new HashMap<>(attributes));
    }

    public static Builder builder(String channel) { return new Builder(channel); }

    public static final class Builder {
        private final String channel;
        private String message = "";
        private final Map<String, Object> attributes = new HashMap<>();
        private Builder(String channel) { this.channel = channel == null ? "default" : channel; }
        public Builder message(String message) { this.message = message == null ? "" : message; return this; }
        public Builder attribute(String key, Object value) { if (key != null && value != null) attributes.put(key, value); return this; }
        public BrainDebugEvent build() { return new BrainDebugEvent(channel, message, attributes); }
    }
}
