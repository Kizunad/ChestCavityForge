package net.tigereye.chestcavity.soul.fakeplayer.brain.debug;

import java.util.Locale;

import net.tigereye.chestcavity.ChestCavity;

/** Lightweight logger gated by a JVM property. */
public final class BrainDebugLogger {

    private static final boolean ENABLED = Boolean.parseBoolean(System.getProperty("chestcavity.soul.brain.debug", "false"));

    private BrainDebugLogger() {}

    public static boolean isEnabled() {
        return ENABLED;
    }

    public static void trace(String channel, String message, Object... args) {
        if (!ENABLED) {
            return;
        }
        String formatted = args == null || args.length == 0
                ? message
                : String.format(Locale.ROOT, message, args);
        ChestCavity.LOGGER.debug("[brain:{}] {}", channel, formatted);
    }
}
