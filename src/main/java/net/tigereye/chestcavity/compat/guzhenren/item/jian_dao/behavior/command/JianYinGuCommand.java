package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.behavior.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Arrays;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.behavior.organ.JianYinGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.command.CommandTactic;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.command.SwordCommandCenter;
import net.tigereye.chestcavity.interfaces.ChestCavityEntity;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.FlyingSwordEntity;

/**
 * 剑引蛊专用命令：负责聊天 TUI 的回调。
 */
@EventBusSubscriber(modid = ChestCavity.MODID)
public final class JianYinGuCommand {

  private static final SimpleCommandExceptionType UNKNOWN_TACTIC =
      new SimpleCommandExceptionType(
          Component.translatable("message.guzhenren.jianyingu.command.unknown_tactic"));

  private JianYinGuCommand() {}

  @SubscribeEvent
  public static void onRegisterCommands(RegisterCommandsEvent event) {
    CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
    dispatcher.register(
        Commands.literal("jianyin")
            .requires(src -> src.getEntity() instanceof ServerPlayer)
            .then(
                Commands.literal("command")
                    .then(Commands.literal("open").executes(JianYinGuCommand::open))
                    .then(
                        Commands.literal("tactic")
                            .then(
                                Commands.argument("id", StringArgumentType.word())
                                    .suggests(
                                        (ctx, builder) -> {
                                          Arrays.stream(CommandTactic.values())
                                              .forEach(t -> builder.suggest(t.id()));
                                          return builder.buildFuture();
                                        })
                                    .executes(JianYinGuCommand::setTactic)))
                    .then(
                        Commands.literal("group")
                            .then(
                                Commands.argument(
                                        "group",
                                        // 支持特殊的青莲集群组ID（SWARM_GROUP_ID=900）
                                        IntegerArgumentType.integer(0, FlyingSwordEntity.SWARM_GROUP_ID))
                                    .executes(JianYinGuCommand::setGroup)))
                    .then(Commands.literal("execute").executes(JianYinGuCommand::execute))
                    .then(Commands.literal("cancel").executes(JianYinGuCommand::cancel))
                    .then(Commands.literal("clear").executes(JianYinGuCommand::clear))));
  }

  private static int open(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
    ServerPlayer player = ctx.getSource().getPlayerOrException();
    if (!ensureOrgan(player)) {
      return 0;
    }
    SwordCommandCenter.openTui(player);
    return 1;
  }

  private static int setTactic(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
    ServerPlayer player = ctx.getSource().getPlayerOrException();
    if (!ensureOrgan(player)) {
      return 0;
    }
    String id = StringArgumentType.getString(ctx, "id");
    CommandTactic tactic =
        CommandTactic.byId(id).orElseThrow(() -> UNKNOWN_TACTIC.create());
    SwordCommandCenter.setTactic(player, tactic);
    player.sendSystemMessage(
        Component.translatable(
            "message.guzhenren.jianyingu.command.tactic_set", tactic.displayName()));
    SwordCommandCenter.openTui(player);
    return 1;
  }

  private static int execute(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
    ServerPlayer player = ctx.getSource().getPlayerOrException();
    if (!ensureOrgan(player)) {
      return 0;
    }
    long now = player.level().getGameTime();
    boolean success = SwordCommandCenter.execute(player, now);
    if (success) {
      SwordCommandCenter.currentTactic(player)
          .ifPresent(
              tactic ->
                  player.sendSystemMessage(
                      Component.translatable(
                          "message.guzhenren.jianyingu.command.executed", tactic.displayName())));
      SwordCommandCenter.openTui(player);
      return 1;
    }
    player.sendSystemMessage(
        Component.translatable("message.guzhenren.jianyingu.command.no_targets"));
    return 0;
  }

  private static int cancel(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
    ServerPlayer player = ctx.getSource().getPlayerOrException();
    if (!ensureOrgan(player)) {
      return 0;
    }
    SwordCommandCenter.cancelSelection(player);
    player.sendSystemMessage(
        Component.translatable("message.guzhenren.jianyingu.command.cancelled"));
    SwordCommandCenter.openTui(player);
    return 1;
  }

  private static int setGroup(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
    ServerPlayer player = ctx.getSource().getPlayerOrException();
    if (!ensureOrgan(player)) {
      return 0;
    }
    int group = IntegerArgumentType.getInteger(ctx, "group");
    SwordCommandCenter.setCommandGroup(player, group);
    player.sendSystemMessage(
        Component.translatable(
            "message.guzhenren.jianyingu.command.group_set", Math.max(0, group)));
    SwordCommandCenter.openTui(player);
    return 1;
  }

  private static int clear(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
    ServerPlayer player = ctx.getSource().getPlayerOrException();
    if (!ensureOrgan(player)) {
      return 0;
    }
    SwordCommandCenter.clearMarks(player);
    player.sendSystemMessage(
        Component.translatable("message.guzhenren.jianyingu.command.cleared"));
    SwordCommandCenter.openTui(player);
    return 1;
  }

  private static boolean ensureOrgan(ServerPlayer player) {
    ChestCavityInstance cc =
        ChestCavityEntity.of(player).map(ChestCavityEntity::getChestCavityInstance).orElse(null);
    if (cc == null) {
      return false;
    }
    boolean present = JianYinGuOrganBehavior.hasOrgan(cc);
    if (!present) {
      player.sendSystemMessage(
          Component.translatable("message.guzhenren.jianyingu.command.need_organ"));
    }
    return present;
  }
}
