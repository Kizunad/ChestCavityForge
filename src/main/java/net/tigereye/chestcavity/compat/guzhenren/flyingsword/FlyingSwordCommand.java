package net.tigereye.chestcavity.compat.guzhenren.flyingsword;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.List;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.AIMode;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.FlyingSwordEntity;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.command.SwordCommandCenter;

/**
 * 飞剑命令系统
 *
 * <p>命令列表：
 * <ul>
 *   <li>/flyingsword spawn [count] - 召唤飞剑</li>
 *   <li>/flyingsword test spawn - 测试召唤（带调试信息）</li>
 *   <li>/flyingsword recall - 召回所有飞剑</li>
 *   <li>/flyingsword mode <orbit|guard|hunt> - 切换AI模式</li>
 *   <li>/flyingsword status - 查看状态</li>
 *   <li>/flyingsword list - 列出所有飞剑</li>
 *   <li>/flyingsword clear - 清除所有飞剑目标</li>
 * </ul>
 */
@EventBusSubscriber(modid = ChestCavity.MODID)
public final class FlyingSwordCommand {

  private FlyingSwordCommand() {}

  @SubscribeEvent
  public static void onRegisterCommands(RegisterCommandsEvent event) {
    CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
    dispatcher.register(
        Commands.literal("flyingsword")
            // /flyingsword spawn [count]
            .then(
                Commands.literal("spawn").requires(src -> src.hasPermission(2))
                    .executes(FlyingSwordCommand::spawnOne)
                    .then(
                        Commands.argument("count", IntegerArgumentType.integer(1, 10))
                            .executes(FlyingSwordCommand::spawnMultiple)))
            // /flyingsword test spawn
            .then(
                Commands.literal("test").requires(src -> src.hasPermission(2))
                    .then(Commands.literal("spawn").executes(FlyingSwordCommand::testSpawn)))
            // /flyingsword recall
            .then(Commands.literal("recall").executes(FlyingSwordCommand::recallAll))
            // /flyingsword restore
            .then(Commands.literal("restore").executes(FlyingSwordCommand::restoreAll))
            // /flyingsword restore_index <index>
            .then(
                Commands.literal("restore_index")
                    .then(
                        Commands.argument("index", IntegerArgumentType.integer(1, 999))
                            .executes(FlyingSwordCommand::restoreByIndex)))
            // /flyingsword mode <orbit|guard|hunt>
            .then(
                Commands.literal("mode")
                    .then(
                        Commands.argument("mode", StringArgumentType.word())
                            .suggests(
                                (ctx, builder) -> {
                                  builder.suggest("orbit");
                                  builder.suggest("guard");
                                  builder.suggest("hunt");
                                  builder.suggest("hover");
                                  return builder.buildFuture();
                                })
                            .executes(FlyingSwordCommand::setMode)))
            // /flyingsword status
            .then(Commands.literal("status").executes(FlyingSwordCommand::showStatus))
            // /flyingsword list
            .then(Commands.literal("list").executes(FlyingSwordCommand::listSwords))
            // /flyingsword recall_index <index>
            .then(
                Commands.literal("recall_index")
                    .then(
                        Commands.argument("index", IntegerArgumentType.integer(1, 999))
                            .executes(FlyingSwordCommand::recallByIndex)))
            // /flyingsword mode_index <index> <mode>
            .then(
                Commands.literal("mode_index")
                    .then(
                        Commands.argument("index", IntegerArgumentType.integer(1, 999))
                            .then(
                                Commands.argument("mode", StringArgumentType.word())
                                    .suggests(
                                        (ctx, builder) -> {
                                          builder.suggest("orbit");
                                          builder.suggest("guard");
                                          builder.suggest("hunt");
                                          builder.suggest("hover");
                                          return builder.buildFuture();
                                        })
                                    .executes(FlyingSwordCommand::setModeByIndex))))
            // /flyingsword clear
            .then(Commands.literal("clear").executes(FlyingSwordCommand::clearTargets))
            // /flyingsword ui / ui_active [page] / ui_storage [page]
            .then(Commands.literal("ui").executes(FlyingSwordCommand::openUI))
            .then(
                Commands.literal("ui_active")
                    .then(
                        Commands.argument("page", IntegerArgumentType.integer(1, 999))
                            .executes(FlyingSwordCommand::openUIActive)))
            .then(
                Commands.literal("ui_storage")
                    .then(
                        Commands.argument("page", IntegerArgumentType.integer(1, 999))
                            .executes(FlyingSwordCommand::openUIStorage)))
            // /flyingsword withdraw_index <index>
            .then(
                Commands.literal("withdraw_index")
                    .then(
                        Commands.argument("index", IntegerArgumentType.integer(1, 999))
                            .executes(FlyingSwordCommand::withdrawByIndex)))
            // /flyingsword deposit_index <index>
            .then(
                Commands.literal("deposit_index")
                    .then(
                        Commands.argument("index", IntegerArgumentType.integer(1, 999))
                            .executes(FlyingSwordCommand::depositByIndex)))
            // /flyingsword select <nearest|index>
            .then(
                Commands.literal("select")
                    .then(Commands.literal("nearest").executes(FlyingSwordCommand::selectNearest))
                    .then(
                        Commands.literal("index")
                            .then(
                                Commands.argument(
                                        "index", IntegerArgumentType.integer(1, 999))
                                    .executes(FlyingSwordCommand::selectByIndex)))
                    .then(Commands.literal("clear").executes(FlyingSwordCommand::clearSelection))
                    .then(Commands.literal("show").executes(FlyingSwordCommand::showSelection)))
            // /flyingsword recall_selected
            .then(
                Commands.literal("recall_selected")
                    .executes(FlyingSwordCommand::recallSelected))
            // /flyingsword mode_selected <orbit|guard|hunt|hover>
            .then(
                Commands.literal("mode_selected")
                    .then(
                        Commands.argument("mode", StringArgumentType.word())
                            .suggests(
                                (ctx, builder) -> {
                                  builder.suggest("orbit");
                                  builder.suggest("guard");
                                  builder.suggest("hunt");
                                  builder.suggest("hover");
                                  return builder.buildFuture();
                                })
                            .executes(FlyingSwordCommand::setModeSelected)))
            .then(
                Commands.literal("group_selected")
                    .then(
                        Commands.argument(
                                "group",
                                // 接受普通组(0..3)以及青莲集群特殊组ID
                                IntegerArgumentType.integer(0, FlyingSwordEntity.SWARM_GROUP_ID))
                            .executes(FlyingSwordCommand::setGroupSelected)))
            .then(
                Commands.literal("group_index")
                    .then(
                        Commands.argument("index", IntegerArgumentType.integer(1, 999))
                            .then(
                                Commands.argument(
                                        "group",
                                        IntegerArgumentType.integer(0, FlyingSwordEntity.SWARM_GROUP_ID))
                                    .executes(FlyingSwordCommand::setGroupByIndex))))
            .then(
                Commands.literal("group_all")
                    .then(
                        Commands.argument(
                                "group",
                                IntegerArgumentType.integer(0, FlyingSwordEntity.SWARM_GROUP_ID))
                            .executes(FlyingSwordCommand::setGroupAll)))
            // /flyingsword repair_selected
            .then(Commands.literal("repair_selected").executes(FlyingSwordCommand::repairSelected))
            // /flyingsword repair_index <index>
            .then(
                Commands.literal("repair_index")
                    .then(
                        Commands.argument("index", IntegerArgumentType.integer(1, 999))
                            .executes(FlyingSwordCommand::repairByIndex)))
            // /flyingsword storage
            .then(Commands.literal("storage").executes(FlyingSwordCommand::checkStorage))
            // /flyingsword debug
            .then(Commands.literal("debug").executes(FlyingSwordCommand::debugInfo)));
  }

  // ========== 命令实现 ==========

  private static int spawnOne(CommandContext<CommandSourceStack> ctx)
      throws CommandSyntaxException {
    return spawnSwords(ctx, 1, false);
  }

  private static int spawnMultiple(CommandContext<CommandSourceStack> ctx)
      throws CommandSyntaxException {
    int count = IntegerArgumentType.getInteger(ctx, "count");
    return spawnSwords(ctx, count, false);
  }

  private static int testSpawn(CommandContext<CommandSourceStack> ctx)
      throws CommandSyntaxException {
    return spawnSwords(ctx, 1, true);
  }

  private static int spawnSwords(CommandContext<CommandSourceStack> ctx, int count, boolean debug)
      throws CommandSyntaxException {
    ServerPlayer player = ctx.getSource().getPlayerOrException();
    ServerLevel level = player.serverLevel();

    // 使用手持物品作为源（如果是剑）
    ItemStack heldItem = player.getMainHandItem();
    ItemStack sourceStack = null;
    if (heldItem.getItem() instanceof net.minecraft.world.item.SwordItem) {
      sourceStack = heldItem;
    } else {
      // 默认使用铁剑
      sourceStack = new ItemStack(Items.IRON_SWORD);
    }

    int successCount = 0;
    StringBuilder debugInfo = new StringBuilder();

    for (int i = 0; i < count; i++) {
      FlyingSwordEntity sword =
          FlyingSwordSpawner.spawnFromOwner(level, player, sourceStack);

      if (sword != null) {
        successCount++;

        if (debug) {
          debugInfo
              .append(String.format("\n[Sword #%d]", i + 1))
              .append(String.format("\n  Position: %.2f, %.2f, %.2f",
                  sword.getX(), sword.getY(), sword.getZ()))
              .append(String.format("\n  Attributes:"))
              .append(String.format("\n    Damage: %.2f", sword.getSwordAttributes().damageBase))
              .append(String.format("\n    Speed: %.2f/%.2f",
                  sword.getSwordAttributes().speedBase, sword.getSwordAttributes().speedMax))
              .append(String.format("\n    Durability: %.1f/%.1f",
                  sword.getDurability(), sword.getSwordAttributes().maxDurability))
              .append(String.format("\n    Tool Tier: %d", sword.getSwordAttributes().toolTier))
              .append(String.format("\n  State:"))
              .append(String.format("\n    Level: %d", sword.getSwordLevel()))
              .append(String.format("\n    Experience: %d", sword.getExperience()))
              .append(String.format("\n    AI Mode: %s", sword.getAIMode().getDisplayName()));
        }
      }
    }

    final int finalSuccess = successCount;
    final String sourceDesc = sourceStack.getHoverName().getString();
    final String debugStr = debugInfo.toString();

    if (debug) {
      ctx.getSource()
          .sendSuccess(
              () ->
                  Component.literal(
                      String.format(
                          Locale.ROOT,
                          "[flyingsword] Test spawn completed: %d/%d success\nSource: %s%s",
                          finalSuccess,
                          count,
                          sourceDesc,
                          debugStr)),
              false);
    } else {
      ctx.getSource()
          .sendSuccess(
              () ->
                  Component.literal(
                      String.format(
                          Locale.ROOT,
                          "[flyingsword] Spawned %d flying sword(s) from %s",
                          finalSuccess,
                          sourceDesc)),
              true);
    }

    return finalSuccess;
  }

  private static int recallAll(CommandContext<CommandSourceStack> ctx)
      throws CommandSyntaxException {
    ServerPlayer player = ctx.getSource().getPlayerOrException();
    ServerLevel level = player.serverLevel();

    int count = FlyingSwordController.recallAll(level, player);

    final int finalCount = count;
    ctx.getSource()
        .sendSuccess(
            () ->
                Component.literal(
                    String.format(Locale.ROOT, "[flyingsword] Recalled %d sword(s)", finalCount)),
            true);

    return count;
  }

  private static int restoreAll(CommandContext<CommandSourceStack> ctx)
      throws CommandSyntaxException {
    ServerPlayer player = ctx.getSource().getPlayerOrException();
    ServerLevel level = player.serverLevel();

    int count = FlyingSwordSpawner.restoreAll(level, player);

    final int finalCount = count;
    ctx.getSource()
        .sendSuccess(
            () ->
                Component.literal(
                    String.format(Locale.ROOT, "[flyingsword] Restored %d sword(s)", finalCount)),
            true);

    return count;
  }

  private static int restoreByIndex(CommandContext<CommandSourceStack> ctx)
      throws CommandSyntaxException {
    ServerPlayer player = ctx.getSource().getPlayerOrException();
    ServerLevel level = player.serverLevel();
    int index = IntegerArgumentType.getInteger(ctx, "index");
    boolean ok =
        net.tigereye.chestcavity.compat.guzhenren.flyingsword
            .FlyingSwordSpawner.restoreOne(level, player, index);
    if (ok) {
      ctx.getSource().sendSuccess(() -> Component.literal("[flyingsword] Restored one"), true);
      return 1;
    }
    ctx.getSource().sendFailure(Component.literal("[flyingsword] Failed to restore"));
    return 0;
  }

  private static int setMode(CommandContext<CommandSourceStack> ctx)
      throws CommandSyntaxException {
    ServerPlayer player = ctx.getSource().getPlayerOrException();
    ServerLevel level = player.serverLevel();

    String modeStr = StringArgumentType.getString(ctx, "mode").toLowerCase(Locale.ROOT);
    AIMode mode;
    try {
      mode = AIMode.fromId(modeStr);
    } catch (Exception e) {
      ctx.getSource()
          .sendFailure(
              Component.literal(
                  String.format(
                      "[flyingsword] Invalid mode: %s (valid: orbit, guard, hunt, hover)", modeStr)));
      return 0;
    }

    int count = FlyingSwordController.setAllAIMode(level, player, mode);

    final int finalCount = count;
    final String modeName = mode.getDisplayName();
    ctx.getSource()
        .sendSuccess(
            () ->
                Component.literal(
                    String.format(
                        Locale.ROOT,
                        "[flyingsword] Set %d sword(s) to mode: %s",
                        finalCount,
                        modeName)),
            true);

    SwordCommandCenter.clear(player);
    return count;
  }

  private static int showStatus(CommandContext<CommandSourceStack> ctx)
      throws CommandSyntaxException {
    ServerPlayer player = ctx.getSource().getPlayerOrException();
    ServerLevel level = player.serverLevel();

    List<FlyingSwordEntity> swords = FlyingSwordController.getPlayerSwords(level, player);

    if (swords.isEmpty()) {
      ctx.getSource()
          .sendSuccess(() -> Component.literal("[flyingsword] No flying swords"), false);
      return 0;
    }

    StringBuilder status = new StringBuilder();
    status.append(
        String.format(Locale.ROOT, "[flyingsword] Total: %d sword(s)\n", swords.size()));

    for (int i = 0; i < swords.size(); i++) {
      FlyingSwordEntity sword = swords.get(i);
      status.append(String.format("\n[#%d]", i + 1));
      status.append(String.format(" Lv.%d", sword.getSwordLevel()));
      status.append(String.format(" [%s]", sword.getAIMode().getDisplayName()));
      status.append(
          String.format(
              " Dur:%.0f/%.0f",
              sword.getDurability(), sword.getSwordAttributes().maxDurability));
      status.append(
          String.format(
              " Exp:%d/%d",
              sword.getExperience(),
              net.tigereye.chestcavity.compat.guzhenren.flyingsword.calculator
                  .FlyingSwordCalculator.calculateExpToNext(sword.getSwordLevel())));
      status.append(String.format(" Speed:%.2f", sword.getCurrentSpeed()));

      // 距离信息
      double dist = sword.distanceTo(player);
      status.append(String.format(" Dist:%.1fm", dist));
    }

    final String statusStr = status.toString();
    ctx.getSource().sendSuccess(() -> Component.literal(statusStr), false);

    return swords.size();
  }

  private static int listSwords(CommandContext<CommandSourceStack> ctx)
      throws CommandSyntaxException {
    ServerPlayer player = ctx.getSource().getPlayerOrException();
    ServerLevel level = player.serverLevel();

    List<FlyingSwordEntity> swords = FlyingSwordController.getPlayerSwords(level, player);

    if (swords.isEmpty()) {
      ctx.getSource()
          .sendSuccess(() -> Component.literal("[flyingsword] No flying swords"), false);
      return 0;
    }

    StringBuilder list = new StringBuilder();
    list.append(String.format(Locale.ROOT, "[flyingsword] Flying Swords (%d):\n", swords.size()));

    for (int i = 0; i < swords.size(); i++) {
      FlyingSwordEntity sword = swords.get(i);
      list.append(String.format("\n  #%d: ", i + 1));
      list.append(String.format("Level %d", sword.getSwordLevel()));
      list.append(String.format(" | Mode: %s", sword.getAIMode().getDisplayName()));
      list.append(
          String.format(
              " | Position: (%.1f, %.1f, %.1f)",
              sword.getX(), sword.getY(), sword.getZ()));
      list.append(String.format(" | Distance: %.1fm", sword.distanceTo(player)));

      if (sword.getTargetEntity() != null) {
        list.append(
            String.format(" | Target: %s", sword.getTargetEntity().getName().getString()));
      }
    }

    final String listStr = list.toString();
    ctx.getSource().sendSuccess(() -> Component.literal(listStr), false);

    return swords.size();
  }

  private static int clearTargets(CommandContext<CommandSourceStack> ctx)
      throws CommandSyntaxException {
    ServerPlayer player = ctx.getSource().getPlayerOrException();
    ServerLevel level = player.serverLevel();

    int count = FlyingSwordController.clearAllTargets(level, player);

    final int finalCount = count;
    ctx.getSource()
        .sendSuccess(
            () ->
                Component.literal(
                    String.format(
                        Locale.ROOT, "[flyingsword] Cleared targets for %d sword(s)", finalCount)),
            true);

    return count;
  }

  private static int recallByIndex(CommandContext<CommandSourceStack> ctx)
      throws CommandSyntaxException {
    ServerPlayer player = ctx.getSource().getPlayerOrException();
    ServerLevel level = player.serverLevel();
    int index = IntegerArgumentType.getInteger(ctx, "index");
    boolean ok =
        net.tigereye.chestcavity.compat.guzhenren.flyingsword
            .FlyingSwordController.recallByIndex(level, player, index);
    if (ok) {
      ctx.getSource().sendSuccess(() -> Component.literal("[flyingsword] Recalled one"), true);
      return 1;
    }
    ctx.getSource().sendFailure(Component.literal("[flyingsword] Failed to recall"));
    return 0;
  }

  private static int setModeByIndex(CommandContext<CommandSourceStack> ctx)
      throws CommandSyntaxException {
    ServerPlayer player = ctx.getSource().getPlayerOrException();
    ServerLevel level = player.serverLevel();
    int index = IntegerArgumentType.getInteger(ctx, "index");
    String modeStr = StringArgumentType.getString(ctx, "mode");
    var mode =
        net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.AIMode
            .fromId(modeStr);
    boolean ok =
        net.tigereye.chestcavity.compat.guzhenren.flyingsword
            .FlyingSwordController.setModeByIndex(level, player, index, mode);
    if (ok) {
      ctx.getSource()
          .sendSuccess(
              () ->
                  Component.literal(
                      String.format(
                          java.util.Locale.ROOT,
                          "[flyingsword] Sword #%d set to mode: %s",
                          index, mode.getDisplayName())),
              true);
      SwordCommandCenter.clear(player);
      return 1;
    }
    ctx.getSource().sendFailure(Component.literal("[flyingsword] Failed to set mode"));
    return 0;
  }

  // ========== Selection commands ==========
  private static int selectNearest(CommandContext<CommandSourceStack> ctx)
      throws CommandSyntaxException {
    ServerPlayer player = ctx.getSource().getPlayerOrException();
    ServerLevel level = player.serverLevel();
    FlyingSwordEntity sword = FlyingSwordController.getNearestSword(level, player);
    if (sword == null) {
      ctx.getSource().sendFailure(Component.literal("[flyingsword] No sword found nearby"));
      return 0;
    }
    boolean ok = FlyingSwordController.setSelectedSword(player, sword);
    if (ok) {
      ctx.getSource()
          .sendSuccess(
              () ->
                  Component.literal(
                      String.format(
                          "[flyingsword] Selected nearest sword (Lv.%d, Dist: %.1fm)",
                          sword.getSwordLevel(), sword.distanceTo(player))),
              false);
      return 1;
    }
    ctx.getSource().sendFailure(Component.literal("[flyingsword] Failed to select sword"));
    return 0;
  }

  private static int selectByIndex(CommandContext<CommandSourceStack> ctx)
      throws CommandSyntaxException {
    ServerPlayer player = ctx.getSource().getPlayerOrException();
    ServerLevel level = player.serverLevel();
    int index = IntegerArgumentType.getInteger(ctx, "index");
    java.util.List<FlyingSwordEntity> swords = FlyingSwordController.getPlayerSwords(level, player);
    if (swords.isEmpty()) {
      ctx.getSource().sendFailure(Component.literal("[flyingsword] No flying swords"));
      return 0;
    }
    if (index < 1 || index > swords.size()) {
      ctx.getSource()
          .sendFailure(
              Component.literal(
                  String.format(
                      "[flyingsword] Invalid index %d (1..%d)", index, swords.size())));
      return 0;
    }
    FlyingSwordEntity sword = swords.get(index - 1);
    boolean ok = FlyingSwordController.setSelectedSword(player, sword);
    if (ok) {
      ctx.getSource()
          .sendSuccess(
              () ->
                  Component.literal(
                      String.format(
                          "[flyingsword] Selected sword #%d (Lv.%d, Dist: %.1fm)",
                          index, sword.getSwordLevel(), sword.distanceTo(player))),
              false);
      return 1;
    }
    ctx.getSource().sendFailure(Component.literal("[flyingsword] Failed to select sword"));
    return 0;
  }

  private static int clearSelection(CommandContext<CommandSourceStack> ctx)
      throws CommandSyntaxException {
    ServerPlayer player = ctx.getSource().getPlayerOrException();
    FlyingSwordController.clearSelectedSword(player);
    ctx.getSource().sendSuccess(
        () -> Component.literal("[flyingsword] Selection cleared"), false);
    return 1;
  }

  private static int showSelection(CommandContext<CommandSourceStack> ctx)
      throws CommandSyntaxException {
    ServerPlayer player = ctx.getSource().getPlayerOrException();
    ServerLevel level = player.serverLevel();
    FlyingSwordEntity sword = FlyingSwordController.getSelectedSword(level, player);
    if (sword == null) {
      ctx.getSource().sendSuccess(() -> Component.literal("[flyingsword] No selected sword"), false);
      return 0;
    }
    String msg =
        String.format(
            java.util.Locale.ROOT,
            "[flyingsword] Selected: Lv.%d | Mode:%s | Dur:%.0f/%.0f | Dist:%.1fm",
            sword.getSwordLevel(),
            sword.getAIMode().getDisplayName(),
            sword.getDurability(),
            sword.getSwordAttributes().maxDurability,
            sword.distanceTo(player));
    ctx.getSource().sendSuccess(() -> Component.literal(msg), false);
    return 1;
  }

  private static int recallSelected(CommandContext<CommandSourceStack> ctx)
      throws CommandSyntaxException {
    ServerPlayer player = ctx.getSource().getPlayerOrException();
    ServerLevel level = player.serverLevel();
    FlyingSwordEntity sword = FlyingSwordController.getSelectedSword(level, player);
    if (sword == null) {
      ctx.getSource().sendFailure(Component.literal("[flyingsword] No selected sword"));
      return 0;
    }
    FlyingSwordController.recall(sword);
    ctx.getSource().sendSuccess(() -> Component.literal("[flyingsword] Recalled selected sword"), true);
    return 1;
  }

  private static int setModeSelected(CommandContext<CommandSourceStack> ctx)
      throws CommandSyntaxException {
    ServerPlayer player = ctx.getSource().getPlayerOrException();
    ServerLevel level = player.serverLevel();
    FlyingSwordEntity sword = FlyingSwordController.getSelectedSword(level, player);
    if (sword == null) {
      ctx.getSource().sendFailure(Component.literal("[flyingsword] No selected sword"));
      return 0;
    }
    String modeStr = StringArgumentType.getString(ctx, "mode");
    net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.AIMode mode =
        net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.AIMode
            .fromId(modeStr);
    sword.setAIMode(mode);
    ctx.getSource()
        .sendSuccess(
            () ->
                Component.literal(
                    String.format("[flyingsword] Selected sword set to mode: %s", mode.getDisplayName())),
            true);
    SwordCommandCenter.clear(player);
    return 1;
  }

  private static int setGroupSelected(CommandContext<CommandSourceStack> ctx)
      throws CommandSyntaxException {
    ServerPlayer player = ctx.getSource().getPlayerOrException();
    ServerLevel level = player.serverLevel();
    FlyingSwordEntity sword = FlyingSwordController.getSelectedSword(level, player);
    if (sword == null) {
      ctx.getSource().sendFailure(Component.literal("[flyingsword] No selected sword"));
      return 0;
    }
    int group = IntegerArgumentType.getInteger(ctx, "group");
    FlyingSwordController.setGroup(sword, group);
    ctx.getSource()
        .sendSuccess(
            () ->
                Component.literal(
                    String.format(Locale.ROOT, "[flyingsword] Selected sword -> group %d", Math.max(0, group))),
            true);
    SwordCommandCenter.clear(player);
    return 1;
  }

  private static int setGroupByIndex(CommandContext<CommandSourceStack> ctx)
      throws CommandSyntaxException {
    ServerPlayer player = ctx.getSource().getPlayerOrException();
    ServerLevel level = player.serverLevel();
    int index = IntegerArgumentType.getInteger(ctx, "index");
    int group = IntegerArgumentType.getInteger(ctx, "group");
    boolean ok = FlyingSwordController.setGroupByIndex(level, player, index, group);
    if (!ok) {
      ctx.getSource().sendFailure(Component.literal("[flyingsword] Failed to set group"));
      return 0;
    }
    ctx.getSource()
        .sendSuccess(
            () ->
                Component.literal(
                    String.format(Locale.ROOT, "[flyingsword] Sword #%d -> group %d", index, Math.max(0, group))),
            true);
    SwordCommandCenter.clear(player);
    return 1;
  }

  private static int setGroupAll(CommandContext<CommandSourceStack> ctx)
      throws CommandSyntaxException {
    ServerPlayer player = ctx.getSource().getPlayerOrException();
    ServerLevel level = player.serverLevel();
    int group = IntegerArgumentType.getInteger(ctx, "group");
    int count = FlyingSwordController.setAllGroup(level, player, group);
    ctx.getSource()
        .sendSuccess(
            () ->
                Component.literal(
                    String.format(
                        Locale.ROOT, "[flyingsword] Set %d sword(s) -> group %d", count, Math.max(0, group))),
            true);
    SwordCommandCenter.clear(player);
    return count;
  }

  private static int checkStorage(CommandContext<CommandSourceStack> ctx)
      throws CommandSyntaxException {
    ServerPlayer player = ctx.getSource().getPlayerOrException();

    net.tigereye.chestcavity.compat.guzhenren.flyingsword.FlyingSwordStorage
        storage =
            net.tigereye.chestcavity.registration.CCAttachments.getFlyingSwordStorage(player);

    int count = storage.getCount();
    java.util.List<
            net.tigereye.chestcavity.compat.guzhenren.flyingsword
                .FlyingSwordStorage.RecalledSword>
        swords = storage.getRecalledSwords();

    StringBuilder message = new StringBuilder();
    message.append(
        String.format(Locale.ROOT, "[flyingsword] Storage: %d recalled sword(s)\n", count));

    for (int i = 0; i < swords.size(); i++) {
      var sword = swords.get(i);
      message.append(String.format("\n  #%d: ", i + 1));
      message.append(String.format("Level %d", sword.level));
      message.append(String.format(" | Exp: %d", sword.experience));
      message.append(String.format(" | Durability: %.1f/%.1f", sword.durability, sword.attributes.maxDurability));
    }

    final String msg = message.toString();
    ctx.getSource().sendSuccess(() -> Component.literal(msg), false);

    return count;
  }

  private static int openUI(CommandContext<CommandSourceStack> ctx)
      throws CommandSyntaxException {
    ServerPlayer player = ctx.getSource().getPlayerOrException();
    net.tigereye.chestcavity.compat.guzhenren.flyingsword.ui
            .FlyingSwordTUI
        .openMain(player);
    return 1;
  }

  // ===== 修复/赋能 =====
  private static int repairSelected(CommandContext<CommandSourceStack> ctx)
      throws CommandSyntaxException {
    ServerPlayer player = ctx.getSource().getPlayerOrException();
    boolean ok = net.tigereye.chestcavity.compat.guzhenren.flyingsword.ops
        .RepairOps.repairSelected(player.serverLevel(), player);
    return ok ? 1 : 0;
  }

  private static int repairByIndex(CommandContext<CommandSourceStack> ctx)
      throws CommandSyntaxException {
    ServerPlayer player = ctx.getSource().getPlayerOrException();
    int index = IntegerArgumentType.getInteger(ctx, "index");
    boolean ok = net.tigereye.chestcavity.compat.guzhenren.flyingsword.ops
        .RepairOps.repairByIndex(player.serverLevel(), player, index);
    return ok ? 1 : 0;
  }

  private static int openUIActive(CommandContext<CommandSourceStack> ctx)
      throws CommandSyntaxException {
    ServerPlayer player = ctx.getSource().getPlayerOrException();
    int page = IntegerArgumentType.getInteger(ctx, "page");
    net.tigereye.chestcavity.compat.guzhenren.flyingsword.ui
            .FlyingSwordTUI
        .openActiveList(player, page);
    return 1;
  }

  private static int openUIStorage(CommandContext<CommandSourceStack> ctx)
      throws CommandSyntaxException {
    ServerPlayer player = ctx.getSource().getPlayerOrException();
    int page = IntegerArgumentType.getInteger(ctx, "page");
    net.tigereye.chestcavity.compat.guzhenren.flyingsword.ui
            .FlyingSwordTUI
        .openStorageList(player, page);
    return 1;
  }

  private static int withdrawByIndex(CommandContext<CommandSourceStack> ctx)
      throws CommandSyntaxException {
    ServerPlayer player = ctx.getSource().getPlayerOrException();
    int index = IntegerArgumentType.getInteger(ctx, "index");
    net.tigereye.chestcavity.compat.guzhenren.flyingsword.ui
            .FlyingSwordTUIOps
        .withdrawDisplayItem(player.serverLevel(), player, index);
    return 1;
  }

  private static int depositByIndex(CommandContext<CommandSourceStack> ctx)
      throws CommandSyntaxException {
    ServerPlayer player = ctx.getSource().getPlayerOrException();
    int index = IntegerArgumentType.getInteger(ctx, "index");
    net.tigereye.chestcavity.compat.guzhenren.flyingsword.ui
            .FlyingSwordTUIOps
        .depositMainHand(player.serverLevel(), player, index);
    return 1;
  }

  private static int debugInfo(CommandContext<CommandSourceStack> ctx)
      throws CommandSyntaxException {
    ServerPlayer player = ctx.getSource().getPlayerOrException();
    ServerLevel level = player.serverLevel();

    List<FlyingSwordEntity> swords = FlyingSwordController.getPlayerSwords(level, player);

    if (swords.isEmpty()) {
      ctx.getSource()
          .sendSuccess(() -> Component.literal("[flyingsword] No flying swords"), false);
      return 0;
    }

    StringBuilder debug = new StringBuilder();
    debug.append(
        String.format(Locale.ROOT, "[flyingsword] Debug Info (%d swords):\n", swords.size()));

    for (int i = 0; i < swords.size(); i++) {
      FlyingSwordEntity sword = swords.get(i);
      var attrs = sword.getSwordAttributes();
      net.minecraft.world.phys.Vec3 velocity = sword.getDeltaMovement();
      double speed = velocity.length();

      // 计算当前伤害
      double levelScale =
          net.tigereye.chestcavity.compat.guzhenren.flyingsword.calculator
              .FlyingSwordCalculator.calculateLevelScale(
                  sword.getSwordLevel(),
                  net.tigereye.chestcavity.compat.guzhenren.flyingsword.tuning
                      .FlyingSwordTuning.DAMAGE_PER_LEVEL);

      double damage =
          net.tigereye.chestcavity.compat.guzhenren.flyingsword.calculator
              .FlyingSwordCalculator.calculateDamageWithContext(
                  attrs.damageBase,
                  speed,
                  net.tigereye.chestcavity.compat.guzhenren.flyingsword.tuning
                      .FlyingSwordTuning.V_REF,
                  attrs.velDmgCoef,
                  levelScale,
                  net.tigereye.chestcavity.compat.guzhenren.flyingsword
                      .calculator.context.CalcContexts.from(sword));

      debug.append(String.format("\n[Sword #%d]", i + 1));
      debug.append(String.format("\n  Mode: %s", sword.getAIMode().getDisplayName()));
      debug.append(
          String.format(
              "\n  Target: %s",
              sword.getTargetEntity() != null
                  ? sword.getTargetEntity().getName().getString()
                  : "None"));
      debug.append(String.format("\n  Speed: %.3f (%.1f%%)", speed, (speed / attrs.speedMax) * 100));
      debug.append(String.format("\n  Damage Calc:"));
      debug.append(String.format("\n    Base Damage: %.1f", attrs.damageBase));
      debug.append(
          String.format(
              "\n    Speed Factor: (%.3f / %.2f)² × %.2f = %.3f",
              speed,
              net.tigereye.chestcavity.compat.guzhenren.flyingsword.tuning
                  .FlyingSwordTuning.V_REF,
              attrs.velDmgCoef,
              Math.pow(
                      speed
                          / net.tigereye.chestcavity.compat.guzhenren.flyingsword.tuning
                              .FlyingSwordTuning.V_REF,
                      2.0)
                  * attrs.velDmgCoef));
      debug.append(String.format("\n    Level Scale: %.2f", levelScale));
      debug.append(String.format("\n    Final Damage: %.2f", damage));
      debug.append(
          String.format(
              "\n  Position: (%.1f, %.1f, %.1f)",
              sword.getX(), sword.getY(), sword.getZ()));
      debug.append(String.format("\n  Distance to Player: %.1fm", sword.distanceTo(player)));
    }

    final String debugStr = debug.toString();
    ctx.getSource().sendSuccess(() -> Component.literal(debugStr), false);

    return swords.size();
  }
}
