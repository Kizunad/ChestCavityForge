package kizuna.guzhenren_event_ext.common.system_modules;

import com.google.gson.JsonObject;
import net.minecraft.world.entity.player.Player;

/**
 * Interface for a condition that must be met for an event's actions to execute.
 */
@FunctionalInterface
public interface ICondition {
    boolean check(Player player, JsonObject definition);
}
