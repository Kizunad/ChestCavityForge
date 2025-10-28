package net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.shou_pi.roll;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.behavior.ShouPiGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.shou_pi.roll.calculator.ShouPiRollEvasionCalculator;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.shou_pi.roll.calculator.ShouPiRollEvasionCalculator.RollParameters;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.shou_pi.roll.tuning.ShouPiRollEvasionTuning;
import org.junit.jupiter.api.Test;

final class ShouPiRollEvasionCalculatorTest {

  @Test
  void computeWithSingleSynergyUsesBaseBonuses() {
    RollParameters params = ShouPiRollEvasionCalculator.compute(1);
    assertEquals(
        ShouPiRollEvasionTuning.BASE_DISTANCE + ShouPiRollEvasionTuning.SYNERGY_DISTANCE_BONUS,
        params.distance(),
        1.0E-6);
    assertEquals(
        ShouPiRollEvasionTuning.RESISTANCE_DURATION_TICKS, params.resistanceDurationTicks());
    assertEquals(ShouPiRollEvasionTuning.BASE_RESISTANCE_AMPLIFIER, params.resistanceAmplifier());
    assertEquals(ShouPiRollEvasionTuning.BASE_SLOW_TICKS, params.slowDurationTicks());
    assertEquals(ShouPiRollEvasionTuning.BASE_SLOW_AMPLIFIER, params.slowAmplifier());
    assertEquals(
        (int) ShouPiGuOrganBehavior.ROLL_DAMAGE_WINDOW_TICKS, params.mitigationWindowTicks());
  }

  @Test
  void computeWithDualSynergyBoostsDistanceAndAmplifiers() {
    RollParameters params = ShouPiRollEvasionCalculator.compute(2);
    assertEquals(
        ShouPiRollEvasionTuning.BASE_DISTANCE
            + ShouPiRollEvasionTuning.SYNERGY_DISTANCE_BONUS
            + ShouPiRollEvasionTuning.DUAL_DISTANCE_BONUS,
        params.distance(),
        1.0E-6);
    assertEquals(
        ShouPiRollEvasionTuning.DUAL_RESISTANCE_AMPLIFIER, params.resistanceAmplifier());
    assertEquals(
        ShouPiRollEvasionTuning.BASE_SLOW_TICKS + ShouPiRollEvasionTuning.DUAL_SLOW_EXTRA_TICKS,
        params.slowDurationTicks());
    assertEquals(ShouPiRollEvasionTuning.DUAL_SLOW_AMPLIFIER, params.slowAmplifier());
  }

  @Test
  void computeWithoutSynergyThrows() {
    assertThrows(IllegalArgumentException.class, () -> ShouPiRollEvasionCalculator.compute(0));
  }
}

