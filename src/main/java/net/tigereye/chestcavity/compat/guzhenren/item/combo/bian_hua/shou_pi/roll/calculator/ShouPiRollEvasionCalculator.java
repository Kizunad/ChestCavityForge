package net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.shou_pi.roll.calculator;

import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.behavior.ShouPiGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.shou_pi.common.ShouPiComboLogic;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.shou_pi.common.ShouPiComboLogic.BianHuaDaoSnapshot;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.shou_pi.roll.tuning.ShouPiRollEvasionTuning;

/** 皮走滚袭的纯逻辑计算器。 */
public final class ShouPiRollEvasionCalculator {
  private ShouPiRollEvasionCalculator() {}

  public static RollParameters compute(int armorSynergyCount, BianHuaDaoSnapshot snapshot) {
    if (armorSynergyCount <= 0) {
      throw new IllegalArgumentException("roll evasion requires at least one synergy organ");
    }
    int cappedSynergy = Math.min(armorSynergyCount, 2);
    double distance =
        ShouPiComboLogic.applyDaoHenBuff(
                ShouPiRollEvasionTuning.BASE_DISTANCE, snapshot.daoHen())
            + ShouPiComboLogic.applyDaoHenBuff(
                ShouPiRollEvasionTuning.SYNERGY_DISTANCE_BONUS, snapshot.daoHen());
    if (cappedSynergy >= 2) {
      distance += ShouPiComboLogic.applyDaoHenBuff(
          ShouPiRollEvasionTuning.DUAL_DISTANCE_BONUS, snapshot.daoHen());
    }
    int resistanceAmplifier =
        cappedSynergy >= 2
            ? ShouPiRollEvasionTuning.DUAL_RESISTANCE_AMPLIFIER
            : ShouPiRollEvasionTuning.BASE_RESISTANCE_AMPLIFIER;
    int slowTicks =
        ShouPiRollEvasionTuning.BASE_SLOW_TICKS
            + (cappedSynergy >= 2 ? ShouPiRollEvasionTuning.DUAL_SLOW_EXTRA_TICKS : 0);
    int slowAmplifier =
        cappedSynergy >= 2
            ? ShouPiRollEvasionTuning.DUAL_SLOW_AMPLIFIER
            : ShouPiRollEvasionTuning.BASE_SLOW_AMPLIFIER;
    return new RollParameters(
        distance,
        ShouPiRollEvasionTuning.RESISTANCE_DURATION_TICKS,
        resistanceAmplifier,
        slowTicks,
        slowAmplifier,
        ShouPiComboLogic.applyDaoHenBuff(
            ShouPiRollEvasionTuning.TARGET_SEARCH_RADIUS, snapshot.daoHen()),
        (int) ShouPiGuOrganBehavior.ROLL_DAMAGE_WINDOW_TICKS,
        ShouPiComboLogic.computeCooldown(
            ShouPiRollEvasionTuning.COOLDOWN_TICKS, snapshot.flowExperience()));
  }

  /** 皮走滚袭输出参数。 */
  public record RollParameters(
      double distance,
      int resistanceDurationTicks,
      int resistanceAmplifier,
      int slowDurationTicks,
      int slowAmplifier,
      double slowRadius,
      int mitigationWindowTicks,
      long cooldown) {}
}

