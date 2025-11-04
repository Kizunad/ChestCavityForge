package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.command;

import java.util.Locale;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;

/**
 * 剑引指挥棒的聊天式 TUI。
 */
public final class SwordCommandTUI {

  private SwordCommandTUI() {}

  static void open(ServerPlayer player, SwordCommandCenter.CommandSession session) {
    player.sendSystemMessage(banner(Component.translatable("text.guzhenren.jianyingu.command.title")));

    int marked = session.markedCount();
    MutableComponent targetInfo =
        Component.translatable("text.guzhenren.jianyingu.command.targets", marked);
    if (session.executing()) {
      long remaining = Math.max(0, session.executingUntil() - player.level().getGameTime());
      double seconds = remaining / 20.0;
      targetInfo =
          targetInfo.append(space())
              .append(
                  dim(
                      Component.translatable(
                          "text.guzhenren.jianyingu.command.state.executing",
                          String.format(Locale.ROOT, "%.1f", seconds))));
    } else if (session.selectionActive()) {
      long remaining = Math.max(0, session.selectionExpiresAt() - player.level().getGameTime());
      double seconds = remaining / 20.0;
      targetInfo =
          targetInfo.append(space())
              .append(
                  dim(
                      Component.translatable(
                          "text.guzhenren.jianyingu.command.state.selecting",
                          String.format(Locale.ROOT, "%.1f", seconds))));
    } else {
      targetInfo =
          targetInfo.append(space())
              .append(dim(Component.translatable("text.guzhenren.jianyingu.command.state.idle")));
    }
    player.sendSystemMessage(targetInfo);

    MutableComponent tacticLine =
        Component.translatable("text.guzhenren.jianyingu.command.tactic");
    for (CommandTactic tactic : CommandTactic.values()) {
      tacticLine = tacticLine.append(space()).append(tacticButton(tactic, session.tactic() == tactic));
    }
    player.sendSystemMessage(tacticLine);

    MutableComponent actions =
        Component.translatable("text.guzhenren.jianyingu.command.actions")
            .append(space())
            .append(
                button(
                    Component.translatable("text.guzhenren.jianyingu.command.button.execute"),
                    "/jianyin command execute",
                    Component.translatable("text.guzhenren.jianyingu.command.button.execute.hover")))
            .append(space())
            .append(
                button(
                    Component.translatable("text.guzhenren.jianyingu.command.button.cancel"),
                    "/jianyin command cancel",
                    Component.translatable("text.guzhenren.jianyingu.command.button.cancel.hover")))
            .append(space())
            .append(
                button(
                    Component.translatable("text.guzhenren.jianyingu.command.button.clear"),
                    "/jianyin command clear",
                    Component.translatable("text.guzhenren.jianyingu.command.button.clear.hover")))
            .append(space())
            .append(
                button(
                    Component.translatable("text.guzhenren.jianyingu.command.button.refresh"),
                    "/jianyin command open",
                    Component.translatable("text.guzhenren.jianyingu.command.button.refresh.hover")));
    player.sendSystemMessage(actions);
    player.sendSystemMessage(hr());
  }

  private static MutableComponent tacticButton(CommandTactic tactic, boolean selected) {
    MutableComponent label =
        Component.literal("[")
            .append(tactic.displayName())
            .append(Component.literal("]"));
    Style style =
        (selected
                ? Style.EMPTY.withColor(0x22FFAA).withBold(true)
                : Style.EMPTY.withColor(0xA0A0A0))
            .withClickEvent(
                new ClickEvent(
                    ClickEvent.Action.RUN_COMMAND, "/jianyin command tactic " + tactic.id()))
            .withHoverEvent(
                new HoverEvent(
                    HoverEvent.Action.SHOW_TEXT,
                    Component.empty()
                        .append(tactic.displayName().copy().withStyle(Style.EMPTY.withBold(true)))
                        .append(Component.literal("\n"))
                        .append(tactic.description())));
    return label.withStyle(style);
  }

  private static MutableComponent button(
      Component display, String command, Component hoverText) {
    return Component.literal("[")
        .append(display)
        .append(Component.literal("]"))
        .withStyle(
            Style.EMPTY
                .withColor(0x4FC3F7)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverText)));
  }

  private static MutableComponent banner(Component text) {
    return text.copy().withStyle(Style.EMPTY.withColor(0x3FD5C3));
  }

  private static MutableComponent hr() {
    return dim(Component.literal("------------------------------"));
  }

  private static MutableComponent dim(Component component) {
    return component.copy().withStyle(Style.EMPTY.withColor(0x808080));
  }

  private static MutableComponent space() {
    return Component.literal(" ");
  }
}
