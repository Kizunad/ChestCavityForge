package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.behavior.active;

import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.FlyingSwordController;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.FlyingSwordEntity;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.behavior.organ.JianLiaoGuState;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.calculator.JianLiaoGuCalc;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.fx.JianLiaoGuFx;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.tuning.JianLiaoGuTuning;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;

/**
 * 剑疗蛊主动技：剑血互济。
 */
public final class JianLiaoGuActive {

  private JianLiaoGuActive() {}

  /**
   * @return 是否成功激活
   */
  public static boolean activate(
      ServerPlayer player,
      ChestCavityInstance cc,
      ItemStack organ,
      OrganState state,
      MultiCooldown cooldown,
      long now,
      double swordScar) {
    MultiCooldown.Entry readyEntry =
        cooldown.entry(JianLiaoGuState.KEY_READY_TICK).withDefault(0L);
    if (now < readyEntry.getReadyTick()) {
      return false;
    }

    float maxHp = player.getMaxHealth();
    float currentHp = player.getHealth();
    float spendBase = Math.max(0f, JianLiaoGuTuning.ACTIVE_HP_SPEND_RATIO * maxHp);
    float allowed = Math.max(0f, currentHp - 1.0f);
    float hpSpend = Math.min(spendBase, allowed);
    if (hpSpend <= 0.001f) {
      return false;
    }

    List<FlyingSwordEntity> swords =
        FlyingSwordController.getPlayerSwords(player.serverLevel(), player);
    if (swords.isEmpty()) {
      return false;
    }

    // 激活特效：生命消耗
    JianLiaoGuFx.playActiveActivate(player, hpSpend);

    double repaired = 0.0;
    for (FlyingSwordEntity sword : swords) {
      double add = JianLiaoGuCalc.activeRepairAmount(sword, hpSpend, maxHp, swordScar);
      if (add <= 0.0) {
        continue;
      }
      double max = Math.max(1.0, sword.getSwordAttributes().maxDurability);
      float before = sword.getDurability();
      sword.setDurability((float) Math.min(max, before + add));
      if (sword.getDurability() > before) {
        repaired += sword.getDurability() - before;
        // 能量传导特效：玩家 → 飞剑
        JianLiaoGuFx.playActiveTransfer(player, sword);
        // 修复特效：飞剑恢复
        JianLiaoGuFx.playActiveRepair(sword, add);
      }
    }

    if (repaired <= 0.0) {
      return false;
    }

    player.setHealth(Math.max(1.0f, currentHp - hpSpend));
    readyEntry.setReadyAt(now + JianLiaoGuCalc.activeCooldownTicks(swordScar));

    // 完成音效
    JianLiaoGuFx.playActiveComplete(player, repaired);

    return true;
  }
}
