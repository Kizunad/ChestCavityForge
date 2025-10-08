package net.tigereye.chestcavity.client.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.tigereye.chestcavity.client.modernui.TestModernUIFragment;
import icyllis.modernui.mc.MuiModApi;
import net.tigereye.chestcavity.client.modernui.container.network.TestModernUIContainerRequestPayload;

/**
 * Client-only brigadier commands for Modern UI bring-up and manual diagnostics.
 */
@OnlyIn(Dist.CLIENT)
public final class ModernUIClientCommands {

    private ModernUIClientCommands() {}

    public static void register(RegisterClientCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("testmodernUI")
                .executes(context -> openScreen())
                .then(Commands.literal("screen").executes(context -> openScreen()))
                .then(Commands.literal("container").executes(context -> requestContainer())));
    }

    private static int openScreen() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return 0;
        }

        mc.execute(() -> {
            var fragment = new TestModernUIFragment();
            var screen = MuiModApi.get().createScreen(fragment);
            mc.setScreen(screen);
            mc.player.displayClientMessage(
                    Component.translatable("commands.chestcavity.testmodernui.opened"),
                    true
            );
        });

        return Command.SINGLE_SUCCESS;
    }

    private static int requestContainer() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null) {
            return 0;
        }
        mc.execute(() -> {
            if (mc.getConnection() != null) {
                mc.getConnection().send(new TestModernUIContainerRequestPayload());
            }
        });
        return Command.SINGLE_SUCCESS;
    }
}
