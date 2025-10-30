package net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.shou_pi.qian_jia_crash.calculator;

import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.behavior.ShouPiGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.shou_pi.common.ShouPiComboLogic;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.shou_pi.common.ShouPiComboLogic.BianHuaDaoSnapshot;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.shou_pi.qian_jia_crash.tuning.ShouPiQianJiaCrashTuning;

/** 嵌甲冲撞的纯逻辑计算器。 */
public final class ShouPiQianJiaCrashCalculator {
  private ShouPiQianJiaCrashCalculator() {}

  public static CrashParameters compute(
      double softPool,
      double attackDamage,
      int armorSynergyCount,
      BianHuaDaoSnapshot snapshot) {
    if (armorSynergyCount <= 0) {
      throw new IllegalArgumentException("crash combo requires at least one synergy organ");
    }
    int cappedSynergy = Math.min(armorSynergyCount, 2);
    double ratio = ShouPiComboLogic.applyDaoHenBuff(
        ShouPiQianJiaCrashTuning.BASE_REFLECT_RATIO, snapshot.daoHen());
    if (cappedSynergy >= 2) {
      ratio += ShouPiComboLogic.applyDaoHenBuff(
          ShouPiQianJiaCrashTuning.DUAL_REFLECT_BONUS, snapshot.daoHen());
    }
    double damage = Math.max(0.0D, softPool * ratio);
    double cap =
        ShouPiComboLogic.applyDaoHenBuff(
                ShouPiQianJiaCrashTuning.BASE_DAMAGE_CAP, snapshot.daoHen())
            + attackDamage * ShouPiComboLogic.applyDaoHenBuff(
                ShouPiQianJiaCrashTuning.ATTACK_SCALE, snapshot.daoHen());
    if (cappedSynergy >= 2) {
      cap += ShouPiComboLogic.applyDaoHenBuff(
          ShouPiQianJiaCrashTuning.DUAL_DAMAGE_CAP_BONUS, snapshot.daoHen());
    }
    damage = Math.min(damage, cap);
    double radius = ShouPiGuOrganBehavior.CRASH_SPLASH_RADIUS;
    if (cappedSynergy >= 2) {
      radius += ShouPiComboLogic.applyDaoHenBuff(
          ShouPiQianJiaCrashTuning.DUAL_RADIUS_BONUS, snapshot.daoHen());
    }
    return new CrashParameters(
        damage,
        radius,
        ShouPiComboLogic.computeCooldown(
            ShouPiQianJiaCrashTuning.COOLDOWN_TICKS, snapshot.flowExperience()));
  }

  /** 嵌甲冲撞输出参数。 */
  public record CrashParameters(double damage, double radius, long cooldown) {}
}

