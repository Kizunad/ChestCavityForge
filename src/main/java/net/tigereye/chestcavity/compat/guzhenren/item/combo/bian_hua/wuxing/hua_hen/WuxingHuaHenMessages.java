package net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.wuxing.hua_hen;

import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.wuxing.hua_hen.state.WuxingHuaHenAttachment.Element;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.wuxing.hua_hen.state.WuxingHuaHenAttachment.Mode;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.framework.ComboSkillMessenger;

/**
 * 五行化痕 消息工具。
 */
public final class WuxingHuaHenMessages {

  private WuxingHuaHenMessages() {}

  public static void sendFailure(ServerPlayer player, String message) {
    ComboSkillMessenger.sendFailure(player, "[五行化痕] " + message);
  }

  public static void sendTransmuteSuccess(
      ServerPlayer player,
      Element element,
      double amountIn,
      double amountOut,
      double tax,
      long undoSeconds) {
    ComboSkillMessenger.sendSuccess(
        player,
        String.format(
            "§a五行化痕：§f变化道 §e%.1f §f→ §b%s §e%.1f §7(税率 %.1f%%) §8[%d秒内可撤销]",
            amountIn, getElementName(element), amountOut, tax * 100.0, undoSeconds));
  }

  public static void warnUndoExpiry(ServerPlayer player, Element element, long seconds) {
    ComboSkillMessenger.sendSuccess(
        player,
        String.format(
            "§e⚠ 警告：§b%s §f转化将在 §c%d秒 §f后无法撤销！",
            getElementName(element), seconds));
  }

  public static void sendUndoExpired(ServerPlayer player) {
    sendFailure(player, "§c撤销窗口已过期，无法撤销上次转化。");
  }

  public static void sendUndoMissing(ServerPlayer player) {
    sendFailure(player, "§c无可撤销的转化记录。");
  }

  public static void sendUndoInsufficient(ServerPlayer player, double need, double current) {
    sendFailure(
        player,
        String.format("目标道痕不足，无法撤销（需要 %.1f，当前 %.1f）。", need, current));
  }

  public static void sendUndoSuccess(
      ServerPlayer player, Element element, double amountOut, double returnAmount, long seconds) {
    ComboSkillMessenger.sendSuccess(
        player,
        String.format(
            "§a撤销成功：§b%s §e%.1f §f→ 变化道 §e%.1f §7(返还率 80%%) §8[剩余 %d 秒]",
            getElementName(element), amountOut, returnAmount, seconds));
  }

  public static void sendUndoStatus(
      ServerPlayer player, Element element, double amountOut, double returnAmount, long seconds) {
    String color = seconds <= 10 ? "c" : "a";
    ComboSkillMessenger.sendSystem(
        player,
        String.format(
            "§6═══ 撤销状态 ═══\n"
                + "§f上次转化：§b%s §e%.1f\n"
                + "§f可返还：变化道 §e%.1f §7(80%%)\n"
                + "§f剩余时间：§%s%d秒\n"
                + "§7提示：使用撤销技能可返还道痕",
            getElementName(element), amountOut, returnAmount, color, seconds));
  }

  public static void sendNoUndoStatus(ServerPlayer player) {
    ComboSkillMessenger.sendSystem(player, "§7当前无可撤销的转化记录。");
  }

  public static void sendConfigHeader(ServerPlayer player, Element element, Mode mode) {
    ComboSkillMessenger.sendSystem(player, "§6═══════ 五行化痕配置 ═══════");
    ComboSkillMessenger.sendSystem(
        player,
        String.format(
            "§f当前配置：§b%s §7| §e%s", getElementName(element), getModeName(mode)));
    ComboSkillMessenger.sendSystem(player, "");
  }

  public static void sendConfigFooter(ServerPlayer player) {
    ComboSkillMessenger.sendSystem(player, "§6═══════════════════════════");
  }

  public static MutableComponent buildElementOptions(Element current) {
    MutableComponent line = Component.literal("");
    line.append(buildElementOption(Element.JIN, current));
    line.append(Component.literal(" §8| "));
    line.append(buildElementOption(Element.MU, current));
    line.append(Component.literal(" §8| "));
    line.append(buildElementOption(Element.SHUI, current));
    line.append(Component.literal(" §8| "));
    line.append(buildElementOption(Element.YAN, current));
    line.append(Component.literal(" §8| "));
    line.append(buildElementOption(Element.TU, current));
    return line;
  }

  public static MutableComponent buildRatioModes(Mode current) {
    MutableComponent line = Component.literal("§7比例：");
    line.append(buildModeOption(Mode.ALL, current, "全部"));
    line.append(Component.literal(" §8| "));
    line.append(buildModeOption(Mode.RATIO_25, current, "25%"));
    line.append(Component.literal(" §8| "));
    line.append(buildModeOption(Mode.RATIO_50, current, "50%"));
    line.append(Component.literal(" §8| "));
    line.append(buildModeOption(Mode.RATIO_100, current, "100%"));
    return line;
  }

  public static MutableComponent buildFixedModes(Mode current) {
    MutableComponent line = Component.literal("§7固定：");
    line.append(buildModeOption(Mode.FIXED_10, current, "10"));
    line.append(Component.literal(" §8| "));
    line.append(buildModeOption(Mode.FIXED_25, current, "25"));
    line.append(Component.literal(" §8| "));
    line.append(buildModeOption(Mode.FIXED_50, current, "50"));
    line.append(Component.literal(" §8| "));
    line.append(buildModeOption(Mode.FIXED_100, current, "100"));
    return line;
  }

  private static MutableComponent buildElementOption(Element element, Element current) {
    boolean selected = element == current;
    String label = switch (element) {
      case JIN -> "金";
      case MU -> "木";
      case SHUI -> "水";
      case YAN -> "炎";
      case TU -> "土";
    };
    MutableComponent component = Component.literal((selected ? "§b§l" : "§f") + "[" + label + "]");
    component.setStyle(
        component
            .getStyle()
            .withClickEvent(
                new ClickEvent(
                    ClickEvent.Action.RUN_COMMAND,
                    "/wuxing_config element " + element.name().toLowerCase()))
            .withHoverEvent(
                new HoverEvent(
                    HoverEvent.Action.SHOW_TEXT,
                    Component.literal("§a点击切换到 " + getElementName(element)))));
    return component;
  }

  private static MutableComponent buildModeOption(Mode mode, Mode current, String label) {
    boolean selected = mode == current;
    MutableComponent component = Component.literal((selected ? "§e§l" : "§7") + "[" + label + "]");
    component.setStyle(
        component
            .getStyle()
            .withClickEvent(
                new ClickEvent(
                    ClickEvent.Action.RUN_COMMAND,
                    "/wuxing_config mode " + mode.name().toLowerCase()))
            .withHoverEvent(
                new HoverEvent(
                    HoverEvent.Action.SHOW_TEXT,
                    Component.literal("§a点击切换到 " + getModeName(mode)))));
    return component;
  }

  public static String getElementName(Element element) {
    return switch (element) {
      case JIN -> "金道";
      case MU -> "木道";
      case SHUI -> "水道";
      case YAN -> "炎道";
      case TU -> "土道";
    };
  }

  public static String getModeName(Mode mode) {
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
