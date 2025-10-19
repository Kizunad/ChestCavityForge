package net.tigereye.chestcavity.soul.fakeplayer.brain.debug;

import java.util.Locale;

import net.tigereye.chestcavity.ChestCavity;

/** 轻量日志器，受 JVM 参数开关控制，避免引入复杂依赖。 */
public final class BrainDebugLogger {

    private static final boolean ENABLED = Boolean.parseBoolean(System.getProperty("chestcavity.soul.brain.debug", "false"));

    private BrainDebugLogger() {}

    public static boolean isEnabled() { return ENABLED; }

    public static void trace(String channel, String message, Object... args) {
        if (!ENABLED) return;
        String formatted = args == null || args.length == 0 ? message : String.format(Locale.ROOT, message, args);
        ChestCavity.LOGGER.info("[brain:{}] {}", channel, formatted);
    }
}
