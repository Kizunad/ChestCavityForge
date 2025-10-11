package net.tigereye.chestcavity.soul.util;

import net.tigereye.chestcavity.ChestCavity;
import org.apache.logging.log4j.Logger;

/**
 * Centralised logging helper for soul-related modules. All INFO-level diagnostics flow through this
 * class so toggling {@link #DEBUG_LOGS} in a single place enables or suppresses verbose output.
 */
public final class SoulLog {

    private SoulLog() {
    }

    /**
     * Master toggle for soul module diagnostics. Flip to {@code false} to silence INFO logs.
     */
    public static boolean DEBUG_LOGS = Boolean.getBoolean("chestcavity.debugSoul");

    private static final Logger LOGGER = ChestCavity.LOGGER;

    public static void info(String message, Object... args) {
        LOGGER.info(message, args);
    }

    public static void warn(String message, Object... args) {
        LOGGER.warn(message, args);
    }

    public static void error(String message, Throwable throwable, Object... args) {
        // Log message with formatted args then attach stacktrace
        if (args != null && args.length > 0) {
            LOGGER.error(message, args);
            LOGGER.error("[soul] stack:", throwable);
        } else {
            LOGGER.error(message, throwable);
        }
    }
}
