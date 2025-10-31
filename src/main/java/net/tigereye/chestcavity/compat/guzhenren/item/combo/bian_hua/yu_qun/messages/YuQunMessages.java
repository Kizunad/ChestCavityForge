package net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yu_qun.messages;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** 鱼群·组合 消息集中地（便于本地化与管理）。 */
public final class YuQunMessages {
  private static final Component MISSING_ORGAN =
      Component.literal("缺少鱼鳞蛊，无法施展鱼群·组合");
  private static final Component INSUFFICIENT_ZHENYUAN =
      Component.literal("真元不足，鱼群溃散。");
  private static final Component INSUFFICIENT_JINGLI =
      Component.literal("精力不足，鱼群溃散。");

  private YuQunMessages() {}

  public static void missingOrgan(ServerPlayer player) {
    send(player, MISSING_ORGAN, true);
  }

  public static void insufficientZhenyuan(ServerPlayer player) {
    send(player, INSUFFICIENT_ZHENYUAN, true);
  }

  public static void insufficientJingli(ServerPlayer player) {
    send(player, INSUFFICIENT_JINGLI, true);
  }

  public static void sendAction(ServerPlayer player, Component message) {
    send(player, message, true);
  }

  private static void send(ServerPlayer player, Component message, boolean overlay) {
    if (player != null && message != null) {
      player.displayClientMessage(message, overlay);
    }
  }
}
