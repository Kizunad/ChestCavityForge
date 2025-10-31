package net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yu_shi.messages;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class YuShiMessages {
  private static final Component MISSING_ORGAN =
      Component.literal("缺少鱼鳞蛊，无法施展饵祭召鲨（组合）");
  private static final Component NEED_OFFERING =
      Component.literal("需要整组鲨材作为供品（牙齿/鱼鳍 ×64）");
  private static final Component INVALID_OFFERING =
      Component.literal("供品无效，召鲨仪式失败");
  private static final Component INSUFFICIENT_ZHENYUAN =
      Component.literal("真元不足，召鲨仪式失败。");
  private static final Component INSUFFICIENT_JINGLI =
      Component.literal("精力不足，召鲨仪式失败。");

  private YuShiMessages() {}

  public static void missingOrgan(ServerPlayer player) {
    send(player, MISSING_ORGAN, true);
  }

  public static void needOffering(ServerPlayer player) {
    send(player, NEED_OFFERING, true);
  }

  public static void invalidOffering(ServerPlayer player) {
    send(player, INVALID_OFFERING, true);
  }

  public static void insufficientZhenyuan(ServerPlayer player) {
    send(player, INSUFFICIENT_ZHENYUAN, true);
  }

  public static void insufficientJingli(ServerPlayer player) {
    send(player, INSUFFICIENT_JINGLI, true);
  }

  public static void action(ServerPlayer player, Component message) {
    send(player, message, true);
  }

  private static void send(ServerPlayer player, Component message, boolean overlay) {
    if (player != null && message != null) {
      player.displayClientMessage(message, overlay);
    }
  }
}
