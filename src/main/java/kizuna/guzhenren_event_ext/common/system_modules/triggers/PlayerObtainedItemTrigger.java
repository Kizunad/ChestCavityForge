package kizuna.guzhenren_event_ext.common.system_modules.triggers;

import com.google.gson.JsonObject;
import kizuna.guzhenren_event_ext.common.event.api.PlayerObtainedItemEvent;
import kizuna.guzhenren_event_ext.common.system_modules.ITrigger;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

/**
 * Trigger that matches when a player obtains a specific item.
 * JSON format:
 * {
 *   "type": "player_obtained_item",
 *   "item": "minecraft:dirt",
 *   "min_count": 1  // optional
 * }
 */
public class PlayerObtainedItemTrigger implements ITrigger<PlayerObtainedItemEvent> {

    @Override
    public boolean matches(PlayerObtainedItemEvent event, JsonObject definition) {
        // Check if item matches
        if (definition.has("item")) {
            String itemId = definition.get("item").getAsString();
            ResourceLocation itemLocation = ResourceLocation.parse(itemId);
            ResourceLocation eventItemLocation = BuiltInRegistries.ITEM.getKey(event.getItem());

            if (!itemLocation.equals(eventItemLocation)) {
                return false;
            }
        }

        // Check minimum count if specified
        if (definition.has("min_count")) {
            int minCount = definition.get("min_count").getAsInt();
            if (event.getCount() < minCount) {
                return false;
            }
        }

        return true;
    }
}
