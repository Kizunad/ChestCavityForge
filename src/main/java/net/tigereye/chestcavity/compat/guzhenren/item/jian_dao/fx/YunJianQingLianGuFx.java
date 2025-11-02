package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.fx;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * 蕴剑青莲蛊轻量FX提示
 *
 * <p>UI toast、音效入口；默认克制。
 */
public final class YunJianQingLianGuFx {

  private YunJianQingLianGuFx() {}

  /**
   * 发送技能激活提示
   *
   * @param player 玩家
   * @param swordCount 飞剑数量
   */
  public static void sendActivationToast(ServerPlayer player, int swordCount) {
    player.displayClientMessage(Component.literal("§a§l蕴剑青莲·开"), true);
    player.displayClientMessage(
        Component.literal("§7" + swordCount + "片莲瓣环护，无敌焦点已开启"), false);
  }

  /**
   * 发送技能关闭提示
   *
   * @param player 玩家
   * @param removedSwords 移除的飞剑数量
   */
  public static void sendDeactivationToast(ServerPlayer player, int removedSwords) {
    player.displayClientMessage(Component.literal("§c§l蕴剑青莲·收"), true);
    if (removedSwords > 0) {
      player.displayClientMessage(Component.literal("§7" + removedSwords + "片莲瓣已归鞘"), false);
    }
  }

  /**
   * 发送资源耗尽提示
   *
   * @param player 玩家
   */
  public static void sendResourceDepletedToast(ServerPlayer player) {
    player.displayClientMessage(Component.literal("§c资源耗尽，青莲收拢"), true);
  }

  /**
   * 发送青莲护体触发提示
   *
   * @param player 玩家
   * @param cost 消耗的真元量
   */
  public static void sendShieldActivatedToast(ServerPlayer player, double cost) {
    player.displayClientMessage(Component.literal("§b§l青莲护体！"), true);
    player.displayClientMessage(
        Component.literal("§7消耗 " + String.format("%.1f", cost) + " 真元，完全格挡致命伤"), false);
  }

  /**
   * 发送青莲护体失效提示（真元不足）
   *
   * @param player 玩家
   */
  public static void sendShieldFailedToast(ServerPlayer player) {
    player.displayClientMessage(Component.literal("§c真元不足，青莲护体失效！"), false);
  }

  /**
   * 发送冷却提示（未实现多冷却系统，保留接口）
   *
   * @param player 玩家
   * @param abilityId 技能ID
   * @param readyAt 可用时间
   * @param now 当前时间
   */
  public static void scheduleCooldownToast(
      ServerPlayer player, ResourceLocation abilityId, long readyAt, long now) {
    long remaining = readyAt - now;
    double seconds = remaining / 20.0;
    player.displayClientMessage(
        Component.literal("§c技能冷却中，还需 " + String.format("%.1f", seconds) + " 秒"), true);
  }
}
