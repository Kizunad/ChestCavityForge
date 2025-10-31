package net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.shou_pi.roll.calculator;

import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.shou_pi.roll.tuning.ShouPiRollEvasionTuning;

/** 皮走滚袭的纯逻辑计算器（安全默认实现，移除未实现依赖）。 */
public final class ShouPiRollEvasionCalculator {
  private ShouPiRollEvasionCalculator() {}

  public static RollParameters compute(int armorSynergyCount) {
    if (armorSynergyCount <= 0) {
      throw new IllegalArgumentException("roll evasion requires at least one synergy organ");
    }
    int cappedSynergy = Math.min(armorSynergyCount, 2);
    double distance = ShouPiRollEvasionTuning.BASE_DISTANCE
        + ShouPiRollEvasionTuning.SYNERGY_DISTANCE_BONUS
        + (cappedSynergy >= 2 ? ShouPiRollEvasionTuning.DUAL_DISTANCE_BONUS : 0.0);
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
    double slowRadius = ShouPiRollEvasionTuning.TARGET_SEARCH_RADIUS;
    long cooldown = ShouPiRollEvasionTuning.COOLDOWN_TICKS;
    return new RollParameters(
        distance,
        ShouPiRollEvasionTuning.RESISTANCE_DURATION_TICKS,
        resistanceAmplifier,
        slowTicks,
        slowAmplifier,
        slowRadius,
        ShouPiRollEvasionTuning.MITIGATION_WINDOW_TICKS,
        cooldown);
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
