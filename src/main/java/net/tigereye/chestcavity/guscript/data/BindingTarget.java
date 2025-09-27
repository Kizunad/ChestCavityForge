package net.tigereye.chestcavity.guscript.data;

import net.minecraft.network.chat.Component;

import java.util.Locale;

public enum BindingTarget {
    KEYBIND("keybind"),
    LISTENER("listener");

    private final String translationKey;

    BindingTarget(String translationKey) {
        this.translationKey = translationKey;
    }

    public BindingTarget next() {
        return switch (this) {
            case KEYBIND -> LISTENER;
            case LISTENER -> KEYBIND;
        };
    }

    public Component label() {
        return Component.translatable("gui.chestcavity.guscript.binding." + translationKey);
    }

    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static BindingTarget fromSerializedName(String name) {
        if (name == null || name.isBlank()) {
            return KEYBIND;
        }
        String normalized = name.toUpperCase(Locale.ROOT);
        for (BindingTarget value : values()) {
            if (value.name().equals(normalized)) {
                return value;
            }
        }
        return KEYBIND;
    }

    public static BindingTarget fromOrdinal(int ordinal) {
        BindingTarget[] values = values();
        if (ordinal < 0 || ordinal >= values.length) {
            return KEYBIND;
        }
        return values[ordinal];
    }
}
