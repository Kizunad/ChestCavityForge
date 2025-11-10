package net.tigereye.chestcavity.compat.guzhenren.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.registration.CCAttachments;

/**
 * 剑域蛊配置命令：提供可点击的聊天 TUI 用于快速设置半径缩放。
 *
 * <p>命令： - /sword_domain ui 显示可点击选项：min 10 20 30 40 50 60 70 80 90 max - /sword_domain radius set
 * <v> 直接设置缩放（正数） - /sword_domain radius preset
 *
 * <p>以预设设置（min/10/.../90/max）
 */
@EventBusSubscriber(modid = ChestCavity.MODID)
public final class SwordDomainConfigCommand {

  private SwordDomainConfigCommand() {}

  @SubscribeEvent
  public static void register(RegisterCommandsEvent event) {
    CommandDispatcher<CommandSourceStack> d = event.getDispatcher();
    d.register(
        Commands.literal("sword_domain")
            .then(Commands.literal("ui").executes(SwordDomainConfigCommand::openTui))
            .then(
                Commands.literal("radius")
                    .then(
                        Commands.literal("set")
                            .then(
                                Commands.argument("value", DoubleArgumentType.doubleArg(0.000001))
                                    .executes(SwordDomainConfigCommand::setRadiusScale)))
                    .then(
                        Commands.literal("preset")
                            .then(
                                Commands.argument("value", StringArgumentType.word())
                                    .executes(SwordDomainConfigCommand::setPreset)))));
  }

  private static int openTui(CommandContext<CommandSourceStack> ctx) {
    CommandSourceStack src = ctx.getSource();
    if (!(src.getEntity() instanceof ServerPlayer player)) {
      src.sendFailure(Component.literal("只有玩家可以使用此命令"));
      return 0;
    }
    if (!hasJianYuGuEquipped(player)) {
      player.sendSystemMessage(Component.literal("§c需要装备§e 剑域蛊 §c才可调整半径。"));
      return 0;
    }
    openTuiFor(player);
    return 1;
  }

  /** 供其他逻辑（如器官主动）直接打开 TUI 使用。 */
  public static void openTuiFor(ServerPlayer player) {
    if (!hasJianYuGuEquipped(player)) {
      player.sendSystemMessage(Component.literal("§c需要装备§e 剑域蛊 §c才可调整半径。"));
      return;
    }
    // 当前值
    double cur = CCAttachments.getSwordDomainConfig(player).getRadiusScale();
    player.sendSystemMessage(Component.literal("§a剑域蛊：半径缩放当前值 §e" + fmt(cur)));

    // 按钮行（min 10 20 ... 90 max）
    String[] labels = {"min", "10", "20", "30", "40", "50", "60", "70", "80", "90", "max"};
    MutableComponent line = Component.literal("");
    for (int i = 0; i < labels.length; i++) {
      String label = labels[i];
      double value = mapPreset(label);
      MutableComponent btn =
          Component.literal("[" + label + "]")
              .withStyle(
                  Style.EMPTY
                      .withColor(ChatFormatting.AQUA)
                      .withBold(false)
                      .withUnderlined(true)
                      .withClickEvent(
                          new ClickEvent(
                              ClickEvent.Action.RUN_COMMAND,
                              "/sword_domain radius set " + fmt(value)))
                      .withHoverEvent(
                          new net.minecraft.network.chat.HoverEvent(
                              net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT,
                              Component.literal("设置为 " + fmt(value) + " (R=BASE*scale)"))));
      line.append(btn);
      if (i < labels.length - 1) {
        line.append(Component.literal(" "));
      }
    }
    player.sendSystemMessage(line);
  }

  private static int setRadiusScale(CommandContext<CommandSourceStack> ctx) {
    CommandSourceStack src = ctx.getSource();
    if (!(src.getEntity() instanceof ServerPlayer player)) {
      src.sendFailure(Component.literal("只有玩家可以使用此命令"));
      return 0;
    }
    if (!hasJianYuGuEquipped(player)) {
      player.sendSystemMessage(Component.literal("§c需要装备§e 剑域蛊 §c才可调整半径。"));
      return 0;
    }
    double value = DoubleArgumentType.getDouble(ctx, "value");
    if (!(value > 0.0) || !Double.isFinite(value)) {
      player.sendSystemMessage(Component.literal("§c无效的数值"));
      return 0;
    }
    CCAttachments.getSwordDomainConfig(player).setRadiusScale(value);
    player.sendSystemMessage(Component.literal("§a剑域蛊：半径缩放已设置为 §e" + fmt(value)));
    return 1;
  }

  private static int setPreset(CommandContext<CommandSourceStack> ctx) {
    CommandSourceStack src = ctx.getSource();
    if (!(src.getEntity() instanceof ServerPlayer player)) {
      src.sendFailure(Component.literal("只有玩家可以使用此命令"));
      return 0;
    }
    if (!hasJianYuGuEquipped(player)) {
      player.sendSystemMessage(Component.literal("§c需要装备§e 剑域蛊 §c才可调整半径。"));
      return 0;
    }
    String label = StringArgumentType.getString(ctx, "value");
    double value = mapPreset(label);
    if (!(value > 0.0)) {
      player.sendSystemMessage(Component.literal("§c无效的预设：" + label));
      return 0;
    }
    CCAttachments.getSwordDomainConfig(player).setRadiusScale(value);
    player.sendSystemMessage(Component.literal("§a剑域蛊：半径预设 §b" + label + " §a→ §e" + fmt(value)));
    return 1;
  }

  // 预设映射：
  // - min  → 0.2（确保 BASE_RADIUS=5 时最小≈1格半径）
  // - 10..90 → 按百分比映射到 (0,10] 的 10%..90%：即 1.0..9.0 倍
  // - max  → 10.0（最大支持10倍半径）
  private static double mapPreset(String label) {
    String v = label.toLowerCase();
    if (v.equals("min")) return 0.2; // BASE(5)*0.2=1格
    if (v.equals("max")) return 10.0; // 最大10倍半径
    try {
      int n = Integer.parseInt(v);
      if (n >= 0 && n <= 100) {
        // 百分比到[1.0,9.0]的线性映射（相对“max=10.0”）
        if (n == 0) return 0.2; // 防御性：0 视作最小
        if (n == 100) return 10.0; // 100 视作最大
        return 10.0 * (n / 100.0);
      }
    } catch (NumberFormatException ignore) {
    }
    return -1.0;
  }

  private static String fmt(double d) {
    return String.format(java.util.Locale.ROOT, "%.2f", d);
  }

  private static boolean hasJianYuGuEquipped(ServerPlayer player) {
    // 检查胸腔物品是否包含 guzhenren:jianyugu
    var ccOpt = net.tigereye.chestcavity.registration.CCAttachments.getExistingChestCavity(player);
    if (ccOpt.isEmpty()) return false;
    var cc = ccOpt.get();
    if (cc.inventory == null) return false;
    final net.minecraft.resources.ResourceLocation targetId =
        net.minecraft.resources.ResourceLocation.parse("guzhenren:jianyugu");
    for (int i = 0, n = cc.inventory.getContainerSize(); i < n; i++) {
      ItemStack s = cc.inventory.getItem(i);
      if (s.isEmpty()) continue;
      net.minecraft.resources.ResourceLocation id =
          net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(s.getItem());
      if (id != null && id.equals(targetId)) {
        return true;
      }
    }
    return false;
  }
}
