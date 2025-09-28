package net.tigereye.chestcavity.guscript.runtime.flow;

import java.util.Locale;

/**
 * External input types a flow can react to.
 */
public enum FlowInput {
    RELEASE,
    CANCEL;

    public static FlowInput fromName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Flow input name must not be blank");
        }
        return FlowInput.valueOf(name.trim().toUpperCase(Locale.ROOT));
    }

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public FlowTrigger toTrigger() {
        return switch (this) {
            case RELEASE -> FlowTrigger.RELEASE;
            case CANCEL -> FlowTrigger.CANCEL;
        };
    }
}
