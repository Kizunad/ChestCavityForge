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
        return openFragmentCommand(TestModernUIFragment::new, "commands.chestcavity.testmodernui.opened");
    }

    private static int openConfigScreen() {
        return openFragmentCommand(ChestCavityConfigFragment::new, "commands.chestcavity.testmodernui.config.opened");
    }

    public static void openConfigViaHotkey() {
        openFragment(ChestCavityConfigFragment::new, null);
    }

    private static int openFragmentCommand(Supplier<Fragment> fragmentSupplier, String translationKey) {
        return openFragment(fragmentSupplier, Component.translatable(translationKey)) ? Command.SINGLE_SUCCESS : 0;
    }

    private static boolean openFragment(Supplier<Fragment> fragmentSupplier, Component toastMessage) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return false;
        }

        mc.execute(() -> {
            var fragment = fragmentSupplier.get();
            var screen = MuiModApi.get().createScreen(fragment);
            mc.setScreen(screen);
            if (toastMessage != null) {
                mc.player.displayClientMessage(toastMessage, true);
            }
        });

        return true;
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
