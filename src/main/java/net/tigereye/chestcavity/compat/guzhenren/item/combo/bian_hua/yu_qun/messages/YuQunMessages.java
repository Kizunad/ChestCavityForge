package net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yu_qun.messages;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** 鱼群·组合 消息集中地（便于本地化与管理）。 */
public final class YuQunMessages {
  private YuQunMessages() {}

  public static void sendFailure(ServerPlayer player, String text) {
    if (player != null) {
      player.displayClientMessage(Component.literal(text), true);
    }
  }

  public static void sendAction(ServerPlayer player, String text) {
    if (player != null) {
      player.displayClientMessage(Component.literal(text), true);
    }
  }
}

