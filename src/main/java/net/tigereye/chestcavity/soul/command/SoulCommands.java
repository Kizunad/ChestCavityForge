package net.tigereye.chestcavity.soul.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.commands.arguments.UuidArgument;
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
                .then(Commands.literal("order")
                        .then(Commands.literal("follow")
                                .then(Commands.argument("uuid", UuidArgument.uuid())
                                        .suggests((ctx, builder) -> SoulFakePlayerSpawner.suggestSoulPlayerUuids(ctx.getSource(), builder))
                                        .executes(SoulCommands::orderFollow)))
                        .then(Commands.literal("guard")
                                .then(Commands.argument("uuid", UuidArgument.uuid())
                                        .suggests((ctx, builder) -> SoulFakePlayerSpawner.suggestSoulPlayerUuids(ctx.getSource(), builder))
                                        .executes(SoulCommands::orderGuard)))
                        .then(Commands.literal("idle")
                                .then(Commands.argument("uuid", UuidArgument.uuid())
                                        .suggests((ctx, builder) -> SoulFakePlayerSpawner.suggestSoulPlayerUuids(ctx.getSource(), builder))
                                        .executes(SoulCommands::orderIdle))))
                .then(Commands.literal("test")
                        .then(Commands.literal("SoulPlayerList")
                                .executes(SoulCommands::listSoulPlayers))
                        .then(Commands.literal("SoulPlayerSwitch")
                                .then(Commands.literal("owner")
                                        .executes(SoulCommands::switchOwner))
                                .then(Commands.argument("uuid", UuidArgument.uuid())
                                        .suggests((ctx, builder) -> SoulFakePlayerSpawner.suggestSoulPlayerUuids(ctx.getSource(), builder))
                                        .executes(SoulCommands::switchSoulPlayer)))
                        .then(Commands.literal("SoulPlayerRemove")
                                .then(Commands.argument("uuid", UuidArgument.uuid())
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

    private static int orderFollow(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer executor = context.getSource().getPlayerOrException();
        if (!net.tigereye.chestcavity.soul.engine.SoulFeatureToggle.isEnabled()) {
            context.getSource().sendFailure(Component.literal("[soul] 请先执行 /soul enable 后再下达订单。"));
            return 0;
        }
        UUID raw = UuidArgument.getUuid(context, "uuid");
        UUID uuid = SoulFakePlayerSpawner.resolveSoulUuid(raw).orElse(raw);
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
        UUID raw = UuidArgument.getUuid(context, "uuid");
        UUID uuid = SoulFakePlayerSpawner.resolveSoulUuid(raw).orElse(raw);
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
        UUID raw = UuidArgument.getUuid(context, "uuid");
        UUID uuid = net.tigereye.chestcavity.soul.fakeplayer.SoulFakePlayerSpawner.resolveSoulUuid(raw).orElse(raw);
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
            source.sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
                    "[soul] spawnFakePlayer -> soul=%s",
                    result.soulPlayer().getUUID())), true);
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
            InventorySnapshot inv = new InventorySnapshot(net.minecraft.core.NonNullList.withSize(41, net.minecraft.world.item.ItemStack.EMPTY));
            PlayerStatsSnapshot stats = PlayerStatsSnapshot.empty();
            PlayerEffectsSnapshot fx = PlayerEffectsSnapshot.empty();
            PlayerPositionSnapshot pos = PlayerPositionSnapshot.capture(executor);
            SoulProfile profile = SoulProfile.fromSnapshot(soulId, inv, stats, fx, pos);
            container.putProfile(soulId, profile);
            SoulProfileOps.markContainerDirty(executor, container, "command-createSoulDefault");
        }
        var spawned = SoulFakePlayerSpawner.respawnForOwner(executor, soulId);
        if (spawned.isPresent()) {
            SoulLog.info("[soul] command-createSoulDefault owner={} soul={}", executor.getUUID(), soulId);
            context.getSource().sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
                    "[soul] CreateSoulDefault -> soul=%s", soulId)), true);
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
            InventorySnapshot inv = new InventorySnapshot(net.minecraft.core.NonNullList.withSize(41, net.minecraft.world.item.ItemStack.EMPTY));
            PlayerStatsSnapshot stats = PlayerStatsSnapshot.empty();
            PlayerEffectsSnapshot fx = PlayerEffectsSnapshot.empty();
            PlayerPositionSnapshot pos = PlayerPositionSnapshot.of(executor.level().dimension(), x, y, z, yaw, pitch, yaw);
            SoulProfile profile = SoulProfile.fromSnapshot(soulId, inv, stats, fx, pos);
            container.putProfile(soulId, profile);
            SoulProfileOps.markContainerDirty(executor, container, "command-createSoulAt");
        }
        var spawned = SoulFakePlayerSpawner.respawnForOwner(executor, soulId);
        if (spawned.isPresent()) {
            SoulLog.info("[soul] command-createSoulAt owner={} soul={} pos=({},{},{})",
                    executor.getUUID(), soulId, x, y, z);
            context.getSource().sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
                    "[soul] CreateSoulAt -> soul=%s @ (%.1f, %.1f, %.1f)", soulId, x, y, z)), true);
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
        UUID uuid = UuidArgument.getUuid(context, "uuid");
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
        SoulLog.info("[soul] command-switch owner={} target={} ", executor.getUUID(), uuid);
        context.getSource().sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
                "[soul] 已切换至 SoulPlayer %s。", uuid)), true);
        return 1;
    }

    private static int removeSoulPlayer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer executor = context.getSource().getPlayerOrException();
        if (!SoulFeatureToggle.isEnabled()) {
            context.getSource().sendFailure(Component.literal("[soul] 请先执行 /soul enable 后再移除 SoulPlayer。"));
            return 0;
        }
        UUID uuid = UuidArgument.getUuid(context, "uuid");
        if (!SoulFakePlayerSpawner.remove(uuid, executor)) {
            context.getSource().sendFailure(Component.literal(String.format(Locale.ROOT,
                    "[soul] 未找到 UUID=%s 的 SoulPlayer 或你无权移除。", uuid)));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
                "[soul] 已移除 SoulPlayer %s。", uuid)), true);
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
 
