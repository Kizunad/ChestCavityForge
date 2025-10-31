package net.tigereye.chestcavity.compat.guzhenren.item.feng_dao.messages;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.CountdownOps;

public final class FengMessages {
  private FengMessages() {}

  public static void scheduleReadyToast(
      ServerPlayer player, ItemStack icon, long readyAtTick, long now, String title, String msg) {
    if (player == null) return;
    if (!net.tigereye.chestcavity.compat.guzhenren.item.feng_dao.tuning.FengTuning.TOAST_ENABLED) {
      return;
    }
    if (readyAtTick <= now) return;
    CountdownOps.scheduleToastAt(player.serverLevel(), player, readyAtTick, now, icon, title, msg);
  }

  public static void sendActionBar(ServerPlayer player, String key, Object... args) {
    if (player == null) return;
    player.sendSystemMessage(Component.translatable(key, args));
  }
}
