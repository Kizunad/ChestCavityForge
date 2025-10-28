package net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yu_shi.messages;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class YuShiMessages {
  private YuShiMessages() {}

  public static void failure(ServerPlayer p, String text) {
    if (p != null) p.displayClientMessage(Component.literal(text), true);
  }

  public static void action(ServerPlayer p, String text) {
    if (p != null) p.displayClientMessage(Component.literal(text), true);
  }
}

