package net.tigereye.chestcavity.soul.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.commands.arguments.UuidArgument;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.tigereye.chestcavity.soul.engine.SoulFeatureToggle;
import net.tigereye.chestcavity.soul.fakeplayer.SoulFakePlayerSpawner;
import net.tigereye.chestcavity.soul.fakeplayer.SoulFakePlayerSpawner.SoulPlayerInfo;
import net.tigereye.chestcavity.soul.util.SoulLog;
import net.tigereye.chestcavity.soul.util.SoulProfileOps;
import net.tigereye.chestcavity.registration.CCAttachments;
import net.tigereye.chestcavity.soul.container.SoulContainer;
import net.tigereye.chestcavity.soul.profile.InventorySnapshot;
import net.tigereye.chestcavity.soul.profile.PlayerEffectsSnapshot;
import net.tigereye.chestcavity.soul.profile.PlayerPositionSnapshot;
import net.tigereye.chestcavity.soul.profile.PlayerStatsSnapshot;
import net.tigereye.chestcavity.soul.profile.SoulProfile;

import java.util.Locale;
import java.util.UUID;

/**
 * Temporary soul command entry point; provides only the test hook until the soul system is fully implemented.
 */
public final class SoulCommands {

    private SoulCommands() {
    }

    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("soul")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("enable")
                        .executes(SoulCommands::enableSoulSystem))
                .then(Commands.literal("action")
                        .then(Commands.literal("start")
                                .then(Commands.argument("idOrName", StringArgumentType.string())
                                        .suggests((ctx, builder) -> net.tigereye.chestcavity.soul.fakeplayer.SoulFakePlayerSpawner.suggestSoulPlayerUuids(ctx.getSource(), builder))
                                        .then(Commands.argument("actionId", StringArgumentType.string())
                                                .executes(SoulCommands::actionStart))))
                        .then(Commands.literal("cancel")
                                .then(Commands.argument("idOrName", StringArgumentType.string())
                                        .suggests((ctx, builder) -> net.tigereye.chestcavity.soul.fakeplayer.SoulFakePlayerSpawner.suggestSoulPlayerUuids(ctx.getSource(), builder))
                                        .then(Commands.argument("actionId", StringArgumentType.string())
                                                .executes(SoulCommands::actionCancel))
                                        .executes(SoulCommands::actionCancelAll)))
                        .then(Commands.literal("status")
                                .then(Commands.argument("idOrName", StringArgumentType.string())
                                        .suggests((ctx, builder) -> net.tigereye.chestcavity.soul.fakeplayer.SoulFakePlayerSpawner.suggestSoulPlayerUuids(ctx.getSource(), builder))
                                        .executes(SoulCommands::actionStatus))))
                .then(Commands.literal("order")
                        .then(Commands.literal("follow")
                                .then(Commands.argument("idOrName", StringArgumentType.string())
                                        .suggests((ctx, builder) -> SoulFakePlayerSpawner.suggestSoulPlayerUuids(ctx.getSource(), builder))
                                        .executes(SoulCommands::orderFollow)))
                .then(Commands.literal("testheal")
                        .then(Commands.argument("idOrName", StringArgumentType.string())
                                .suggests((ctx, builder) -> SoulFakePlayerSpawner.suggestSoulPlayerUuids(ctx.getSource(), builder))
                                .then(Commands.argument("type", StringArgumentType.word())
                                        .executes(SoulCommands::testHealDefaultOffhand)
                                        .then(Commands.argument("hand", StringArgumentType.word())
                                                .executes(SoulCommands::testHeal)))))
                        .then(Commands.literal("guard")
                                .then(Commands.argument("idOrName", StringArgumentType.string())
                                        .suggests((ctx, builder) -> SoulFakePlayerSpawner.suggestSoulPlayerUuids(ctx.getSource(), builder))
                                        .executes(SoulCommands::orderGuard)))
                        .then(Commands.literal("forcefight")
                                .then(Commands.argument("idOrName", StringArgumentType.string())
                                        .suggests((ctx, builder) -> SoulFakePlayerSpawner.suggestSoulPlayerUuids(ctx.getSource(), builder))
                                        .executes(SoulCommands::orderForceFight)))
                        .then(Commands.literal("idle")
                                .then(Commands.argument("idOrName", StringArgumentType.string())
                                        .suggests((ctx, builder) -> SoulFakePlayerSpawner.suggestSoulPlayerUuids(ctx.getSource(), builder))
                                        .executes(SoulCommands::orderIdle))))
                .then(Commands.literal("name")
                        .then(Commands.literal("set")
                                .then(Commands.argument("idOrName", StringArgumentType.string())
                                        .suggests((ctx, builder) -> SoulFakePlayerSpawner.suggestSoulPlayerUuids(ctx.getSource(), builder))
                                        .then(Commands.argument("newName", StringArgumentType.greedyString())
                                                .executes(SoulCommands::renameSoul))))
                        .then(Commands.literal("apply")
                                .then(Commands.argument("idOrName", StringArgumentType.string())
                                        .suggests((ctx, builder) -> SoulFakePlayerSpawner.suggestSoulPlayerUuids(ctx.getSource(), builder))
                                        .executes(SoulCommands::applyNameNow))))
                .then(Commands.literal("skin")
                        .then(Commands.literal("set")
                                .then(Commands.argument("idOrName", StringArgumentType.string())
                                        .suggests((ctx, builder) -> SoulFakePlayerSpawner.suggestSoulPlayerUuids(ctx.getSource(), builder))
                                        .then(Commands.argument("mojangName", StringArgumentType.word())
                                                .executes(SoulCommands::skinSet))))
                        .then(Commands.literal("apply")
                                .then(Commands.argument("idOrName", StringArgumentType.string())
                                        .suggests((ctx, builder) -> SoulFakePlayerSpawner.suggestSoulPlayerUuids(ctx.getSource(), builder))
                                        .executes(SoulCommands::skinApply))))
                .then(Commands.literal("control")
                        .then(Commands.literal("owner")
                                .executes(SoulCommands::switchOwner))
                        .then(Commands.argument("idOrName", StringArgumentType.string())
                                .suggests((ctx, builder) -> SoulFakePlayerSpawner.suggestSoulPlayerUuids(ctx.getSource(), builder))
                                .executes(SoulCommands::switchSoulPlayer)))
                .then(Commands.literal("autospawn")
                        .then(Commands.literal("on")
                                .then(Commands.argument("idOrName", StringArgumentType.string())
                                        .suggests((ctx, builder) -> SoulFakePlayerSpawner.suggestSoulPlayerUuids(ctx.getSource(), builder))
                                        .executes(ctx -> setAutospawn(ctx, true))))
                        .then(Commands.literal("off")
                                .then(Commands.argument("idOrName", StringArgumentType.string())
                                        .suggests((ctx, builder) -> SoulFakePlayerSpawner.suggestSoulPlayerUuids(ctx.getSource(), builder))
                                        .executes(ctx -> setAutospawn(ctx, false)))))
                .then(Commands.literal("test")
                        .then(Commands.literal("SoulPlayerList")
                                .executes(SoulCommands::listSoulPlayers))
                        .then(Commands.literal("SoulPlayerSwitch")
                                .then(Commands.literal("owner")
                                        .executes(SoulCommands::switchOwner))
                                .then(Commands.argument("idOrName", StringArgumentType.string())
                                        .suggests((ctx, builder) -> SoulFakePlayerSpawner.suggestSoulPlayerUuids(ctx.getSource(), builder))
                                        .executes(SoulCommands::switchSoulPlayer)))
                        .then(Commands.literal("SoulPlayerRemove")
                                .then(Commands.argument("idOrName", StringArgumentType.string())
                                        .suggests((ctx, builder) -> SoulFakePlayerSpawner.suggestSoulPlayerUuids(ctx.getSource(), builder))
                                        .executes(SoulCommands::removeSoulPlayer)))
                        .then(Commands.literal("spawnFakePlayer")
                                .executes(SoulCommands::spawnFakePlayer))
                        .then(Commands.literal("CreateSoulDefault")
                                .executes(SoulCommands::createSoulDefault))
                        .then(Commands.literal("CreateSoulAt")
                                .then(Commands.argument("x", com.mojang.brigadier.arguments.DoubleArgumentType.doubleArg())
                                        .then(Commands.argument("y", com.mojang.brigadier.arguments.DoubleArgumentType.doubleArg())
                                                .then(Commands.argument("z", com.mojang.brigadier.arguments.DoubleArgumentType.doubleArg())
                                                        .executes(SoulCommands::createSoulAt)))))
                        .then(Commands.literal("saveAll")
                                .executes(SoulCommands::saveAll))));
    }

    private static int testHealDefaultOffhand(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return testHealWithHand(context, "offhand");
    }

    private static int testHeal(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String hand = StringArgumentType.getString(context, "hand");
        return testHealWithHand(context, hand);
    }

    private static int testHealWithHand(CommandContext<CommandSourceStack> context, String handToken) throws CommandSyntaxException {
        ServerPlayer executor = context.getSource().getPlayerOrException();
        String sidToken = unquote(StringArgumentType.getString(context, "idOrName"));
        String type = StringArgumentType.getString(context, "type");
        var soulOpt = SoulFakePlayerSpawner.findSoulPlayer(
                SoulFakePlayerSpawner.resolveSoulUuidFlexible(executor, sidToken).orElse(null));
        if (soulOpt.isEmpty()) { context.getSource().sendFailure(Component.literal("[soul] 未找到 SoulPlayer。")); return 0; }
        var soul = soulOpt.get();
        if (!executor.getUUID().equals(soul.getOwnerId().orElse(null))) {
            context.getSource().sendFailure(Component.literal("[soul] 你无权控制该 SoulPlayer。"));
            return 0;
        }

        net.minecraft.world.InteractionHand hand = switch (handToken.toLowerCase(Locale.ROOT)) {
            case "main", "mainhand", "m", "0" -> net.minecraft.world.InteractionHand.MAIN_HAND;
            default -> net.minecraft.world.InteractionHand.OFF_HAND;
        };

        net.minecraft.world.item.ItemStack stack;
        switch (type.toLowerCase(Locale.ROOT)) {
            case "potion", "heal_potion", "instant_health", "healing" -> {
                // Instant health potion (drinkable)
                stack = net.minecraft.world.item.alchemy.PotionContents.createItemStack(
                        net.minecraft.world.item.Items.POTION,
                        net.minecraft.world.item.alchemy.Potions.HEALING
                );
            }
            case "gap", "golden_apple", "ga" -> {
                stack = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.GOLDEN_APPLE);
            }
            case "egap", "enchanted_golden_apple", "ega" -> {
                stack = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ENCHANTED_GOLDEN_APPLE);
            }
            default -> {
                context.getSource().sendFailure(Component.literal("[soul] 未知类型: " + type + " (可用: potion|golden_apple|enchanted_golden_apple)"));
                return 0;
            }
        }

        float beforeHp = soul.getHealth();
        net.minecraft.world.item.ItemStack prev = soul.getItemInHand(hand).copy();
        soul.setItemInHand(hand, stack);
        boolean used = net.tigereye.chestcavity.soul.util.SoulPlayerInput.rightMouseItemUse(soul, hand, true);
        // return remain to inventory, then restore previous hand
        net.minecraft.world.item.ItemStack remain = soul.getItemInHand(hand);
        soul.setItemInHand(hand, net.minecraft.world.item.ItemStack.EMPTY);
        if (!remain.isEmpty()) {
            var inv = soul.getInventory();
            if (!inv.add(remain.copy())) {
                soul.drop(remain.copy(), false);
            }
        }
        soul.setItemInHand(hand, prev);
        float afterHp = soul.getHealth();
        String msg = String.format("[soul] testheal type=%s hand=%s used=%s hp: %.1f -> %.1f", type, hand.name().toLowerCase(Locale.ROOT), used, beforeHp, afterHp);
        context.getSource().sendSuccess(() -> Component.literal(msg), true);
        return used ? 1 : 0;
    }

    private static int actionStart(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer executor = context.getSource().getPlayerOrException();
        if (!net.tigereye.chestcavity.soul.engine.SoulFeatureToggle.isEnabled()) {
            context.getSource().sendFailure(Component.literal("[soul] 请先执行 /soul enable 后再下达动作。"));
            return 0;
        }
        String sidToken = unquote(StringArgumentType.getString(context, "idOrName"));
        String actionStr = StringArgumentType.getString(context, "actionId");
        var soulOpt = net.tigereye.chestcavity.soul.fakeplayer.SoulFakePlayerSpawner.findSoulPlayer(
                net.tigereye.chestcavity.soul.fakeplayer.SoulFakePlayerSpawner.resolveSoulUuidFlexible(executor, sidToken).orElse(null));
        if (soulOpt.isEmpty()) { context.getSource().sendFailure(Component.literal("[soul] 未找到 SoulPlayer。")); return 0; }
        var soul = soulOpt.get();
        if (!executor.getUUID().equals(soul.getOwnerId().orElse(null))) {
            context.getSource().sendFailure(Component.literal("[soul] 你无权控制该 SoulPlayer。"));
            return 0;
        }
        var id = net.minecraft.resources.ResourceLocation.parse(actionStr);
        var action = net.tigereye.chestcavity.soul.fakeplayer.actions.registry.ActionRegistry.resolveOrCreate(id);
        if (action == null) { context.getSource().sendFailure(Component.literal("[soul] 未注册的 Action: " + actionStr)); return 0; }
        boolean ok = net.tigereye.chestcavity.soul.fakeplayer.actions.state.ActionStateManager.of(soul)
                .tryStart(soul.serverLevel(), soul, action, executor);
        if (ok) {
            context.getSource().sendSuccess(() -> Component.literal("[soul] 已启动动作: " + actionStr), true);
            return 1;
        } else {
            context.getSource().sendFailure(Component.literal("[soul] 无法启动，可能已在运行或条件不满足。"));
            return 0;
        }
    }

    private static int actionCancel(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer executor = context.getSource().getPlayerOrException();
        String sidToken = unquote(StringArgumentType.getString(context, "idOrName"));
        String actionStr = StringArgumentType.getString(context, "actionId");
        var soulOpt = net.tigereye.chestcavity.soul.fakeplayer.SoulFakePlayerSpawner.findSoulPlayer(
                net.tigereye.chestcavity.soul.fakeplayer.SoulFakePlayerSpawner.resolveSoulUuidFlexible(executor, sidToken).orElse(null));
        if (soulOpt.isEmpty()) { context.getSource().sendFailure(Component.literal("[soul] 未找到 SoulPlayer。")); return 0; }
        var soul = soulOpt.get();
        var id = net.minecraft.resources.ResourceLocation.parse(actionStr);
        var action = net.tigereye.chestcavity.soul.fakeplayer.actions.registry.ActionRegistry.find(id);
        if (action == null) { context.getSource().sendFailure(Component.literal("[soul] 未注册的 Action: " + actionStr)); return 0; }
        net.tigereye.chestcavity.soul.fakeplayer.actions.state.ActionStateManager.of(soul)
                .cancel(soul.serverLevel(), soul, action, executor);
        context.getSource().sendSuccess(() -> Component.literal("[soul] 已取消动作: " + actionStr), true);
        return 1;
    }

    private static int actionCancelAll(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer executor = context.getSource().getPlayerOrException();
        String sidToken = unquote(StringArgumentType.getString(context, "idOrName"));
        var soulOpt = net.tigereye.chestcavity.soul.fakeplayer.SoulFakePlayerSpawner.findSoulPlayer(
                net.tigereye.chestcavity.soul.fakeplayer.SoulFakePlayerSpawner.resolveSoulUuidFlexible(executor, sidToken).orElse(null));
        if (soulOpt.isEmpty()) { context.getSource().sendFailure(Component.literal("[soul] 未找到 SoulPlayer。")); return 0; }
        var soul = soulOpt.get();
        var mgr = net.tigereye.chestcavity.soul.fakeplayer.actions.state.ActionStateManager.of(soul);
        for (var rt : new java.util.ArrayList<>(mgr.active())) {
            var act = net.tigereye.chestcavity.soul.fakeplayer.actions.registry.ActionRegistry.find(rt.id);
            if (act != null) mgr.cancel(soul.serverLevel(), soul, act, executor);
        }
        context.getSource().sendSuccess(() -> Component.literal("[soul] 已取消全部动作。"), true);
        return 1;
    }

    private static int actionStatus(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer executor = context.getSource().getPlayerOrException();
        String sidToken = unquote(StringArgumentType.getString(context, "idOrName"));
        var soulOpt = net.tigereye.chestcavity.soul.fakeplayer.SoulFakePlayerSpawner.findSoulPlayer(
                net.tigereye.chestcavity.soul.fakeplayer.SoulFakePlayerSpawner.resolveSoulUuidFlexible(executor, sidToken).orElse(null));
        if (soulOpt.isEmpty()) { context.getSource().sendFailure(Component.literal("[soul] 未找到 SoulPlayer。")); return 0; }
        var soul = soulOpt.get();
        var mgr = net.tigereye.chestcavity.soul.fakeplayer.actions.state.ActionStateManager.of(soul);
        int count = 0;
        for (var rt : mgr.active()) {
            context.getSource().sendSuccess(() -> Component.literal("- " + rt.id + " step=" + rt.step + " next=" + rt.nextReadyAt), false);
            count++;
        }
        if (count == 0) {
            context.getSource().sendSuccess(() -> Component.literal("[soul] 当前没有运行的动作。"), false);
        }
        return 1;
    }

    private static String unquote(String s) {
        if (s == null) return null;
        String t = s.trim();
        if (t.length() >= 2 && t.startsWith("\"") && t.endsWith("\"")) {
            return t.substring(1, t.length() - 1);
        }
        return t;
    }

    private static int orderFollow(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer executor = context.getSource().getPlayerOrException();
        if (!net.tigereye.chestcavity.soul.engine.SoulFeatureToggle.isEnabled()) {
            context.getSource().sendFailure(Component.literal("[soul] 请先执行 /soul enable 后再下达订单。"));
            return 0;
        }
        String token = unquote(StringArgumentType.getString(context, "idOrName"));
        if ("@a".equalsIgnoreCase(token)) {
            int count = 0;
            for (UUID sid : SoulFakePlayerSpawner.getOwnedSoulIds(executor.getUUID())) {
                var sp = SoulFakePlayerSpawner.findSoulPlayer(sid);
                if (sp.isEmpty()) continue;
                net.tigereye.chestcavity.soul.ai.SoulAIOrders.set(executor, sid, net.tigereye.chestcavity.soul.ai.SoulAIOrders.Order.FOLLOW, "order-follow-all");
                count++;
            }
            final int total = count;
            context.getSource().sendSuccess(() -> Component.literal("[soul] 已对所有分魂设置 FOLLOW，共 " + total + " 个。"), true);
            return count;
        }
        UUID uuid = SoulFakePlayerSpawner.resolveSoulUuidFlexible(executor, token).orElse(null);
        var soulOpt = SoulFakePlayerSpawner.findSoulPlayer(uuid);
        if (soulOpt.isEmpty()) {
            context.getSource().sendFailure(Component.literal("[soul] 未找到该 SoulPlayer。"));
            return 0;
        }
        var soul = soulOpt.get();
        UUID owner = soul.getOwnerId().orElse(null);
        if (owner == null || !owner.equals(executor.getUUID())) {
            context.getSource().sendFailure(Component.literal("[soul] 你无权对该 SoulPlayer 下达订单。"));
            return 0;
        }
        net.tigereye.chestcavity.soul.ai.SoulAIOrders.set(executor, soul.getSoulId(), net.tigereye.chestcavity.soul.ai.SoulAIOrders.Order.FOLLOW, "order-follow");
        context.getSource().sendSuccess(() -> Component.literal("[soul] 已设置 FOLLOW。超过5格将跟随你当前操控的身体。"), true);
        return 1;
    }

    private static int orderIdle(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer executor = context.getSource().getPlayerOrException();
        if (!net.tigereye.chestcavity.soul.engine.SoulFeatureToggle.isEnabled()) {
            context.getSource().sendFailure(Component.literal("[soul] 请先执行 /soul enable 后再下达订单。"));
            return 0;
        }
        String token = unquote(StringArgumentType.getString(context, "idOrName"));
        if ("@a".equalsIgnoreCase(token)) {
            int count = 0;
            for (UUID sid : SoulFakePlayerSpawner.getOwnedSoulIds(executor.getUUID())) {
                var sp = SoulFakePlayerSpawner.findSoulPlayer(sid);
                if (sp.isEmpty()) continue;
                net.tigereye.chestcavity.soul.ai.SoulAIOrders.set(executor, sid, net.tigereye.chestcavity.soul.ai.SoulAIOrders.Order.IDLE, "order-idle-all");
                net.tigereye.chestcavity.soul.navigation.SoulNavigationMirror.clearGoal(sp.get());
                count++;
            }
            final int total = count;
            context.getSource().sendSuccess(() -> Component.literal("[soul] 已对所有分魂设置 IDLE，共 " + total + " 个。"), true);
            return count;
        }
        UUID uuid = SoulFakePlayerSpawner.resolveSoulUuidFlexible(executor, token).orElse(null);
        var soulOpt = SoulFakePlayerSpawner.findSoulPlayer(uuid);
        if (soulOpt.isEmpty()) {
            context.getSource().sendFailure(Component.literal("[soul] 未找到该 SoulPlayer。"));
            return 0;
        }
        var soul = soulOpt.get();
        UUID owner = soul.getOwnerId().orElse(null);
        if (owner == null || !owner.equals(executor.getUUID())) {
            context.getSource().sendFailure(Component.literal("[soul] 你无权对该 SoulPlayer 下达订单。"));
            return 0;
        }
        net.tigereye.chestcavity.soul.ai.SoulAIOrders.set(executor, soul.getSoulId(), net.tigereye.chestcavity.soul.ai.SoulAIOrders.Order.IDLE, "order-idle");
        net.tigereye.chestcavity.soul.navigation.SoulNavigationMirror.clearGoal(soul);
        context.getSource().sendSuccess(() -> Component.literal("[soul] 已设置 IDLE。"), true);
        return 1;
    }

    private static int orderGuard(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer executor = context.getSource().getPlayerOrException();
        if (!net.tigereye.chestcavity.soul.engine.SoulFeatureToggle.isEnabled()) {
            context.getSource().sendFailure(Component.literal("[soul] 请先执行 /soul enable 后再下达订单。"));
            return 0;
        }
        String token = unquote(StringArgumentType.getString(context, "idOrName"));
        if ("@a".equalsIgnoreCase(token)) {
            int count = 0;
            for (UUID sid : SoulFakePlayerSpawner.getOwnedSoulIds(executor.getUUID())) {
                var sp = SoulFakePlayerSpawner.findSoulPlayer(sid);
                if (sp.isEmpty()) continue;
                net.tigereye.chestcavity.soul.ai.SoulAIOrders.set(executor, sid, net.tigereye.chestcavity.soul.ai.SoulAIOrders.Order.GUARD, "order-guard-all");
                count++;
            }
            final int total = count;
            context.getSource().sendSuccess(() -> Component.literal("[soul] 已对所有分魂设置 GUARD，共 " + total + " 个。"), true);
            return count;
        }
        UUID uuid = net.tigereye.chestcavity.soul.fakeplayer.SoulFakePlayerSpawner.resolveSoulUuidFlexible(executor, token).orElse(null);
        var soulOpt = net.tigereye.chestcavity.soul.fakeplayer.SoulFakePlayerSpawner.findSoulPlayer(uuid);
        if (soulOpt.isEmpty()) {
            context.getSource().sendFailure(Component.literal("[soul] 未找到该 SoulPlayer。"));
            return 0;
        }
        var soul = soulOpt.get();
        UUID owner = soul.getOwnerId().orElse(null);
        if (owner == null || !owner.equals(executor.getUUID())) {
            context.getSource().sendFailure(Component.literal("[soul] 你无权对该 SoulPlayer 下达订单。"));
            return 0;
        }
        net.tigereye.chestcavity.soul.ai.SoulAIOrders.set(executor, soul.getSoulId(), net.tigereye.chestcavity.soul.ai.SoulAIOrders.Order.GUARD, "order-guard");
        context.getSource().sendSuccess(() -> Component.literal("[soul] 已设置 GUARD。在你周围16格内，仅在自身生命值大于目标2倍，且区域内不存在更强敌人时才会追击。"), true);
        return 1;
    }

    private static int orderForceFight(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer executor = context.getSource().getPlayerOrException();
        if (!net.tigereye.chestcavity.soul.engine.SoulFeatureToggle.isEnabled()) {
            context.getSource().sendFailure(Component.literal("[soul] 请先执行 /soul enable 后再下达订单。"));
            return 0;
        }
        String token = unquote(StringArgumentType.getString(context, "idOrName"));
        if ("@a".equalsIgnoreCase(token)) {
            int count = 0;
            for (UUID sid : SoulFakePlayerSpawner.getOwnedSoulIds(executor.getUUID())) {
                var sp = SoulFakePlayerSpawner.findSoulPlayer(sid);
                if (sp.isEmpty()) continue;
                net.tigereye.chestcavity.soul.ai.SoulAIOrders.set(executor, sid, net.tigereye.chestcavity.soul.ai.SoulAIOrders.Order.FORCE_FIGHT, "order-forcefight-all");
                count++;
            }
            final int total = count;
            context.getSource().sendSuccess(() -> Component.literal("[soul] 已对所有分魂设置 FORCE_FIGHT，共 " + total + " 个。"), true);
            return count;
        }
        UUID uuid = net.tigereye.chestcavity.soul.fakeplayer.SoulFakePlayerSpawner.resolveSoulUuidFlexible(executor, token).orElse(null);
        var soulOpt = net.tigereye.chestcavity.soul.fakeplayer.SoulFakePlayerSpawner.findSoulPlayer(uuid);
        if (soulOpt.isEmpty()) {
            context.getSource().sendFailure(Component.literal("[soul] 未找到该 SoulPlayer。"));
            return 0;
        }
        var soul = soulOpt.get();
        UUID owner = soul.getOwnerId().orElse(null);
        if (owner == null || !owner.equals(executor.getUUID())) {
            context.getSource().sendFailure(Component.literal("[soul] 你无权对该 SoulPlayer 下达订单。"));
            return 0;
        }
        net.tigereye.chestcavity.soul.ai.SoulAIOrders.set(executor, soul.getSoulId(), net.tigereye.chestcavity.soul.ai.SoulAIOrders.Order.FORCE_FIGHT, "order-forcefight");
        context.getSource().sendSuccess(() -> Component.literal("[soul] 已设置 FORCE_FIGHT。在你周围16格内，无差别进攻所有生物（排除你与友方分魂）。"), true);
        return 1;
    }

    private static int renameSoul(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer executor = context.getSource().getPlayerOrException();
        if (!net.tigereye.chestcavity.soul.engine.SoulFeatureToggle.isEnabled()) {
            context.getSource().sendFailure(Component.literal("[soul] 请先执行 /soul enable 后再重命名。"));
            return 0;
        }
        String token = unquote(StringArgumentType.getString(context, "idOrName"));
        String newName = unquote(StringArgumentType.getString(context, "newName"));
        var resolved = SoulFakePlayerSpawner.resolveSoulUuidFlexible(executor, token);
        if (resolved.isEmpty()) {
            context.getSource().sendFailure(Component.literal("[soul] 未找到该 SoulPlayer。"));
            return 0;
        }
        UUID soulId = resolved.get();
        boolean ok = SoulFakePlayerSpawner.rename(executor, soulId, newName, false);
        if (ok) {
            context.getSource().sendSuccess(() -> Component.literal("[soul] 已重命名为: " + newName), true);
            return 1;
        }
        context.getSource().sendFailure(Component.literal("[soul] 重命名失败。"));
        return 0;
    }

    private static int applyNameNow(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer executor = context.getSource().getPlayerOrException();
        String token = unquote(StringArgumentType.getString(context, "idOrName"));
        var resolved = SoulFakePlayerSpawner.resolveSoulUuidFlexible(executor, token);
        if (resolved.isEmpty()) {
            context.getSource().sendFailure(Component.literal("[soul] 未找到该 SoulPlayer。"));
            return 0;
        }
        UUID soulId = resolved.get();
        String name = CCAttachments.getSoulContainer(executor).getName(soulId);
        boolean ok = SoulFakePlayerSpawner.rename(executor, soulId, name, true);
        if (ok) {
            context.getSource().sendSuccess(() -> Component.literal("[soul] 已应用名称变更。"), true);
            return 1;
        }
        context.getSource().sendFailure(Component.literal("[soul] 应用失败。"));
        return 0;
    }

    private static int skinSet(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer executor = context.getSource().getPlayerOrException();
        if (!net.tigereye.chestcavity.soul.engine.SoulFeatureToggle.isEnabled()) {
            context.getSource().sendFailure(Component.literal("[soul] 请先执行 /soul enable 后再设置皮肤。"));
            return 0;
        }
        String token = unquote(StringArgumentType.getString(context, "idOrName"));
        String mojangName = unquote(StringArgumentType.getString(context, "mojangName"));
        var resolved = SoulFakePlayerSpawner.resolveSoulUuidFlexible(executor, token);
        if (resolved.isEmpty()) {
            context.getSource().sendFailure(Component.literal("[soul] 未找到该 SoulPlayer。"));
            return 0;
        }
        UUID soulId = resolved.get();
        boolean ok = SoulFakePlayerSpawner.setSkinFromMojangName(executor, soulId, mojangName, false);
        if (ok) {
            context.getSource().sendSuccess(() -> Component.literal("[soul] 皮肤已设置（需 /soul skin apply 应用）。"), true);
            return 1;
        }
        context.getSource().sendFailure(Component.literal("[soul] 皮肤设置失败（用户不存在或服务不可用）。"));
        return 0;
    }

    private static int skinApply(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer executor = context.getSource().getPlayerOrException();
        String token = unquote(StringArgumentType.getString(context, "idOrName"));
        var resolved = SoulFakePlayerSpawner.resolveSoulUuidFlexible(executor, token);
        if (resolved.isEmpty()) {
            context.getSource().sendFailure(Component.literal("[soul] 未找到该 SoulPlayer。"));
            return 0;
        }
        UUID soulId = resolved.get();
        // Force respawn to make clients see current identity properties (textures)
        if (net.tigereye.chestcavity.soul.fakeplayer.SoulFakePlayerSpawner.respawnForOwner(executor, soulId).isPresent()) {
            context.getSource().sendSuccess(() -> Component.literal("[soul] 已尝试重新生成以应用皮肤缓存。"), true);
            return 1;
        }
        context.getSource().sendFailure(Component.literal("[soul] 应用皮肤失败。"));
        return 0;
    }

    private static int setAutospawn(CommandContext<CommandSourceStack> context, boolean value) throws CommandSyntaxException {
        ServerPlayer executor = context.getSource().getPlayerOrException();
        if (!net.tigereye.chestcavity.soul.engine.SoulFeatureToggle.isEnabled()) {
            context.getSource().sendFailure(Component.literal("[soul] 请先执行 /soul enable 后再设置自动生成。"));
            return 0;
        }
        String token = unquote(StringArgumentType.getString(context, "idOrName"));
        var resolved = SoulFakePlayerSpawner.resolveSoulUuidFlexible(executor, token);
        if (resolved.isEmpty()) {
            context.getSource().sendFailure(Component.literal("[soul] 未找到该 SoulPlayer。"));
            return 0;
        }
        UUID soulId = resolved.get();
        var container = CCAttachments.getSoulContainer(executor);
        container.setAutospawn(executor, soulId, value, value?"autospawn-on":"autospawn-off");
        context.getSource().sendSuccess(() -> Component.literal("[soul] 已" + (value?"启用":"关闭") + "自动生成。"), true);
        return 1;
    }

    private static int enableSoulSystem(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer executor = context.getSource().getPlayerOrException();
        SoulFeatureToggle.enable(executor);
        var container = net.tigereye.chestcavity.registration.CCAttachments.getSoulContainer(executor);
        container.setActiveProfile(executor.getUUID());
        container.getOrCreateProfile(executor.getUUID()).updateFrom(executor);
        return 1;
    }

    private static int spawnFakePlayer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer executor = source.getPlayerOrException();
        var resultOpt = SoulFakePlayerSpawner.spawnTestFakePlayer(executor);
        if (resultOpt.isPresent()) {
            var result = resultOpt.get();
            String name = result.soulPlayer().getGameProfile().getName();
            source.sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
                    "[soul] spawnFakePlayer -> %s",
                    name == null || name.isBlank() ? "<unnamed>" : name)), true);
            return 1;
        }
        source.sendFailure(Component.literal(String.format(Locale.ROOT,
                "[soul] spawnFakePlayer 失败：无法在 %s 生成伪玩家。",
                executor.serverLevel().dimension().location())));
        return 0;
    }

    // Create a new soul with empty inventory and default stats at the player's current position, then spawn it.
    private static int createSoulDefault(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer executor = context.getSource().getPlayerOrException();
        if (!SoulFeatureToggle.isEnabled()) {
            context.getSource().sendFailure(Component.literal("[soul] 请先执行 /soul enable 后再创建分魂。"));
            return 0;
        }
        UUID soulId = java.util.UUID.randomUUID();
        SoulContainer container = CCAttachments.getSoulContainer(executor);
        if (!container.hasProfile(soulId)) {
            int selected = Math.max(0, Math.min(8, executor.getInventory().selected));
            InventorySnapshot inv = new InventorySnapshot(net.minecraft.core.NonNullList.withSize(41, net.minecraft.world.item.ItemStack.EMPTY), selected);
            PlayerStatsSnapshot stats = PlayerStatsSnapshot.empty();
            PlayerEffectsSnapshot fx = PlayerEffectsSnapshot.empty();
            PlayerPositionSnapshot pos = PlayerPositionSnapshot.capture(executor);
            SoulProfile profile = SoulProfile.fromSnapshot(soulId, inv, stats, fx, pos);
            container.putProfile(soulId, profile);
            // 默认开启 autospawn，便于下次登录自动生成壳体
            container.setAutospawn(executor, soulId, true, "autospawn-default-on");
            SoulProfileOps.markContainerDirty(executor, container, "command-createSoulDefault");
        }
        var spawned = SoulFakePlayerSpawner.respawnForOwner(executor, soulId);
        if (spawned.isPresent()) {
            SoulLog.info("[soul] command-createSoulDefault owner={} soul={}", executor.getUUID(), soulId);
            String name = SoulFakePlayerSpawner.resolveDisplayName(executor, soulId);
            context.getSource().sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
                    "[soul] CreateSoulDefault -> %s", name)), true);
            return 1;
        }
        context.getSource().sendFailure(Component.literal("[soul] CreateSoulDefault 失败：无法生成分魂实体。"));
        return 0;
    }

    // Create a new soul with default stats and empty inventory at a given position in current dimension, then spawn it.
    private static int createSoulAt(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer executor = context.getSource().getPlayerOrException();
        if (!SoulFeatureToggle.isEnabled()) {
            context.getSource().sendFailure(Component.literal("[soul] 请先执行 /soul enable 后再创建分魂。"));
            return 0;
        }
        double x = com.mojang.brigadier.arguments.DoubleArgumentType.getDouble(context, "x");
        double y = com.mojang.brigadier.arguments.DoubleArgumentType.getDouble(context, "y");
        double z = com.mojang.brigadier.arguments.DoubleArgumentType.getDouble(context, "z");
        float yaw = executor.getYRot();
        float pitch = executor.getXRot();
        UUID soulId = java.util.UUID.randomUUID();
        SoulContainer container = CCAttachments.getSoulContainer(executor);
        if (!container.hasProfile(soulId)) {
            int selected = Math.max(0, Math.min(8, executor.getInventory().selected));
            InventorySnapshot inv = new InventorySnapshot(net.minecraft.core.NonNullList.withSize(41, net.minecraft.world.item.ItemStack.EMPTY), selected);
            PlayerStatsSnapshot stats = PlayerStatsSnapshot.empty();
            PlayerEffectsSnapshot fx = PlayerEffectsSnapshot.empty();
            PlayerPositionSnapshot pos = PlayerPositionSnapshot.of(executor.level().dimension(), x, y, z, yaw, pitch, yaw);
            SoulProfile profile = SoulProfile.fromSnapshot(soulId, inv, stats, fx, pos);
            container.putProfile(soulId, profile);
            // 默认开启 autospawn
            container.setAutospawn(executor, soulId, true, "autospawn-default-on");
            SoulProfileOps.markContainerDirty(executor, container, "command-createSoulAt");
        }
        var spawned = SoulFakePlayerSpawner.respawnForOwner(executor, soulId);
        if (spawned.isPresent()) {
            SoulLog.info("[soul] command-createSoulAt owner={} soul={} pos=({},{},{})",
                    executor.getUUID(), soulId, x, y, z);
            String name = SoulFakePlayerSpawner.resolveDisplayName(executor, soulId);
            context.getSource().sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
                    "[soul] CreateSoulAt -> %s @ (%.1f, %.1f, %.1f)", name, x, y, z)), true);
            return 1;
        }
        context.getSource().sendFailure(Component.literal("[soul] CreateSoulAt 失败：无法生成分魂实体。"));
        return 0;
    }

    private static int listSoulPlayers(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer executor = context.getSource().getPlayerOrException();
        if (!SoulFeatureToggle.isEnabled()) {
            context.getSource().sendFailure(Component.literal("[soul] 请先执行 /soul enable 后再查看 SoulPlayer 列表。"));
            return 0;
        }
        UUID ownerFilter = executor.getUUID();
        var entries = SoulFakePlayerSpawner.listActive().stream()
                .filter(info -> info.ownerId() != null && info.ownerId().equals(ownerFilter))
                .toList();
        SoulLog.info("[soul] command-list owner={} count={}", ownerFilter, entries.size());
        if (entries.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal("[soul] 暂无活跃的 SoulPlayer。"), false);
            return 0;
        }
        for (SoulPlayerInfo info : entries) {
            String line = String.format(Locale.ROOT, "[soul] %s soul=%s", info.active() ? "*" : "-", info.soulUuid());
            context.getSource().sendSuccess(() -> Component.literal(line), false);
        }
        return entries.size();
    }

    private static int switchOwner(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer executor = context.getSource().getPlayerOrException();
        if (!SoulFeatureToggle.isEnabled()) {
            context.getSource().sendFailure(Component.literal("[soul] 请先执行 /soul enable 后再切换 SoulPlayer。"));
            return 0;
        }
        try {
            if (!SoulFakePlayerSpawner.switchTo(executor, executor.getUUID())) {
                context.getSource().sendFailure(Component.literal("[soul] 未能切换回本体魂档。"));
                return 0;
            }
        } catch (Exception e) {
            SoulLog.error("[soul] switchOwner command failed for owner={}", e, executor.getUUID());
            context.getSource().sendFailure(Component.literal("[soul] 试图执行该命令时出现意外错误 (owner)。请查看日志。"));
            return 0;
        }
        SoulLog.info("[soul] command-switch owner={} target=owner", executor.getUUID());
        context.getSource().sendSuccess(() -> Component.literal("[soul] 已切换回本体魂档。"), true);
        return 1;
    }

    private static int switchSoulPlayer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer executor = context.getSource().getPlayerOrException();
        if (!SoulFeatureToggle.isEnabled()) {
            context.getSource().sendFailure(Component.literal("[soul] 请先执行 /soul enable 后再切换 SoulPlayer。"));
            return 0;
        }
        String token = unquote(StringArgumentType.getString(context, "idOrName"));
        var resolved = SoulFakePlayerSpawner.resolveSoulUuidFlexible(executor, token);
        if (resolved.isEmpty()) {
            context.getSource().sendFailure(Component.literal("[soul] 无效的标识或不可用：请使用 profile-UUID、entity-UUID、玩家为该魂设置的名字，或输入 owner。"));
            return 0;
        }
        UUID uuid = resolved.get();
        try {
            if (!SoulFakePlayerSpawner.switchTo(executor, uuid)) {
                context.getSource().sendFailure(Component.literal(String.format(Locale.ROOT,
                        "[soul] 未找到 UUID=%s 的 SoulPlayer，或你无权切换。", uuid)));
                return 0;
            }
        } catch (Exception e) {
            SoulLog.error("[soul] switchSoulPlayer command failed owner={} target={}", e, executor.getUUID(), uuid);
            context.getSource().sendFailure(Component.literal("[soul] 试图执行该命令时出现意外错误 (switch)。请查看日志。"));
            return 0;
        }
        String name = SoulFakePlayerSpawner.resolveDisplayName(executor, uuid);
        SoulLog.info("[soul] command-switch owner={} target={}/{} ", executor.getUUID(), name, uuid);
        context.getSource().sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
                "[soul] 已切换至 %s。", name)), true);
        return 1;
    }

    private static int removeSoulPlayer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer executor = context.getSource().getPlayerOrException();
        if (!SoulFeatureToggle.isEnabled()) {
            context.getSource().sendFailure(Component.literal("[soul] 请先执行 /soul enable 后再移除 SoulPlayer。"));
            return 0;
        }
        String token = unquote(StringArgumentType.getString(context, "idOrName"));
        var resolved = SoulFakePlayerSpawner.resolveSoulUuidFlexible(executor, token);
        if (resolved.isEmpty()) {
            context.getSource().sendFailure(Component.literal("[soul] 无效的标识或不可用：请使用 profile-UUID、entity-UUID、设置的名字，或 owner。"));
            return 0;
        }
        UUID uuid = resolved.get();
        String disp = SoulFakePlayerSpawner.resolveDisplayName(executor, uuid);
        if (!SoulFakePlayerSpawner.remove(uuid, executor)) {
            context.getSource().sendFailure(Component.literal(String.format(Locale.ROOT,
                    "[soul] 未找到 UUID=%s 的 SoulPlayer 或你无权移除。", uuid)));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
                "[soul] 已移除 %s。", disp)), true);
        return 1;
    }

    private static int saveAll(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer executor = context.getSource().getPlayerOrException();
        if (!SoulFeatureToggle.isEnabled()) {
            context.getSource().sendFailure(Component.literal("[soul] 请先执行 /soul enable 后再保存魂档。"));
            return 0;
        }
        int saved = SoulFakePlayerSpawner.saveAll(executor);
        context.getSource().sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
                "[soul] 已保存 %d 个 SoulPlayer 状态。", saved)), true);
        return saved;
    }
}
 
