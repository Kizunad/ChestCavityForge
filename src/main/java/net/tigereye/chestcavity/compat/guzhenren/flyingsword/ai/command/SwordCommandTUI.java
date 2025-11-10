package net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.command;

import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.FlyingSwordEntity;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ui.TUITheme;

/** 剑引指挥棒的聊天式 TUI。 */
public final class SwordCommandTUI {

  private SwordCommandTUI() {}

  static void open(ServerPlayer player, SwordCommandCenter.CommandSession session) {
    // 估算内容宽度并初始化frame
    String titleText = Component.translatable("text.guzhenren.jianyingu.command.title").getString();
    int frameWidth = TUITheme.estimateFrameWidthFromStrings(40, titleText);
    TUITheme.beginFrame(frameWidth);

    // 顶部边框
    player.sendSystemMessage(TUITheme.createTopBorder(titleText));

    // 目标信息行
    int currentGroup = session.groupId();
    int marked = session.markedCount(currentGroup);
    MutableComponent targetInfo =
        Component.translatable("text.guzhenren.jianyingu.command.targets", marked)
            .withStyle(TUITheme.TEXT);
    if (session.hasExecutingGroup(currentGroup)) {
      long remaining =
          Math.max(0, session.executingUntil(currentGroup) - player.level().getGameTime());
      double seconds = remaining / 20.0;
      targetInfo =
          targetInfo
              .append(space())
              .append(
                  Component.translatable(
                          "text.guzhenren.jianyingu.command.state.executing",
                          String.format(Locale.ROOT, "%.1f", seconds))
                      .withStyle(TUITheme.DIM));
    } else if (session.hasSelectionActive()) {
      long remaining = Math.max(0, session.selectionExpiresAt() - player.level().getGameTime());
      double seconds = remaining / 20.0;
      targetInfo =
          targetInfo
              .append(space())
              .append(
                  Component.translatable(
                          "text.guzhenren.jianyingu.command.state.selecting",
                          String.format(Locale.ROOT, "%.1f", seconds))
                      .withStyle(TUITheme.DIM));
    } else {
      targetInfo =
          targetInfo
              .append(space())
              .append(
                  Component.translatable("text.guzhenren.jianyingu.command.state.idle")
                      .withStyle(TUITheme.DIM));
    }
    player.sendSystemMessage(TUITheme.wrapContentLine(targetInfo));

    // 其他组概览
    long now = player.level().getGameTime();
    var otherGroups = session.groupSummaries(now);
    MutableComponent otherLine = null;
    boolean hasOther = false;
    for (SwordCommandCenter.CommandSession.GroupSummary summary : otherGroups) {
      if (summary.groupId() == currentGroup) {
        continue;
      }
      if (!summary.hasActivity()) {
        continue;
      }
      if (!hasOther) {
        otherLine =
            Component.translatable("text.guzhenren.jianyingu.command.group.overview")
                .withStyle(TUITheme.DIM);
        hasOther = true;
      }
      otherLine = otherLine.append(space()).append(otherGroupStatus(summary));
    }
    if (hasOther && otherLine != null) {
      player.sendSystemMessage(TUITheme.wrapContentLine(otherLine));
    }

    // 分隔线
    player.sendSystemMessage(TUITheme.createDivider());

    // 战术选择行
    MutableComponent tacticLine =
        Component.translatable("text.guzhenren.jianyingu.command.tactic")
            .withStyle(TUITheme.ACCENT);
    for (CommandTactic tactic : CommandTactic.values()) {
      tacticLine =
          tacticLine.append(space()).append(tacticButton(tactic, session.tactic() == tactic));
    }
    player.sendSystemMessage(TUITheme.wrapContentLine(tacticLine));

    // 分组选择行
    MutableComponent groupLine =
        Component.translatable("text.guzhenren.jianyingu.command.group")
            .withStyle(TUITheme.ACCENT)
            .append(space())
            .append(
                groupButton(
                    0,
                    currentGroup == 0,
                    Component.translatable("text.guzhenren.jianyingu.command.group.all")));
    groupLine =
        groupLine
            .append(space())
            .append(
                groupButton(
                    1,
                    currentGroup == 1,
                    Component.translatable("text.guzhenren.jianyingu.command.group.g1")))
            .append(space())
            .append(
                groupButton(
                    2,
                    currentGroup == 2,
                    Component.translatable("text.guzhenren.jianyingu.command.group.g2")))
            .append(space())
            .append(
                groupButton(
                    3,
                    currentGroup == 3,
                    Component.translatable("text.guzhenren.jianyingu.command.group.g3")));
    if (net.tigereye.chestcavity.compat.guzhenren.flyingsword.tuning.FlyingSwordTuning
        .ENABLE_SWARM) {
      groupLine =
          groupLine
              .append(space())
              .append(
                  groupButton(
                      FlyingSwordEntity.SWARM_GROUP_ID,
                      currentGroup == FlyingSwordEntity.SWARM_GROUP_ID,
                      Component.translatable("text.guzhenren.jianyingu.command.group.swarm")));
    }
    player.sendSystemMessage(TUITheme.wrapContentLine(groupLine));

    // 操作按钮行
    MutableComponent actions =
        Component.translatable("text.guzhenren.jianyingu.command.actions")
            .withStyle(TUITheme.ACCENT)
            .append(space())
            .append(
                button(
                    Component.translatable("text.guzhenren.jianyingu.command.button.execute"),
                    "/jianyin command execute",
                    Component.translatable(
                        "text.guzhenren.jianyingu.command.button.execute.hover")))
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
                    Component.translatable(
                        "text.guzhenren.jianyingu.command.button.refresh.hover")));
    player.sendSystemMessage(TUITheme.wrapContentLine(actions));

    // 底部边框
    player.sendSystemMessage(TUITheme.createBottomBorder());
  }

  private static MutableComponent tacticButton(CommandTactic tactic, boolean selected) {
    MutableComponent label =
        Component.literal("[").append(tactic.displayName()).append(Component.literal("]"));
    ChatFormatting color = selected ? TUITheme.SUCCESS : TUITheme.LABEL;
    Style style =
        Style.EMPTY
            .withColor(color.getColor())
            .withBold(selected)
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

  private static MutableComponent button(Component display, String command, Component hoverText) {
    return Component.literal("[")
        .append(display)
        .append(Component.literal("]"))
        .withStyle(
            Style.EMPTY
                .withColor(TUITheme.BUTTON.getColor())
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverText)));
  }

  private static MutableComponent groupButton(int groupId, boolean selected, Component label) {
    MutableComponent content = Component.literal("[").append(label).append(Component.literal("]"));
    ChatFormatting color = selected ? TUITheme.VALUE : TUITheme.LABEL;
    Style style =
        Style.EMPTY
            .withColor(color.getColor())
            .withBold(selected)
            .withClickEvent(
                new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/jianyin command group " + groupId))
            .withHoverEvent(
                new HoverEvent(
                    HoverEvent.Action.SHOW_TEXT,
                    Component.translatable(
                        "text.guzhenren.jianyingu.command.group.button.hover",
                        groupLabel(groupId))));
    return content.withStyle(style);
  }

  private static Component groupLabel(int groupId) {
    if (groupId == 0) {
      return Component.translatable("text.guzhenren.jianyingu.command.group.all");
    }
    if (groupId == 1) {
      return Component.translatable("text.guzhenren.jianyingu.command.group.g1");
    }
    if (groupId == 2) {
      return Component.translatable("text.guzhenren.jianyingu.command.group.g2");
    }
    if (groupId == 3) {
      return Component.translatable("text.guzhenren.jianyingu.command.group.g3");
    }
    if (groupId == FlyingSwordEntity.SWARM_GROUP_ID) {
      return Component.translatable("text.guzhenren.jianyingu.command.group.swarm");
    }
    return Component.literal(String.format(Locale.ROOT, "#%d", groupId));
  }

  private static MutableComponent otherGroupStatus(
      SwordCommandCenter.CommandSession.GroupSummary summary) {
    Component label = groupLabel(summary.groupId());
    if (summary.executing()) {
      String seconds = String.format(Locale.ROOT, "%.1f", summary.executingSeconds());
      return Component.translatable(
              "text.guzhenren.jianyingu.command.group.summary.executing",
              label,
              summary.marks(),
              seconds)
          .withStyle(TUITheme.DIM);
    }
    return Component.translatable(
            "text.guzhenren.jianyingu.command.group.summary.idle", label, summary.marks())
        .withStyle(TUITheme.DIM);
  }

  private static MutableComponent space() {
    return Component.literal(" ");
  }
}
