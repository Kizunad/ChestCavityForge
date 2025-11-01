package net.tigereye.chestcavity.compat.guzhenren.item.combo.jian_dao.messages;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.jian_dao.tuning.JianDaoComboTuning;

/**
 * 剑道组合杀招消息（占位）。
 */
public final class JianDaoComboMessages {
  private JianDaoComboMessages() {}

  public static final Component WINDOW_OPEN = Component.literal("剑道连携：窗口开启");
  public static final Component INSUFFICIENT_RESOURCES = Component.literal("真元/资源不足");

  public static void sendAction(ServerPlayer player, Component msg) {
    if (!JianDaoComboTuning.MESSAGES_ENABLED || player == null) return;
    player.displayClientMessage(msg, true);
  }

  public static void sendFailure(ServerPlayer player, Component msg) {
    if (!JianDaoComboTuning.MESSAGES_ENABLED || player == null) return;
    player.displayClientMessage(msg, true);
  }
}

