package kizuna.guzhenren_event_ext.common.system_modules.conditions;

import com.google.gson.JsonObject;
import kizuna.guzhenren_event_ext.common.system_modules.ICondition;
import net.minecraft.world.entity.player.Player;

/**
 * A condition that checks the player's health percentage.
 * JSON format:
 * {
 *   "type": "guzhenren:player_health_percent",
 *   "min": 0.0,  // optional - minimum health percent (0.0 to 1.0)
 *   "max": 0.5   // optional - maximum health percent (0.0 to 1.0)
 * }
 */
public class PlayerHealthPercentCondition implements ICondition {

    @Override
    public boolean check(Player player, JsonObject definition) {
        float currentHealth = player.getHealth();
        float maxHealth = player.getMaxHealth();

        // Avoid division by zero
        if (maxHealth <= 0) {
            return false;
        }

        double healthPercent = currentHealth / maxHealth;

        // Check minimum health percent
        if (definition.has("min")) {
            double min = definition.get("min").getAsDouble();
            if (healthPercent < min) {
                return false;
            }
        }

        // Check maximum health percent
        if (definition.has("max")) {
            double max = definition.get("max").getAsDouble();
            if (healthPercent > max) {
                return false;
            }
        }

        return true;
    }
}
