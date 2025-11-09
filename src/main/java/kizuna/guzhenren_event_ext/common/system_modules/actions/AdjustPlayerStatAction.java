package kizuna.guzhenren_event_ext.common.system_modules.actions;

import com.google.gson.JsonObject;
import kizuna.guzhenren_event_ext.GuzhenrenEventExtension;
import kizuna.guzhenren_event_ext.common.system_modules.IAction;
import net.minecraft.world.entity.player.Player;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;

/**
 * Action that adjusts (modifies) a player's Guzhenren stat.
 * JSON format:
 * {
 *   "type": "guzhenren_event_ext:adjust_player_stat",
 *   "stat": "zhenyuan",
 *   "operation": "add",  // "add", "subtract", "multiply", "set"
 *   "value": 100.0
 * }
 */
public class AdjustPlayerStatAction implements IAction {

    @Override
    public void execute(Player player, JsonObject definition) {
        if (!definition.has("stat")) {
            GuzhenrenEventExtension.LOGGER.warn("AdjustPlayerStatAction is missing 'stat' field");
            return;
        }

        if (!definition.has("value")) {
            GuzhenrenEventExtension.LOGGER.warn("AdjustPlayerStatAction is missing 'value' field");
            return;
        }

        String stat = definition.get("stat").getAsString();
        double value = definition.get("value").getAsDouble();
        String operation = definition.has("operation") ? definition.get("operation").getAsString() : "add";

        GuzhenrenResourceBridge.open(player).ifPresent(handle -> {
            // Read the current value
            double currentValue = handle.read(stat).orElse(0.0);
            double newValue;

            // Perform the operation
            switch (operation.toLowerCase()) {
                case "add":
                    newValue = currentValue + value;
                    break;
                case "subtract":
                    newValue = currentValue - value;
                    break;
                case "multiply":
                    newValue = currentValue * value;
                    break;
                case "set":
                    newValue = value;
                    break;
                default:
                    GuzhenrenEventExtension.LOGGER.warn("Unknown operation '{}' in AdjustPlayerStatAction, defaulting to 'add'", operation);
                    newValue = currentValue + value;
                    break;
            }

            // Write the new value
            handle.write(stat, newValue);

            GuzhenrenEventExtension.LOGGER.debug("Adjusted stat '{}' for player '{}': {} {} {} = {}",
                    stat, player.getName().getString(), currentValue, operation, value, newValue);
        });
    }
}
