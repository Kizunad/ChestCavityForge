package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword;

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
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.AIMode;

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
            .requires(src -> src.hasPermission(2))
            // /flyingsword spawn [count]
            .then(
                Commands.literal("spawn")
                    .executes(FlyingSwordCommand::spawnOne)
                    .then(
                        Commands.argument("count", IntegerArgumentType.integer(1, 10))
                            .executes(FlyingSwordCommand::spawnMultiple)))
            // /flyingsword test spawn
            .then(
                Commands.literal("test")
                    .then(Commands.literal("spawn").executes(FlyingSwordCommand::testSpawn)))
            // /flyingsword recall
            .then(Commands.literal("recall").executes(FlyingSwordCommand::recallAll))
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
                                  return builder.buildFuture();
                                })
                            .executes(FlyingSwordCommand::setMode)))
            // /flyingsword status
            .then(Commands.literal("status").executes(FlyingSwordCommand::showStatus))
            // /flyingsword list
            .then(Commands.literal("list").executes(FlyingSwordCommand::listSwords))
            // /flyingsword clear
            .then(Commands.literal("clear").executes(FlyingSwordCommand::clearTargets)));
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
                      "[flyingsword] Invalid mode: %s (valid: orbit, guard, hunt)", modeStr)));
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
              net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.calculator
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
}
