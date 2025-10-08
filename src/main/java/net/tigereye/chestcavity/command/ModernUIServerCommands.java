package net.tigereye.chestcavity.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.tigereye.chestcavity.client.modernui.container.TestModernUIContainerDebug;

/**
 * Server-side debug command entry point for Modern UI experiments.
 */
public final class ModernUIServerCommands {

    private ModernUIServerCommands() {}

    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("testmodernUI")
                .requires(src -> src.hasPermission(0))
                .then(Commands.literal("container")
                        .executes(ModernUIServerCommands::openContainer)));
    }

    private static int openContainer(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        player.openMenu(TestModernUIContainerDebug.provider());
        return Command.SINGLE_SUCCESS;
    }

}
