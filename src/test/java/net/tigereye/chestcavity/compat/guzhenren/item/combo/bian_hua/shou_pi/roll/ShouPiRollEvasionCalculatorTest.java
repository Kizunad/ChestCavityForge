package net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.shou_pi.roll;

import static org.junit.jupiter.api.Assertions.*;

import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.shou_pi.roll.calculator.ShouPiRollEvasionCalculator;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.shou_pi.roll.calculator.ShouPiRollEvasionCalculator.RollParameters;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.shou_pi.roll.tuning.ShouPiRollEvasionTuning;
import org.junit.jupiter.api.Test;

public class ShouPiRollEvasionCalculatorTest {

  @Test
  void compute_withSingleSynergy_usesBaseAndSingleBonuses() {
    RollParameters p = ShouPiRollEvasionCalculator.compute(1);
    assertEquals(
        ShouPiRollEvasionTuning.BASE_DISTANCE + ShouPiRollEvasionTuning.SYNERGY_DISTANCE_BONUS,
        p.distance(),
        1e-6);
    assertEquals(ShouPiRollEvasionTuning.RESISTANCE_DURATION_TICKS, p.resistanceDurationTicks());
    assertEquals(ShouPiRollEvasionTuning.BASE_RESISTANCE_AMPLIFIER, p.resistanceAmplifier());
    assertEquals(ShouPiRollEvasionTuning.BASE_SLOW_TICKS, p.slowDurationTicks());
    assertEquals(ShouPiRollEvasionTuning.BASE_SLOW_AMPLIFIER, p.slowAmplifier());
    assertEquals(ShouPiRollEvasionTuning.TARGET_SEARCH_RADIUS, p.slowRadius(), 1e-6);
    assertEquals(ShouPiRollEvasionTuning.MITIGATION_WINDOW_TICKS, p.mitigationWindowTicks());
    assertEquals(ShouPiRollEvasionTuning.COOLDOWN_TICKS, p.cooldown());
  }

  @Test
  void compute_withDualSynergy_appliesDualBonuses() {
    RollParameters p = ShouPiRollEvasionCalculator.compute(2);
    assertEquals(
        ShouPiRollEvasionTuning.BASE_DISTANCE
            + ShouPiRollEvasionTuning.SYNERGY_DISTANCE_BONUS
            + ShouPiRollEvasionTuning.DUAL_DISTANCE_BONUS,
        p.distance(),
        1e-6);
    assertEquals(ShouPiRollEvasionTuning.DUAL_RESISTANCE_AMPLIFIER, p.resistanceAmplifier());
    assertEquals(
        ShouPiRollEvasionTuning.BASE_SLOW_TICKS + ShouPiRollEvasionTuning.DUAL_SLOW_EXTRA_TICKS,
        p.slowDurationTicks());
    assertEquals(ShouPiRollEvasionTuning.DUAL_SLOW_AMPLIFIER, p.slowAmplifier());
  }

  @Test
  void compute_requiresAtLeastOneSynergy() {
    assertThrows(IllegalArgumentException.class, () -> ShouPiRollEvasionCalculator.compute(0));
  }
}

