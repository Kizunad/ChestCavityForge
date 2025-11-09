package kizuna.guzhenren_event_ext.common.system_modules.triggers;

import com.google.gson.JsonObject;
import kizuna.guzhenren_event_ext.common.event.api.GuzhenrenStatChangeEvent;
import kizuna.guzhenren_event_ext.common.system_modules.ITrigger;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;

/**
 * Trigger that matches when a player's Guzhenren stat changes.
 * JSON format:
 * {
 *   "type": "guzhenren_event_ext:player_stat_change",
 *   "stat": "zhenyuan",
 *   "from": {  // optional - conditions on the old value
 *     "min": 100.0,
 *     "max": 500.0,
 *     "min_percent": 0.5,
 *     "max_percent": 0.9
 *   },
 *   "to": {  // optional - conditions on the new value
 *     "min": 50.0,
 *     "max": 200.0,
 *     "min_percent": 0.1,
 *     "max_percent": 0.5
 *   }
 * }
 */
public class PlayerStatChangeTrigger implements ITrigger<GuzhenrenStatChangeEvent> {

    @Override
    public boolean matches(GuzhenrenStatChangeEvent event, JsonObject definition) {
        // Check if stat matches
        if (definition.has("stat")) {
            String requiredStat = definition.get("stat").getAsString();
            if (!requiredStat.equals(event.getStatIdentifier())) {
                return false;
            }
        }

        // Get resource handle for max value lookups
        GuzhenrenResourceBridge.ResourceHandle handle = event.getResourceHandle();

        // Check "from" conditions (old value)
        if (definition.has("from")) {
            JsonObject fromConditions = definition.getAsJsonObject("from");
            if (!checkValueConditions(fromConditions, event.getOldValue(), event.getStatIdentifier(), handle)) {
                return false;
            }
        }

        // Check "to" conditions (new value)
        if (definition.has("to")) {
            JsonObject toConditions = definition.getAsJsonObject("to");
            if (!checkValueConditions(toConditions, event.getNewValue(), event.getStatIdentifier(), handle)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Checks if a value satisfies the given conditions.
     *
     * @param conditions The JSON object containing min/max/min_percent/max_percent conditions
     * @param value The actual value to check
     * @param statId The stat identifier (for looking up max values)
     * @param handle The resource handle (for looking up max values)
     * @return true if all conditions are met, false otherwise
     */
    private boolean checkValueConditions(JsonObject conditions, double value, String statId, GuzhenrenResourceBridge.ResourceHandle handle) {
        // Check absolute min
        if (conditions.has("min")) {
            double min = conditions.get("min").getAsDouble();
            if (value < min) {
                return false;
            }
        }

        // Check absolute max
        if (conditions.has("max")) {
            double max = conditions.get("max").getAsDouble();
            if (value > max) {
                return false;
            }
        }

        // For percentage checks, we need the max value of the stat
        if (conditions.has("min_percent") || conditions.has("max_percent")) {
            // Try to read the max value for this stat
            // Common pattern: stat "zhenyuan" has max "zhenyuan_max"
            String maxStatId = statId + "_max";
            double maxValue = handle.read(maxStatId).orElse(1.0);

            // Avoid division by zero
            if (maxValue <= 0) {
                maxValue = 1.0;
            }

            double percent = value / maxValue;

            // Check min_percent
            if (conditions.has("min_percent")) {
                double minPercent = conditions.get("min_percent").getAsDouble();
                if (percent < minPercent) {
                    return false;
                }
            }

            // Check max_percent
            if (conditions.has("max_percent")) {
                double maxPercent = conditions.get("max_percent").getAsDouble();
                if (percent > maxPercent) {
                    return false;
                }
            }
        }

        return true;
    }
}
