package kizuna.guzhenren_event_ext.common.system_modules.conditions;

import com.google.gson.JsonObject;
import kizuna.guzhenren_event_ext.common.system_modules.ICondition;
import net.minecraft.world.entity.player.Player;

import java.util.Random;

/**
 * A condition that succeeds with a specified probability.
 * JSON format:
 * {
 *   "type": "minecraft:random_chance",
 *   "chance": 0.5  // 0.0 to 1.0, where 1.0 = 100% chance
 * }
 */
public class RandomChanceCondition implements ICondition {

    private static final Random RANDOM = new Random();

    @Override
    public boolean check(Player player, JsonObject definition) {
        if (!definition.has("chance")) {
            // If no chance is specified, default to 100% (always pass)
            return true;
        }

        double chance = definition.get("chance").getAsDouble();

        // Clamp between 0 and 1
        chance = Math.max(0.0, Math.min(1.0, chance));

        return RANDOM.nextDouble() < chance;
    }
}
