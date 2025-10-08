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
import net.tigereye.chestcavity.client.modernui.config.ChestCavityConfigFragment;
import icyllis.modernui.mc.MuiModApi;
import icyllis.modernui.fragment.Fragment;

import java.util.function.Supplier;
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
                .executes(context -> openTestScreen())
                .then(Commands.literal("screen").executes(context -> openTestScreen()))
                .then(Commands.literal("container").executes(context -> requestContainer()))
                .then(Commands.literal("config").executes(context -> openConfigScreen())));
    }

    private static int openTestScreen() {
        return openFragment(TestModernUIFragment::new, "commands.chestcavity.testmodernui.opened");
    }

    private static int openConfigScreen() {
        return openFragment(ChestCavityConfigFragment::new, "commands.chestcavity.testmodernui.config.opened");
    }

    private static int openFragment(Supplier<Fragment> fragmentSupplier, String translationKey) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return 0;
        }

        mc.execute(() -> {
            var fragment = fragmentSupplier.get();
            var screen = MuiModApi.get().createScreen(fragment);
            mc.setScreen(screen);
            mc.player.displayClientMessage(
                    Component.translatable(translationKey),
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
