package net.tigereye.chestcavity.compat.guzhenren.flyingsword.ui;

import net.minecraft.server.level.ServerPlayer;

/**
 * 预留的 TUI 清屏接口（占位实现）。
 *
 * <p>后续可在此实现三种模式之一：
 *
 * <ul>
 *   <li>PUSH: 发送空行将上次 TUI 推出可视区
 *   <li>CLEAR_ALL: 客户端清空聊天窗口
 *   <li>PRECISE: 精确删除上一块 TUI 行
 * </ul>
 */
public final class TUIRefreshOps {
  private TUIRefreshOps() {}

  public enum ClearMode {
    PUSH,
    CLEAR_ALL,
    PRECISE
  }

  /** 预留：按模式清理上一块 TUI。当前为空实现。 */
  public static void clear(ServerPlayer player, ClearMode mode) {
    // 占位：后续实现。此处留空以避免日志噪音。
  }

  /** 预留：常规刷新场景，默认使用 PUSH 策略。 */
  public static void clearPrevious(ServerPlayer player) {
    clear(player, ClearMode.PUSH);
  }
}
