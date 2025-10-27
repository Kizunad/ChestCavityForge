package net.tigereye.chestcavity.compat.guzhenren.item.combo.framework;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * 组合杀招消息工具。
 *
 * <p>集中管理提示样式，方便后续统一视觉语调或增加日志钩子。
 */
public final class ComboSkillMessenger {

  private ComboSkillMessenger() {}

  public static void sendSuccess(ServerPlayer player, String message) {
    player.displayClientMessage(Component.literal(message), true);
  }

  public static void sendFailure(ServerPlayer player, String message) {
    player.displayClientMessage(Component.literal(message), true);
  }

  public static void sendSystem(ServerPlayer player, String message) {
    player.sendSystemMessage(Component.literal(message));
  }

  public static void send(ServerPlayer player, Component component) {
    player.sendSystemMessage(component);
  }
}

