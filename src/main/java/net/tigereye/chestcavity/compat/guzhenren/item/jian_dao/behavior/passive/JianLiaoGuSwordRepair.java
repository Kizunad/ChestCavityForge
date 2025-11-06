package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.behavior.passive;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.FlyingSwordController;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.FlyingSwordEntity;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.behavior.organ.JianLiaoGuState;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.calculator.JianLiaoGuCalc;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.fx.JianLiaoGuFx;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.tuning.JianLiaoGuTuning;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;

/**
 * 剑疗蛊被动——飞剑耐久互补逻辑。
 */
public final class JianLiaoGuSwordRepair {

  private JianLiaoGuSwordRepair() {}

  public static void tick(ServerPlayer player, MultiCooldown cooldown, long now) {
    MultiCooldown.Entry repairEntry =
        cooldown.entry(JianLiaoGuState.KEY_NEXT_SWORD_REPAIR_TICK).withDefault(0L);
    if (now < repairEntry.getReadyTick()) {
      return;
    }

    ServerLevel level = player.serverLevel();
    List<FlyingSwordEntity> swords =
        new ArrayList<>(FlyingSwordController.getPlayerSwords(level, player));
    if (!swords.isEmpty()) {
      List<FlyingSwordEntity> low = new ArrayList<>();
      List<FlyingSwordEntity> donors = new ArrayList<>();
      for (FlyingSwordEntity sword : swords) {
        if (sword == null || sword.isRemoved()) {
          continue;
        }
        if (JianLiaoGuCalc.isLowDurability(sword)) {
          low.add(sword);
        } else {
          donors.add(sword);
        }
      }

      if (!low.isEmpty() && !donors.isEmpty()) {
        double pool = 0.0;
        for (FlyingSwordEntity donor : donors) {
          double cost = JianLiaoGuCalc.donorCost(donor);
          if (cost > 0.0) {
            donor.setDurability((float) Math.max(0.0, donor.getDurability() - cost));
            pool += JianLiaoGuCalc.donorNetFromCost(cost);
            // 捐献特效：健康飞剑贡献能量
            JianLiaoGuFx.playSwordDonate(donor, cost);
          }
        }

        if (pool > 0.0) {
          low.sort(Comparator.comparingDouble(FlyingSwordEntity::getDurability));
          double share = pool / low.size();
          boolean anyRepaired = false;
          for (FlyingSwordEntity target : low) {
            double cap = JianLiaoGuCalc.repairCapPerTarget(target);
            double add = Math.min(share, cap);
            if (add > 0.0) {
              double max = Math.max(1.0, target.getSwordAttributes().maxDurability);
              target.setDurability((float) Math.min(max, target.getDurability() + add));
              anyRepaired = true;
              // 能量传导特效：从捐献者到接收者（选择一个代表性的捐献者）
              if (!donors.isEmpty()) {
                JianLiaoGuFx.playSwordTransfer(donors.get(0), target);
              }
              // 接收修复特效：低耐久飞剑恢复
              JianLiaoGuFx.playSwordReceive(target, add);
            }
          }
          // 修复完成音效
          if (anyRepaired) {
            JianLiaoGuFx.playSwordRepairComplete(level, player);
          }
        }
      }
    }

    repairEntry.setReadyAt(now + Math.max(40L, JianLiaoGuTuning.SWORD_REPAIR_PERIOD_T));
  }
}
