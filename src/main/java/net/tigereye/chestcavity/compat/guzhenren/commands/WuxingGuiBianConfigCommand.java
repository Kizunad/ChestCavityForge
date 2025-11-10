package net.tigereye.chestcavity.compat.guzhenren.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.wuxing.gui_bian.state.WuxingGuiBianAttachment;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.wuxing.gui_bian.state.WuxingGuiBianAttachment.ConversionMode;
import net.tigereye.chestcavity.registration.CCAttachments;

/** Command handler for configuring 五行归变·逆转. Usage: /wuxing_gui_config mode <temporary|permanent> */
public final class WuxingGuiBianConfigCommand {

  private WuxingGuiBianConfigCommand() {}

  public static void register(RegisterCommandsEvent event) {
    CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
    dispatcher.register(
        Commands.literal("wuxing_gui_config")
            .then(
                Commands.literal("mode")
                    .then(
                        Commands.argument("value", StringArgumentType.word())
                            .executes(WuxingGuiBianConfigCommand::setMode))));
  }

  private static int setMode(CommandContext<CommandSourceStack> context) {
    CommandSourceStack source = context.getSource();
    if (!(source.getEntity() instanceof ServerPlayer player)) {
      source.sendFailure(Component.literal("只有玩家可以使用此命令"));
      return 0;
    }

    String value = StringArgumentType.getString(context, "value");
    ConversionMode mode = parseMode(value);
    if (mode == null) {
      player.sendSystemMessage(
          Component.literal("§c无效的模式：" + value + " (可选: temporary, permanent)"));
      return 0;
    }

    WuxingGuiBianAttachment attachment = CCAttachments.getWuxingGuiBian(player);
    attachment.setLastMode(mode);

    player.sendSystemMessage(Component.literal("§a五行归变·逆转 §f转化模式已设置为：§b" + getModeName(mode)));
    return 1;
  }

  private static ConversionMode parseMode(String value) {
    return switch (value.toLowerCase()) {
      case "temporary", "temp" -> ConversionMode.TEMPORARY;
      case "permanent", "perm" -> ConversionMode.PERMANENT;
      default -> null;
    };
  }

  private static String getModeName(ConversionMode mode) {
    return switch (mode) {
      case TEMPORARY -> "暂时模式";
      case PERMANENT -> "永久模式";
    };
  }
}
