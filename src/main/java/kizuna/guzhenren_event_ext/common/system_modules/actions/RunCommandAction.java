package kizuna.guzhenren_event_ext.common.system_modules.actions;

import com.google.gson.JsonObject;
import kizuna.guzhenren_event_ext.GuzhenrenEventExtension;
import kizuna.guzhenren_event_ext.common.system_modules.IAction;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

/**
 * Action that executes a command as the server or as the player.
 * JSON format:
 * {
 *   "type": "guzhenren_event_ext:run_command",
 *   "command": "give @s minecraft:diamond 1",
 *   "as_player": false  // optional, default false (runs as server with level 2 permissions)
 * }
 *
 * Placeholders:
 * - @s or @p will refer to the triggering player
 * - {player} will be replaced with the player's name
 */
public class RunCommandAction implements IAction {

    @Override
    public void execute(Player player, JsonObject definition) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return; // Commands can only run on the server
        }

        if (!definition.has("command")) {
            GuzhenrenEventExtension.LOGGER.warn("RunCommandAction is missing 'command' field");
            return;
        }

        String command = definition.get("command").getAsString();
        boolean asPlayer = definition.has("as_player") && definition.get("as_player").getAsBoolean();

        // Replace placeholders
        command = command.replace("{player}", serverPlayer.getName().getString());

        try {
            CommandSourceStack source;

            if (asPlayer) {
                // Execute as the player (with player's permission level)
                source = serverPlayer.createCommandSourceStack();
            } else {
                // Execute as the server at the player's position (with level 2 permissions)
                source = new CommandSourceStack(
                        CommandSource.NULL,
                        serverPlayer.position(),
                        Vec2.ZERO,
                        serverPlayer.serverLevel(),
                        2, // Permission level 2 (game master)
                        serverPlayer.getName().getString(),
                        Component.literal(serverPlayer.getName().getString()),
                        serverPlayer.server,
                        serverPlayer
                );
            }

            // Execute the command
            serverPlayer.server.getCommands().performPrefixedCommand(source, command);

        } catch (Exception e) {
            GuzhenrenEventExtension.LOGGER.error("Failed to execute command '{}' for player {}", command, serverPlayer.getName().getString(), e);
        }
    }
}
