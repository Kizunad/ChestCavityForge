package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.behavior.passive;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;

/**
 * 剑疗蛊被动效果：心跳治疗与飞剑互补修复。
 */
public final class JianLiaoGuPassive {

  private JianLiaoGuPassive() {}

  public static void tick(
      ServerPlayer player,
      ChestCavityInstance cc,
      ItemStack organ,
      OrganState state,
      MultiCooldown cooldown,
      long now,
      double swordScar) {
    JianLiaoGuHeartbeat.tick(player, cooldown, now, swordScar);
    JianLiaoGuSwordRepair.tick(player, cooldown, now);
  }
}
