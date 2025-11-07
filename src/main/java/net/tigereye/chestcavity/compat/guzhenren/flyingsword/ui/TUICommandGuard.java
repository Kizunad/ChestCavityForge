package net.tigereye.chestcavity.compat.guzhenren.flyingsword.ui;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

/**
 * TUI命令守卫：校验会话ID并生成友好的错误提示。
 *
 * <p>职责：
 * <ul>
 *   <li>从命令参数中提取并校验会话ID（sid）</li>
 *   <li>生成"界面已过期，请刷新"提示组件</li>
 *   <li>提供一键刷新按钮</li>
 *   <li>处理实体/物品不存在的情况</li>
 * </ul>
 */
public final class TUICommandGuard {

  private TUICommandGuard() {}

  /**
   * 校验会话结果。
   */
  public record ValidationResult(boolean isValid, Component errorMessage) {
    public static ValidationResult success() {
      return new ValidationResult(true, null);
    }

    public static ValidationResult failure(Component errorMessage) {
      return new ValidationResult(false, errorMessage);
    }
  }

  /**
   * 校验会话ID是否有效。
   *
   * @param player 玩家
   * @param providedSid 命令提供的会话ID（可能为null）
   * @param nowTick 当前游戏时间
   * @return 校验结果
   */
  public static ValidationResult validateSession(
      ServerPlayer player, String providedSid, long nowTick) {
    // 如果没有提供sid，生成过期提示
    if (providedSid == null || providedSid.isEmpty()) {
      return ValidationResult.failure(createExpiredMessage());
    }

    // 检查是否有活跃会话
    var sessionOpt =
        net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.command.SwordCommandCenter
            .session(player);

    if (sessionOpt.isEmpty()) {
      // 无会话记录时，允许通过（避免误判为过期）
      // 这种情况通常发生在会话刚被清除或玩家刚登录时
      return ValidationResult.success();
    }

    // 有会话时，校验sid是否匹配且未过期
    var session = sessionOpt.get();
    String currentSid = session.tuiSessionId();
    long expiresAt = session.tuiSessionExpiresAt();

    if (!providedSid.equals(currentSid) || nowTick >= expiresAt) {
      return ValidationResult.failure(createExpiredMessage());
    }

    return ValidationResult.success();
  }

  /**
   * 创建"界面已过期"提示消息。
   *
   * @return 带刷新按钮的提示组件
   */
  public static Component createExpiredMessage() {
    MutableComponent message =
        Component.literal("✦ ").withStyle(ChatFormatting.GOLD)
            .append(Component.literal("此界面已过期").withStyle(ChatFormatting.YELLOW))
            .append(Component.literal(" · ").withStyle(ChatFormatting.DARK_GRAY));

    // 添加刷新按钮
    MutableComponent refreshBtn = createRefreshButton();
    message.append(refreshBtn);

    return message;
  }

  /**
   * 创建"目标不存在"提示消息。
   *
   * @param targetType 目标类型（如"飞剑"、"存储物品"）
   * @return 带刷新按钮的提示组件
   */
  public static Component createNotFoundMessage(String targetType) {
    MutableComponent message =
        Component.literal("✦ ").withStyle(ChatFormatting.GOLD)
            .append(
                Component.literal(targetType + "不存在或已被移除")
                    .withStyle(ChatFormatting.YELLOW))
            .append(Component.literal(" · ").withStyle(ChatFormatting.DARK_GRAY));

    // 添加刷新按钮
    MutableComponent refreshBtn = createRefreshButton();
    message.append(refreshBtn);

    return message;
  }

  /**
   * 创建刷新按钮组件。
   *
   * @return 可点击的刷新按钮
   */
  private static MutableComponent createRefreshButton() {
    return Component.literal("[刷新]")
        .withStyle(ChatFormatting.AQUA)
        .withStyle(ChatFormatting.UNDERLINE)
        .withStyle(
            style ->
                style
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/flyingsword ui"))
                    .withHoverEvent(
                        new HoverEvent(
                            HoverEvent.Action.SHOW_TEXT,
                            Component.literal("点击重新打开界面")
                                .withStyle(ChatFormatting.GRAY))));
  }

  /**
   * 创建限流提示消息。
   *
   * @param cooldownSeconds 冷却剩余秒数
   * @return 提示组件
   */
  public static Component createRateLimitMessage(double cooldownSeconds) {
    return Component.literal("⏱ ")
        .withStyle(ChatFormatting.GOLD)
        .append(
            Component.literal(
                    String.format(
                        "界面刷新过于频繁，请稍后再试 (%.1f秒)", cooldownSeconds))
                .withStyle(ChatFormatting.YELLOW));
  }

  /**
   * 创建成功提示消息。
   *
   * @param message 提示内容
   * @return 提示组件
   */
  public static Component createSuccessMessage(String message) {
    return Component.literal("✓ ").withStyle(ChatFormatting.GREEN)
        .append(Component.literal(message).withStyle(ChatFormatting.WHITE));
  }

  /**
   * 创建警告提示消息。
   *
   * @param message 警告内容
   * @return 提示组件
   */
  public static Component createWarningMessage(String message) {
    return Component.literal("⚠ ").withStyle(ChatFormatting.YELLOW)
        .append(Component.literal(message).withStyle(ChatFormatting.WHITE));
  }
}
