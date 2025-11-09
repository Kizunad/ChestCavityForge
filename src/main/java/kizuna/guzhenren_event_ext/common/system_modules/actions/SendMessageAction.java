package kizuna.guzhenren_event_ext.common.system_modules.actions;

import com.google.gson.JsonObject;
import kizuna.guzhenren_event_ext.common.system_modules.IAction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/**
 * Action that sends a message to the player.
 * JSON format:
 * {
 *   "type": "guzhenren_event_ext:send_message",
 *   "message": "获得了一个泥土！"
 * }
 */
public class SendMessageAction implements IAction {

    @Override
    public void execute(Player player, JsonObject definition) {
        if (!definition.has("message")) {
            return;
        }

        String message = definition.get("message").getAsString();
        player.sendSystemMessage(Component.literal(message));
    }
}
