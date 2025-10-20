package net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.state.SoulBeastStateManager;
import net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.state.event.SoulBeastStateChangedEvent;

import java.util.Locale;

@EventBusSubscriber(modid = ChestCavity.MODID)
public final class SoulBeastCommands {

    private SoulBeastCommands() {}

    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("soulbeast")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("enable")
                        .then(Commands.argument("value", BoolArgumentType.bool())
                                .executes(SoulBeastCommands::setEnabled)))
                .then(Commands.literal("permanent")
                        .then(Commands.argument("value", BoolArgumentType.bool())
                                .executes(SoulBeastCommands::setPermanent)))
                .then(Commands.literal("info").executes(SoulBeastCommands::showInfo))
        );
    }

    private static int setEnabled(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        boolean value = BoolArgumentType.getBool(ctx, "value");
        SoulBeastStateManager.setEnabled(player, value);
        ctx.getSource().sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
                "[soulbeast] enabled=%s (permanent=%s, active=%s)",
                SoulBeastStateManager.isEnabled(player),
                SoulBeastStateManager.isPermanent(player),
                SoulBeastStateManager.isActive(player))), true);
        return 1;
    }

    private static int setPermanent(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        boolean value = BoolArgumentType.getBool(ctx, "value");
        SoulBeastStateManager.setPermanent(player, value);
        ctx.getSource().sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
                "[soulbeast] permanent=%s (enabled=%s, active=%s)",
                SoulBeastStateManager.isPermanent(player),
                SoulBeastStateManager.isEnabled(player),
                SoulBeastStateManager.isActive(player))), true);
        return 1;
    }

    private static int showInfo(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        boolean enabled = SoulBeastStateManager.isEnabled(player);
        boolean permanent = SoulBeastStateManager.isPermanent(player);
        boolean active = SoulBeastStateManager.isActive(player);
        var snapshot = net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.state.SoulBeastStateManager.getExisting(player);
        String source = snapshot.flatMap(s -> s.getSource()).map(Object::toString).orElse("<none>");
        long lastTick = snapshot.map(net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.state.SoulBeastState::getLastTick).orElse(0L);
        ctx.getSource().sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
                "[soulbeast] active=%s enabled=%s permanent=%s source=%s lastTick=%d",
                active, enabled, permanent, source, lastTick)), false);
        return 1;
    }

    @SubscribeEvent
    public static void onSoulBeastStateChanged(SoulBeastStateChangedEvent event) {
        if (!(event.entity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.level().isClientSide()) {
            return;
        }
        boolean before = event.previous().isSoulBeast();
        boolean after = event.current().isSoulBeast();
        if (before == after) {
            return;
        }
        Component message = Component.literal(String.format(Locale.ROOT,
                "[soulbeast] state %s (active=%s enabled=%s permanent=%s)",
                after ? "enabled" : "disabled",
                event.current().active(),
                event.current().enabled(),
                event.current().permanent()));
        player.sendSystemMessage(message);
    }
}

