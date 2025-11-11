package kizuna.guzhenren_event_ext.common.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import kizuna.guzhenren_event_ext.GuzhenrenEventExtension;
import kizuna.guzhenren_event_ext.common.system.loader.EventLoader;
import kizuna.guzhenren_event_ext.common.system.def.EventDefinition;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.concurrent.CompletableFuture;

/**
 * 古真人事件扩展调试命令
 * <p>
 * 命令：
 * - /guzhenren_ext_event trigger <event_id> - 手动触发指定事件
 * - /guzhenren_ext_event list - 列出所有已加载的事件
 * - /guzhenren_ext_event reload - 重新加载事件配置
 */
@EventBusSubscriber(modid = GuzhenrenEventExtension.MODID)
public final class GuzhenrenEventExtCommand {

    private GuzhenrenEventExtCommand() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(
            Commands.literal("guzhenren_ext_event")
                .requires(source -> source.hasPermission(2)) // OP 权限
                .then(
                    Commands.literal("trigger")
                        .then(
                            Commands.argument("event_id", StringArgumentType.string())
                                .suggests(GuzhenrenEventExtCommand::suggestEventIds)
                                .executes(GuzhenrenEventExtCommand::triggerEvent)
                        )
                )
                .then(
                    Commands.literal("list")
                        .executes(GuzhenrenEventExtCommand::listEvents)
                )
                .then(
                    Commands.literal("reload")
                        .executes(GuzhenrenEventExtCommand::reloadEvents)
                )
        );
    }

    /**
     * 触发指定事件
     */
    private static int triggerEvent(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("§c只有玩家可以使用此命令"));
            return 0;
        }

        String eventId = StringArgumentType.getString(ctx, "event_id");

        // 查找事件定义
        EventDefinition targetEvent = null;
        for (EventDefinition def : EventLoader.getInstance().getLoadedEvents()) {
            if (def.id.equals(eventId)) {
                targetEvent = def;
                break;
            }
        }

        if (targetEvent == null) {
            source.sendFailure(Component.literal("§c未找到事件: §e" + eventId));
            return 0;
        }

        if (!targetEvent.enabled) {
            source.sendFailure(Component.literal("§c事件已禁用: §e" + eventId));
            return 0;
        }

        // 手动执行事件的 Actions（跳过 Trigger 和 Condition 检查）
        GuzhenrenEventExtension.LOGGER.info("手动触发事件: {} (玩家: {})", eventId, player.getName().getString());

        if (targetEvent.actions != null && !targetEvent.actions.isEmpty()) {
            // 导入必要的类
            var actionRegistry = kizuna.guzhenren_event_ext.common.system.registry.ActionRegistry.getInstance();

            int actionCount = 0;

            for (com.google.gson.JsonObject actionDef : targetEvent.actions) {
                com.google.gson.JsonElement actionTypeElement = actionDef.get("type");
                if (actionTypeElement == null || !actionTypeElement.isJsonPrimitive()) {
                    continue;
                }
                String actionType = actionTypeElement.getAsString();

                var action = actionRegistry.get(actionType);
                if (action == null) {
                    GuzhenrenEventExtension.LOGGER.warn("未知的 action 类型: {}", actionType);
                    continue;
                }

                try {
                    action.execute(player, actionDef);
                    actionCount++;
                } catch (Exception e) {
                    GuzhenrenEventExtension.LOGGER.error("执行 action 失败: {}", actionType, e);
                }
            }

            final String finalEventId = eventId;
            final int finalActionCount = actionCount;
            source.sendSuccess(
                () -> Component.literal("§a已触发事件: §e" + finalEventId + " §7(" + finalActionCount + " 个动作)"),
                true
            );
            return 1;
        } else {
            source.sendFailure(Component.literal("§c事件没有任何 action: §e" + eventId));
            return 0;
        }
    }

    /**
     * 列出所有已加载的事件
     */
    private static int listEvents(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();

        var loadedEvents = EventLoader.getInstance().getLoadedEvents();

        if (loadedEvents.isEmpty()) {
            source.sendSuccess(() -> Component.literal("§7没有已加载的事件"), false);
            return 0;
        }

        source.sendSuccess(
            () -> Component.literal("§6=== 已加载的事件 (" + loadedEvents.size() + " 个) ==="),
            false
        );

        for (EventDefinition def : loadedEvents) {
            String status = def.enabled ? "§a启用" : "§c禁用";
            String triggerOnce = def.triggerOnce ? " §7[仅一次]" : "";

            source.sendSuccess(
                () -> Component.literal("  §e" + def.id + " §7- " + status + triggerOnce),
                false
            );
        }

        return loadedEvents.size();
    }

    /**
     * 重新加载事件配置
     */
    private static int reloadEvents(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();

        source.sendSuccess(
            () -> Component.literal("§e请使用 §6/reload §e命令重新加载数据包以刷新事件配置"),
            false
        );

        source.sendSuccess(
            () -> Component.literal("§7或者重启服务器以确保所有事件正确加载"),
            false
        );

        return 1;
    }

    /**
     * 为 event_id 参数提供自动补全建议
     */
    private static CompletableFuture<Suggestions> suggestEventIds(
        CommandContext<CommandSourceStack> ctx,
        SuggestionsBuilder builder
    ) {
        var loadedEvents = EventLoader.getInstance().getLoadedEvents();

        for (EventDefinition def : loadedEvents) {
            if (def.enabled) { // 只建议启用的事件
                builder.suggest(def.id);
            }
        }

        return builder.buildFuture();
    }
}
