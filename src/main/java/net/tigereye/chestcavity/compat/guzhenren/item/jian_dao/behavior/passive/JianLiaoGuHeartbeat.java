package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.behavior.passive;

import net.minecraft.server.level.ServerPlayer;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.behavior.organ.JianLiaoGuState;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.calculator.JianLiaoGuCalc;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.tuning.JianLiaoGuTuning;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.tigereye.chestcavity.util.ChestCavityUtil;

/**
 * 剑疗蛊被动——通用生物治疗逻辑。
 */
public final class JianLiaoGuHeartbeat {

  private JianLiaoGuHeartbeat() {}

  public static void tick(
      ServerPlayer player, MultiCooldown cooldown, long now, double swordScar) {
    MultiCooldown.Entry heartbeatEntry =
        cooldown.entry(JianLiaoGuState.KEY_NEXT_HEARTBEAT_TICK).withDefault(0L);
    if (now < heartbeatEntry.getReadyTick()) {
      return;
    }

    float heal = JianLiaoGuCalc.heartbeatHeal(player.getMaxHealth(), swordScar);
    if (heal > 0f && player.getHealth() < player.getMaxHealth()) {
      float amount = heal;
      ChestCavityUtil.runWithOrganHeal(() -> player.heal(amount));
    }

    heartbeatEntry.setReadyAt(now + Math.max(20L, JianLiaoGuTuning.HEARTBEAT_PERIOD_T));
  }
}
