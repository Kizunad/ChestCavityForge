package net.tigereye.chestcavity.compat.guzhenren.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.wuxing.hua_hen.state.WuxingHuaHenAttachment;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.wuxing.hua_hen.state.WuxingHuaHenAttachment.Element;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.wuxing.hua_hen.state.WuxingHuaHenAttachment.Mode;
import net.tigereye.chestcavity.registration.CCAttachments;

/**
 * Command handler for WuxingHuaHen configuration.
 * Usage: /wuxing_config element <jin|mu|shui|yan|tu>
 *        /wuxing_config mode <all|ratio_25|ratio_50|ratio_100|fixed_10|fixed_25|fixed_50|fixed_100>
 */
public final class WuxingConfigCommand {

  private WuxingConfigCommand() {}

  public static void register(RegisterCommandsEvent event) {
    CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
    dispatcher.register(
        Commands.literal("wuxing_config")
            .then(
                Commands.literal("element")
                    .then(
                        Commands.argument("value", StringArgumentType.word())
                            .executes(WuxingConfigCommand::setElement)))
            .then(
                Commands.literal("mode")
                    .then(
                        Commands.argument("value", StringArgumentType.word())
                            .executes(WuxingConfigCommand::setMode))));
  }

  private static int setElement(CommandContext<CommandSourceStack> context) {
    CommandSourceStack source = context.getSource();
    if (!(source.getEntity() instanceof ServerPlayer player)) {
      source.sendFailure(Component.literal("只有玩家可以使用此命令"));
      return 0;
    }

    String value = StringArgumentType.getString(context, "value");
    Element element = parseElement(value);
    if (element == null) {
      player.sendSystemMessage(
          Component.literal("§c无效的元素：" + value + " (可选: jin, mu, shui, yan, tu)"));
      return 0;
    }

    WuxingHuaHenAttachment attachment = CCAttachments.getWuxingHuaHen(player);
    attachment.setLastElement(element);

    String elementName = getElementName(element);
    player.sendSystemMessage(Component.literal("§a五行化痕 §f元素已设置为：§b" + elementName));
    return 1;
  }

  private static int setMode(CommandContext<CommandSourceStack> context) {
    CommandSourceStack source = context.getSource();
    if (!(source.getEntity() instanceof ServerPlayer player)) {
      source.sendFailure(Component.literal("只有玩家可以使用此命令"));
      return 0;
    }

    String value = StringArgumentType.getString(context, "value");
    Mode mode = parseMode(value);
    if (mode == null) {
      player.sendSystemMessage(
          Component.literal(
              "§c无效的模式：" + value + " (可选: all, ratio_25, ratio_50, ratio_100, fixed_10, fixed_25, fixed_50, fixed_100)"));
      return 0;
    }

    WuxingHuaHenAttachment attachment = CCAttachments.getWuxingHuaHen(player);
    attachment.setLastMode(mode);

    // 如果是固定数量模式，也更新固定数量
    if (mode == Mode.FIXED_10) {
      attachment.setLastFixedAmount(10);
    } else if (mode == Mode.FIXED_25) {
      attachment.setLastFixedAmount(25);
    } else if (mode == Mode.FIXED_50) {
      attachment.setLastFixedAmount(50);
    } else if (mode == Mode.FIXED_100) {
      attachment.setLastFixedAmount(100);
    }

    String modeName = getModeName(mode);
    player.sendSystemMessage(Component.literal("§a五行化痕 §f模式已设置为：§e" + modeName));
    return 1;
  }

  private static Element parseElement(String value) {
    return switch (value.toLowerCase()) {
      case "jin" -> Element.JIN;
      case "mu" -> Element.MU;
      case "shui" -> Element.SHUI;
      case "yan" -> Element.YAN;
      case "tu" -> Element.TU;
      default -> null;
    };
  }

  private static Mode parseMode(String value) {
    return switch (value.toLowerCase()) {
      case "all" -> Mode.ALL;
      case "ratio_25" -> Mode.RATIO_25;
      case "ratio_50" -> Mode.RATIO_50;
      case "ratio_100" -> Mode.RATIO_100;
      case "fixed_10" -> Mode.FIXED_10;
      case "fixed_25" -> Mode.FIXED_25;
      case "fixed_50" -> Mode.FIXED_50;
      case "fixed_100" -> Mode.FIXED_100;
      default -> null;
    };
  }

  private static String getElementName(Element element) {
    return switch (element) {
      case JIN -> "金道";
      case MU -> "木道";
      case SHUI -> "水道";
      case YAN -> "炎道";
      case TU -> "土道";
    };
  }

  private static String getModeName(Mode mode) {
    return switch (mode) {
      case LAST -> "使用上次配置";
      case ALL -> "全部转化";
      case RATIO_25 -> "25%";
      case RATIO_50 -> "50%";
      case RATIO_100 -> "100%";
      case FIXED_10 -> "固定10";
      case FIXED_25 -> "固定25";
      case FIXED_50 -> "固定50";
      case FIXED_100 -> "固定100";
    };
  }
}
