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
import net.tigereye.chestcavity.soul.navigation.SoulGoalPlanner;
import net.tigereye.chestcavity.soul.navigation.SoulNavEngine;
import net.tigereye.chestcavity.soul.navigation.SoulNavigationMirror;
import net.tigereye.chestcavity.soul.navigation.SoulNavigationTestHarness;
import net.tigereye.chestcavity.soul.util.SoulLog;
import net.tigereye.chestcavity.soul.util.SoulProfileOps;
import net.tigereye.chestcavity.registration.CCAttachments;
import net.tigereye.chestcavity.soul.container.SoulContainer;
import net.tigereye.chestcavity.soul.profile.InventorySnapshot;
import net.tigereye.chestcavity.soul.profile.PlayerEffectsSnapshot;
import net.tigereye.chestcavity.soul.profile.PlayerPositionSnapshot;
import net.tigereye.chestcavity.soul.profile.PlayerStatsSnapshot;
import net.tigereye.chestcavity.soul.profile.SoulProfile;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * 灵魂系统的临时指令入口。
 *
 * <p>当前阶段仅面向开发与调试，暴露“启用/禁用”、“生成/切换/移除”SoulPlayer 以及 AI 行为试验等命令。
 * 正式版本将迁移到数据驱动 UI 与玩家可见的引导流程，本类中的命令树与输出仍以内部诊断信息为主。</p>
 *
 * <p>维护者须关注：</p>
 * <ul>
 *     <li>涉及 SoulPlayer 的操作应先通过
 *     {@link net.tigereye.chestcavity.soul.fakeplayer.SoulFakePlayerSpawner#resolveSoulUuidFlexible(ServerPlayer, String)}
 *     做身份解析，再校验所有权；</li>
 *     <li>调试命令执行后需以 {@link net.tigereye.chestcavity.soul.util.SoulLog} 记录关键信息，以便问题复盘。</li>
 * </ul>
 */
public final class SoulCommands {

    private SoulCommands() {
    }

    /**
     * /soul nav engine <idOrName> <engine|clear>
     */
    private static int navSetEngine(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer executor = context.getSource().getPlayerOrException();
        String sidToken = unquote(StringArgumentType.getString(context, "idOrName"));
        String engineToken = StringArgumentType.getString(context, "engine");
        var soulOpt = SoulFakePlayerSpawner.findSoulPlayer(
                SoulFakePlayerSpawner.resolveSoulUuidFlexible(executor, sidToken).orElse(null));
        if (soulOpt.isEmpty()) { context.getSource().sendFailure(Component.literal("[soul] 未找到 SoulPlayer。")); return 0; }
        var soul = soulOpt.get();
        if (!executor.getUUID().equals(soul.getOwnerId().orElse(null))) {
            context.getSource().sendFailure(Component.literal("[soul] 你无权控制该 SoulPlayer。"));
            return 0;
        }

        if (engineToken.equalsIgnoreCase("clear")) {
            SoulNavigationMirror.setEngine(soul, null);
        } else {
            SoulNavEngine engine = SoulNavEngine.fromProperty(engineToken);
            SoulNavigationMirror.setEngine(soul, engine);
        }
        SoulNavEngine effective = SoulNavigationMirror.getEngine(soul);
        context.getSource().sendSuccess(() -> Component.literal("[soul] nav engine -> " + effective.name().toLowerCase(java.util.Locale.ROOT)), true);
        return 1;
    }

    private static int navDump(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer executor = context.getSource().getPlayerOrException();
        String sidToken = unquote(StringArgumentType.getString(context, "idOrName"));
        var soulOpt = SoulFakePlayerSpawner.findSoulPlayer(
                SoulFakePlayerSpawner.resolveSoulUuidFlexible(executor, sidToken).orElse(null));
        if (soulOpt.isEmpty()) { context.getSource().sendFailure(Component.literal("[soul] 未找到 SoulPlayer。")); return 0; }
        var soul = soulOpt.get();
        var engine = SoulNavigationMirror.getEngine(soul);
        boolean baritoneAvail = net.tigereye.chestcavity.soul.navigation.barintegrate.BaritoneFacade.isAvailable();
        String line = net.tigereye.chestcavity.soul.navigation.SoulNavigationMirror.debugLine(soul);
        String broker = net.tigereye.chestcavity.soul.navigation.net.SoulNavPlanBroker.debugSummary();
        context.getSource().sendSuccess(() -> Component.literal("[soul] nav dump: " + line + ", baritoneAvailable=" + baritoneAvail + ", broker{" + broker + "}"), false);
        return 1;
    }

    /**
     * 注册 {@code /soul} 指令树，按子命令分组覆盖启用开关、AI、动作、命令式移动与调试工具。
     *
     * @param event NeoForge 注入的注册事件，提供 {@link CommandDispatcher} 上下文。
     */
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("soul")
                .requires(source -> source.hasPermission(0))
                .then(Commands.literal("enable")
                        .executes(SoulCommands::enableSoulSystem))
                .then(Commands.literal("nav")
                        .then(Commands.literal("engine")
                                .then(Commands.argument("idOrName", StringArgumentType.string())
                                        .suggests((ctx, builder) -> SoulFakePlayerSpawner.suggestSoulPlayerUuids(ctx.getSource(), builder))
                                        .then(Commands.argument("engine", StringArgumentType.word())
                                                .suggests((ctx, builder) -> {
                                                    builder.suggest("vanilla");
                                                    builder.suggest("baritone");
                                                    builder.suggest("autostep");
                                                    builder.suggest("clear");
                                                    return builder.buildFuture();
                                                })
                                                .executes(SoulCommands::navSetEngine))))
                        .then(Commands.literal("dump")
                                .then(Commands.argument("idOrName", StringArgumentType.string())
                                        .suggests((ctx, builder) -> SoulFakePlayerSpawner.suggestSoulPlayerUuids(ctx.getSource(), builder))
                                        .executes(SoulCommands::navDump))))
                .then(Commands.literal("brain")
                        // /soul brain mode <idOrName> <mode>
                        .then(Commands.literal("mode")
                                .then(Commands.argument("idOrName", StringArgumentType.string())
                                        .suggests((ctx, builder) -> SoulFakePlayerSpawner.suggestSoulPlayerUuids(ctx.getSource(), builder))
                                        .then(Commands.argument("mode", StringArgumentType.word())
                                                .executes(SoulCommands::brainSetMode))))
                        // /soul brain intent combat <idOrName> <style> [ttl]
                        .then(Commands.literal("intent")
                                .then(Commands.literal("combat")
                                        .then(Commands.argument("idOrName", StringArgumentType.string())
                                                .suggests((ctx, builder) -> SoulFakePlayerSpawner.suggestSoulPlayerUuids(ctx.getSource(), builder))
                                                .then(Commands.argument("style", StringArgumentType.word())
                                                        .executes(ctx -> brainIntentCombat(ctx, /*ttl*/200))
                                                        .then(Commands.argument("ttl", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1))
                                                                .executes(ctx -> brainIntentCombat(ctx, com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "ttl"))))))))
                        // /soul brain clear <idOrName>
                        .then(Commands.literal("clear")
                                .then(Commands.argument("idOrName", StringArgumentType.string())
                                        .suggests((ctx, builder) -> SoulFakePlayerSpawner.suggestSoulPlayerUuids(ctx.getSource(), builder))
                                        .executes(SoulCommands::brainClear))))
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
                .then(Commands.literal("vacuum")
                        .then(Commands.literal("on").executes(ctx -> {
                            net.tigereye.chestcavity.soul.runtime.ItemVacuumHandler.setEnabled(true);
                            ctx.getSource().sendSuccess(() -> Component.literal("[soul] vacuum -> on"), true);
                            return 1;
                        }))
                        .then(Commands.literal("off").executes(ctx -> {
                            net.tigereye.chestcavity.soul.runtime.ItemVacuumHandler.setEnabled(false);
                            ctx.getSource().sendSuccess(() -> Component.literal("[soul] vacuum -> off"), true);
                            return 1;
                        }))
                        .then(Commands.literal("radius")
                                .then(Commands.argument("r", com.mojang.brigadier.arguments.DoubleArgumentType.doubleArg(0.5, 24.0))
                                        .executes(ctx -> {
                                            double r = com.mojang.brigadier.arguments.DoubleArgumentType.getDouble(ctx, "r");
                                            net.tigereye.chestcavity.soul.runtime.ItemVacuumHandler.setRadius(r);
                                            ctx.getSource().sendSuccess(() -> Component.literal("[soul] vacuum radius -> " + String.format(java.util.Locale.ROOT, "%.2f", r)), true);
                                            return 1;
                                        }))))
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
                        .then(Commands.literal("TestCollectEntityGoals")
                                .then(Commands.argument("uuid", UuidArgument.uuid())
                                        .suggests((ctx, builder) -> SoulFakePlayerSpawner.suggestSoulPlayerUuidLiterals(ctx.getSource(), builder))
                                        .executes(SoulCommands::testCollectEntityGoals)))
                        .then(Commands.literal("saveAll")
                                .executes(SoulCommands::saveAll))));
    }

    /**
     * {@code /soul test TestCollectEntityGoals <uuid>} 导航测试入口。
     */
    private static int testCollectEntityGoals(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer executor = source.getPlayerOrException();
        if (!SoulFeatureToggle.isEnabled()) {
            source.sendFailure(Component.literal("[soul] 请先执行 /soul enable 后再运行导航测试。"));
            return 0;
        }
        UUID token = UuidArgument.getUuid(context, "uuid");
        UUID soulId = SoulFakePlayerSpawner.resolveSoulUuid(token).orElse(token);
        var soulOpt = SoulFakePlayerSpawner.findSoulPlayer(soulId);
        if (soulOpt.isEmpty()) {
            source.sendFailure(Component.literal("[soul] 未找到该 SoulPlayer。"));
            return 0;
        }
        var soul = soulOpt.get();
        if (!executor.getUUID().equals(soul.getOwnerId().orElse(null))) {
            source.sendFailure(Component.literal("[soul] 你无权控制该 SoulPlayer。"));
            return 0;
        }

        double range = 24.0;
        double speed = 1.15;
        List<SoulGoalPlanner.NavigationGoal> goals =
                SoulGoalPlanner.collectEntityGoals(
                        soul,
                        range,
                        target -> target != null && target.isAlive(),
                        speed
                );
        if (goals.isEmpty()) {
            source.sendFailure(Component.literal(String.format(Locale.ROOT,
                    "[soul] 范围内没有可用目标（range=%.1f）。", range)));
            return 0;
        }

        boolean scheduled = SoulNavigationTestHarness.schedule(soul, goals);
        if (!scheduled) {
            source.sendFailure(Component.literal("[soul] 无法调度导航测试。"));
            return 0;
        }

        source.sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
                "[soul] TestCollectEntityGoals -> %d targets (range %.1f, speed %.2f)",
                goals.size(), range, speed)), true);
        return goals.size();
    }

    /**
     * {@code /soul testheal <id> <type>} 的便捷入口，默认使用副手。
     */
    private static int testHealDefaultOffhand(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return testHealWithHand(context, "offhand");
    }

    /**
     * {@code /soul testheal <id> <type> <hand>} 的通用处理入口。
     */
    private static int testHeal(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String hand = StringArgumentType.getString(context, "hand");
        return testHealWithHand(context, hand);
    }

    /**
     * 让目标 SoulPlayer 模拟使用治疗道具，用于在无 GUI 场景下快速验证 AI 的治疗行为。
     *
     * @param context Brigadier 命令上下文。
     * @param handToken 指定使用的手（main/offhand 别名均可）。
     */
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

    /**
     * 启动指定 {@code action}，允许运营或 QA 强制触发灵魂动作状态机。
     */
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

    // ----- brain 命令实现 -----
    /**
     * 将灵魂的 AI 模式切换为指定枚举值，并即时持久化到容器。
     */
    private static int brainSetMode(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer executor = context.getSource().getPlayerOrException();
        if (!net.tigereye.chestcavity.soul.engine.SoulFeatureToggle.isEnabled()) {
            context.getSource().sendFailure(Component.literal("[soul] 请先执行 /soul enable 后再设置 brain 模式."));
            return 0;
        }
        String token = unquote(StringArgumentType.getString(context, "idOrName"));
        var resolved = SoulFakePlayerSpawner.resolveSoulUuidFlexible(executor, token);
        if (resolved.isEmpty()) { context.getSource().sendFailure(Component.literal("[soul] 未找到该 SoulPlayer。")); return 0; }
        var soulOpt = SoulFakePlayerSpawner.findSoulPlayer(resolved.get());
        if (soulOpt.isEmpty()) { context.getSource().sendFailure(Component.literal("[soul] 该 SoulPlayer 当前不在线。")); return 0; }
        var soul = soulOpt.get();
        if (!executor.getUUID().equals(soul.getOwnerId().orElse(null))) {
            context.getSource().sendFailure(Component.literal("[soul] 你无权控制该 SoulPlayer。"));
            return 0;
        }
        String modeToken = unquote(StringArgumentType.getString(context, "mode"));
        net.tigereye.chestcavity.soul.fakeplayer.brain.BrainMode mode;
        try {
            mode = net.tigereye.chestcavity.soul.fakeplayer.brain.BrainMode.valueOf(modeToken.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            context.getSource().sendFailure(Component.literal("[soul] 无效模式: " + modeToken + " (可用: AUTO|COMBAT|SURVIVAL|IDLE)"));
            return 0;
        }
        net.tigereye.chestcavity.soul.fakeplayer.brain.BrainController.get().setMode(soul.getUUID(), mode);
        // 持久化到容器
        net.tigereye.chestcavity.registration.CCAttachments.getSoulContainer(executor).setBrainMode(executor, soul.getUUID(), mode, "command-brain-mode");
        context.getSource().sendSuccess(() -> Component.literal("[soul] 已设置 brain 模式为 " + mode + "（已持久化）。"), true);
        return 1;
    }

    /**
     * 注入临时战斗意图（CombatStyle），供 AI 在给定的 TTL 内优先执行相应策略。
     */
    private static int brainIntentCombat(CommandContext<CommandSourceStack> context, int ttl) throws CommandSyntaxException {
        ServerPlayer executor = context.getSource().getPlayerOrException();
        if (!net.tigereye.chestcavity.soul.engine.SoulFeatureToggle.isEnabled()) {
            context.getSource().sendFailure(Component.literal("[soul] 请先执行 /soul enable 后再设置意图。"));
            return 0;
        }
        String token = unquote(StringArgumentType.getString(context, "idOrName"));
        var resolved = SoulFakePlayerSpawner.resolveSoulUuidFlexible(executor, token);
        if (resolved.isEmpty()) { context.getSource().sendFailure(Component.literal("[soul] 未找到该 SoulPlayer。")); return 0; }
        var soulOpt = SoulFakePlayerSpawner.findSoulPlayer(resolved.get());
        if (soulOpt.isEmpty()) { context.getSource().sendFailure(Component.literal("[soul] 该 SoulPlayer 当前不在线。")); return 0; }
        var soul = soulOpt.get();
        if (!executor.getUUID().equals(soul.getOwnerId().orElse(null))) {
            context.getSource().sendFailure(Component.literal("[soul] 你无权控制该 SoulPlayer。"));
            return 0;
        }
        String styleToken = unquote(StringArgumentType.getString(context, "style"));
        net.tigereye.chestcavity.soul.fakeplayer.brain.intent.CombatStyle style;
        switch (styleToken.toLowerCase(java.util.Locale.ROOT)) {
            case "guard":
            case "g":
                style = net.tigereye.chestcavity.soul.fakeplayer.brain.intent.CombatStyle.GUARD; break;
            case "force_fight":
            case "forcefight":
            case "ff":
            default:
                style = net.tigereye.chestcavity.soul.fakeplayer.brain.intent.CombatStyle.FORCE_FIGHT; break;
        }
        var intent = new net.tigereye.chestcavity.soul.fakeplayer.brain.intent.CombatIntent(style, null, Math.max(1, ttl));
        // 同步运行时 + 持久化
        net.tigereye.chestcavity.soul.fakeplayer.brain.BrainController.get().pushIntent(soul.getUUID(), intent);
        net.tigereye.chestcavity.registration.CCAttachments.getSoulContainer(executor).setBrainIntent(executor, soul.getUUID(), intent, "command-brain-intent-combat");
        context.getSource().sendSuccess(() -> Component.literal("[soul] 已设置 Combat 意图: " + style + "，持续 " + Math.max(1, ttl) + "t（已持久化）。"), true);
        return 1;
    }

    /**
     * 清空灵魂的 AI 意图堆栈，恢复到默认模式。
     */
    private static int brainClear(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer executor = context.getSource().getPlayerOrException();
        String token = unquote(StringArgumentType.getString(context, "idOrName"));
        var resolved = SoulFakePlayerSpawner.resolveSoulUuidFlexible(executor, token);
        if (resolved.isEmpty()) { context.getSource().sendFailure(Component.literal("[soul] 未找到该 SoulPlayer。")); return 0; }
        var soulOpt = SoulFakePlayerSpawner.findSoulPlayer(resolved.get());
        if (soulOpt.isEmpty()) { context.getSource().sendFailure(Component.literal("[soul] 该 SoulPlayer 当前不在线。")); return 0; }
        var soul = soulOpt.get();
        if (!executor.getUUID().equals(soul.getOwnerId().orElse(null))) {
            context.getSource().sendFailure(Component.literal("[soul] 你无权控制该 SoulPlayer。"));
            return 0;
        }
        net.tigereye.chestcavity.soul.fakeplayer.brain.BrainController.get().clearIntents(soul.getUUID());
        net.tigereye.chestcavity.registration.CCAttachments.getSoulContainer(executor).clearBrainIntent(executor, soul.getUUID(), "command-brain-intent-clear");
        context.getSource().sendSuccess(() -> Component.literal("[soul] 已清除该 Soul 的主动意图（已持久化）。"), true);
        return 1;
    }

    /**
     * 取消特定动作，常用于调试动作状态机卡死的问题。
     */
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

    /**
     * 强制停止所有正在运行的动作，用于紧急恢复灵魂的可控性。
     */
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

    /**
     * 列出当前所有动作及其阶段，便于观察状态机推进情况。
     */
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

    /**
     * 去除命令补全产生的引号，便于兼容 {@code idOrName} 中的空格。
     */
    private static String unquote(String s) {
        if (s == null) return null;
        String t = s.trim();
        if (t.length() >= 2 && t.startsWith("\"") && t.endsWith("\"")) {
            return t.substring(1, t.length() - 1);
        }
        return t;
    }

    /**
     * 设置灵魂为 FOLLOW 模式，支持单个或全部灵魂批量下达。
     */
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

    /**
     * 设置灵魂为 IDLE 模式，自动清空导航目标。
     */
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

    /**
     * 指示灵魂守卫当前站位，维持战斗警戒。
     */
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

    /**
     * 强制灵魂进入激进战斗模式，寻找并攻击附近敌人。
     */
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

    /**
     * 临时修改灵魂的昵称，仅存储于容器并等待下次应用。
     */
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

    /**
     * 立即对在线的 SoulPlayer 应用已保存的昵称设置。
     */
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

    /**
     * 记录灵魂应当使用的皮肤用户名；实际拉取会在 apply 阶段完成。
     */
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

    /**
     * 立即从 Mojang API 拉取皮肤并广播给在线的 SoulPlayer。
     */
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

    /**
     * 设置灵魂在宿主登录时是否自动生成实体。
     */
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

    /**
     * 显式启用灵魂系统，记录审计日志并返回提示。
     */
    private static int enableSoulSystem(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer executor = context.getSource().getPlayerOrException();
        SoulFeatureToggle.enable(executor);
        var container = net.tigereye.chestcavity.registration.CCAttachments.getSoulContainer(executor);
        container.setActiveProfile(executor.getUUID());
        container.getOrCreateProfile(executor.getUUID()).updateFrom(executor);
        return 1;
    }

    /**
     * 生成一个临时 FakePlayer（仅限测试），不带任何持久化。
     */
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
    /**
     * 以宿主当前位置为模板生成新的灵魂实体并立即部署。
     */
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
    /**
     * 在指定坐标生成新的灵魂实体，用于定位测试或剧情脚本。
     */
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

    /**
     * 输出当前服务器上所有活跃的 SoulPlayer 列表。
     */
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

    /**
     * 在玩家与其灵魂之间切换操控权，用于体验“夺舍”流程。
     */
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

    /**
     * 强制宿主转入指定灵魂，常用于验证多灵魂切换。 
     */
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

    /**
     * 移除指定灵魂实体并写回快照，主要用于调试回收逻辑。
     */
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

    /**
     * 强制将所有活跃灵魂的快照持久化到容器，用于宕机前的手动保险。
     */
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
 
