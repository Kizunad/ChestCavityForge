package net.tigereye.chestcavity.guscript.runtime.flow.fx;

import java.util.Locale;

/**
 * Attachment anchors for GeckoLib-based flow FX.
 */
public enum GeckoFxAnchor {
    PERFORMER,
    TARGET,
    ENTITY,
    WORLD;

    public static GeckoFxAnchor fromName(String raw) {
        if (raw == null || raw.isBlank()) {
            return PERFORMER;
        }
        return switch (raw.trim().toUpperCase(Locale.ROOT)) {
            case "TARGET" -> TARGET;
            case "ENTITY", "ENTITY_ID", "ENTITY_VARIABLE" -> ENTITY;
            case "WORLD", "ABSOLUTE", "POSITION" -> WORLD;
            case "PERFORMER", "SELF", "PLAYER" -> PERFORMER;
            default -> PERFORMER;
        };
    }

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}

