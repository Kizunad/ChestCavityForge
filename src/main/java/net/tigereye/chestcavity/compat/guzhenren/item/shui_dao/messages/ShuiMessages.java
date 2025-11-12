package net.tigereye.chestcavity.compat.guzhenren.item.shui_dao.messages;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.compat.guzhenren.item.shui_dao.tuning.ShuiTuning;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.CountdownOps;

/**
 * 水道消息工具类。
 *
 * <p>管理水道相关的玩家消息提示和Toast通知。
 */
public final class ShuiMessages {
  private ShuiMessages() {}

  /**
   * 发送动作栏消息。
   *
   * @param player 玩家
   * @param key 翻译键
   * @param args 参数
   */
  public static void sendActionBar(ServerPlayer player, String key, Object... args) {
    if (player == null) {
      return;
    }
    if (!ShuiTuning.MESSAGES_ENABLED) {
      return;
    }
    player.sendSystemMessage(Component.translatable(key, args), true);
  }

  /**
   * 安排就绪Toast提示。
   *
   * @param player 玩家
   * @param icon 图标
   * @param readyAtTick 就绪时间
   * @param now 当前时间
   * @param title 标题
   * @param msg 消息
   */
  public static void scheduleReadyToast(
      ServerPlayer player,
      ItemStack icon,
      long readyAtTick,
      long now,
      String title,
      String msg) {
    if (player == null) {
      return;
    }
    if (!ShuiTuning.TOAST_ENABLED) {
      return;
    }
    if (readyAtTick <= now) {
      return;
    }
    CountdownOps.scheduleToastAt(
        player.serverLevel(),
        player,
        readyAtTick,
        now,
        icon,
        title,
        msg);
  }

  /**
   * 发送系统消息。
   *
   * @param player 玩家
   * @param key 翻译键
   * @param args 参数
   */
  public static void sendSystemMessage(ServerPlayer player, String key, Object... args) {
    if (player == null) {
      return;
    }
    if (!ShuiTuning.MESSAGES_ENABLED) {
      return;
    }
    player.sendSystemMessage(Component.translatable(key, args));
  }
}
