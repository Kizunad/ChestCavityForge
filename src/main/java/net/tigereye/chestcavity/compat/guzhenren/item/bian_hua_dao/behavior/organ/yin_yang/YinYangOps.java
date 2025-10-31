package net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.behavior.organ.yin_yang;

import net.minecraft.server.level.ServerPlayer;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.tuning.YinYangZhuanShenGuTuning;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.state.YinYangDualityAttachment;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge.ResourceHandle;

/**
 * 阴阳转身蛊聚合入口。
 */
public final class YinYangOps {
  private YinYangOps() {}

  public static void applyModeAttributes(ServerPlayer player, YinYangDualityAttachment attachment) {
    YinYangZhuanShenGuCalculator.applyModeAttributes(player, attachment);
  }

  public static void clampHealth(ServerPlayer player) {
    YinYangZhuanShenGuCalculator.clampHealth(player);
  }

  public static void playPassiveYinFx(ServerPlayer player) {
    YinYangZhuanShenGuCalculator.playPassiveYinFx(player);
  }

  public static void playPassiveYangFx(ServerPlayer player) {
    YinYangZhuanShenGuCalculator.playPassiveYangFx(player);
  }

  public static void playBodySwitchFx(ServerPlayer player, YinYangDualityAttachment.Mode next) {
    YinYangZhuanShenGuCalculator.playBodySwitchFx(player, next);
  }

  public static void sendAction(ServerPlayer player, String message) {
    YinYangZhuanShenGuCalculator.sendAction(player, message);
  }

  public static void sendFailure(ServerPlayer player, String message) {
    YinYangZhuanShenGuCalculator.sendFailure(player, message);
  }

  public static long bodyCooldownTicks() {
    return YinYangZhuanShenGuTuning.BODY_COOLDOWN_TICKS;
  }

  public static void runPassives(
      ServerPlayer player, YinYangDualityAttachment attachment, ResourceHandle handle, long now) {
    YinYangZhuanShenGuCalculator.runPassives(player, attachment, handle, now);
  }
}
