package kizuna.guzhenren_event_ext.common.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Configuration file for the Guzhenren Event Extension module.
 * Provides tunable settings for polling intervals and other system parameters.
 */
public class ModConfig {

    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    // Polling intervals
    public static final ModConfigSpec.IntValue STAT_WATCHER_INTERVAL;
    public static final ModConfigSpec.IntValue INVENTORY_WATCHER_INTERVAL;

    static {
        BUILDER.push("polling");
        BUILDER.comment("Polling settings for the event extension system");

        STAT_WATCHER_INTERVAL = BUILDER
                .comment(
                        "How often (in ticks) the system checks for player stat changes.",
                        "Lower values = more responsive but higher performance cost.",
                        "20 ticks = 1 second",
                        "Default: 40 (2 seconds)"
                )
                .defineInRange("stat_watcher_interval", 40, 1, 1200);

        INVENTORY_WATCHER_INTERVAL = BUILDER
                .comment(
                        "How often (in ticks) the system checks for new items in player inventories.",
                        "Lower values = more responsive but higher performance cost.",
                        "20 ticks = 1 second",
                        "Default: 1200 (60 seconds / 1 minute)"
                )
                .defineInRange("inventory_watcher_interval", 1200, 20, 6000);

        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    /**
     * Gets the configured stat watcher polling interval in ticks.
     */
    public static int getStatWatcherInterval() {
        return STAT_WATCHER_INTERVAL.get();
    }

    /**
     * Gets the configured inventory watcher polling interval in ticks.
     */
    public static int getInventoryWatcherInterval() {
        return INVENTORY_WATCHER_INTERVAL.get();
    }
}
