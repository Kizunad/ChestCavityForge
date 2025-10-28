package net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.wuxing.gui_bian;

import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.wuxing.hua_hen.state.WuxingHuaHenAttachment;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.framework.ComboSkillMessenger;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.wuxing.gui_bian.state.WuxingGuiBianAttachment;

/**
 * 五行归变·逆转 消息组装。
 */
public final class WuxingGuiBianMessages {

  private WuxingGuiBianMessages() {}

  public static void sendSuccessConversion(
      ServerPlayer player,
      WuxingHuaHenAttachment.Element element,
      double amountIn,
      double amountOut,
      double tax,
      int anchorCount,
      boolean temporaryMode,
      long freezeSeconds) {

    if (temporaryMode) {
      ComboSkillMessenger.sendSuccess(
          player,
          String.format(
              "§a五行归变·逆转：§b%s §e%.1f §f→ 变化道 §e%.1f §7(税率 %.1f%%, %d锚) §8[暂时模式: %d秒后返还]",
              getElementName(element),
              amountIn,
              amountOut,
              tax * 100.0,
              anchorCount,
              freezeSeconds));
    } else {
      ComboSkillMessenger.sendSuccess(
          player,
          String.format(
              "§a五行归变·逆转：§b%s §e%.1f §f→ 变化道 §e%.1f §7(税率 %.1f%%, %d锚) §6[永久模式]",
              getElementName(element),
              amountIn,
              amountOut,
              tax * 100.0,
              anchorCount));
    }
  }

  public static void sendTempReturnSuccess(
      ServerPlayer player,
      WuxingHuaHenAttachment.Element element,
      double amountOut,
      double amountIn) {
    ComboSkillMessenger.sendSuccess(
        player,
        String.format(
            "§a暂时转化到期：变化道 §e%.1f §f→ §b%s §e%.1f §7(全额返还)",
            amountOut, getElementName(element), amountIn));
  }

  public static void sendTempReturnFailure(
      ServerPlayer player, double needed, double currentBianhua) {
    ComboSkillMessenger.sendFailure(
        player,
        String.format(
            "§c暂时转化到期，但变化道不足以返还（需要 %.1f，当前 %.1f）。转化已作废。",
            needed, currentBianhua));
  }

  public static void sendPendingWarning(
      ServerPlayer player,
      WuxingHuaHenAttachment.Element element,
      long remainingSeconds) {
    ComboSkillMessenger.sendSuccess(
        player,
        String.format(
            "§e⚠ 警告：§b%s §f→ 变化道 暂时转化将在 §c%d秒 §f后自动返还！",
            getElementName(element), remainingSeconds));
  }

  public static void sendFreezeDetails(
      ServerPlayer player, WuxingGuiBianAttachment.FreezeSnapshot snapshot, long remainingSeconds) {
    ComboSkillMessenger.sendSystem(
        player,
        String.format(
            "§d暂时模式冻结：§b%s §e%.1f §7→ 变化道 §e%.1f §7(剩余 §c%d秒§7)",
            getElementName(snapshot.element()),
            snapshot.amountConsumed(),
            snapshot.amountOut(),
            remainingSeconds));
  }

  public static void sendConfigHeader(ServerPlayer player, WuxingGuiBianAttachment.ConversionMode mode) {
    ComboSkillMessenger.sendSystem(player, "§6═══════ 五行归变·逆转 配置 ═══════");
    ComboSkillMessenger.sendSystem(player, "§f当前转化模式：§b" + getConversionModeName(mode));
  }

  public static void sendConfigFooter(ServerPlayer player) {
    ComboSkillMessenger.sendSystem(player, "§7元素与数量沿用 §a五行化痕·配置§7，可通过该技能调整。");
    ComboSkillMessenger.sendSystem(player, "§6═══════════════════════════");
  }

  public static MutableComponent buildConversionOptionComponent(
      WuxingGuiBianAttachment.ConversionMode option,
      WuxingGuiBianAttachment.ConversionMode current) {
    boolean selected = option == current;
    String label = switch (option) {
      case TEMPORARY -> "§b暂时模式";
      case PERMANENT -> "§6永久模式";
    };
    MutableComponent component = Component.literal(selected ? "§a[✔] " : "§7[ ] ");
    component.append(Component.literal(label));
    component.withStyle(
        style ->
            style
                .withHoverEvent(
                    new HoverEvent(
                        HoverEvent.Action.SHOW_TEXT, Component.literal(getConversionModeHover(option))))
                .withClickEvent(
                    new ClickEvent(
                        ClickEvent.Action.RUN_COMMAND,
                        "/wuxing_gui_config mode "
                            + (option == WuxingGuiBianAttachment.ConversionMode.TEMPORARY
                                ? "temporary"
                                : "permanent"))));
    return component;
  }

  public static String getConversionModeName(WuxingGuiBianAttachment.ConversionMode mode) {
    return switch (mode) {
      case TEMPORARY -> "暂时模式（20秒后自动返还，无税）";
      case PERMANENT -> "永久模式（立即转换，计税）";
    };
  }

  public static String getConversionModeHover(WuxingGuiBianAttachment.ConversionMode mode) {
    return switch (mode) {
      case TEMPORARY -> "20秒冻结，期满自动返还。适合临时需要变化道时使用。";
      case PERMANENT -> "直接转化为变化道，按锚点与阴阳姿态计税。";
    };
  }

  public static String getElementName(WuxingHuaHenAttachment.Element element) {
    return switch (element) {
      case JIN -> "金痕";
      case MU -> "木痕";
      case SHUI -> "水痕";
      case YAN -> "炎痕";
      case TU -> "土痕";
    };
  }
}
