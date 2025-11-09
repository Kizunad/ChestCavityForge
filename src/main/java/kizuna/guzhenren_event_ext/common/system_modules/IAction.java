package kizuna.guzhenren_event_ext.common.system_modules;

import com.google.gson.JsonObject;
import net.minecraft.world.entity.player.Player;

/**
 * Interface for an action to be executed when an event is successfully triggered and all conditions are met.
 */
@FunctionalInterface
public interface IAction {
    void execute(Player player, JsonObject definition);
}
