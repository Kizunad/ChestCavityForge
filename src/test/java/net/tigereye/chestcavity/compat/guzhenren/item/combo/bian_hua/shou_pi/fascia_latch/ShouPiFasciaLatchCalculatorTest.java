package net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.shou_pi.fascia_latch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import net.tigereye.chestcavity.compat.common.tuning.ShouPiGuTuning;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.shou_pi.fascia_latch.calculator.ShouPiFasciaLatchCalculator;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.shou_pi.fascia_latch.calculator.ShouPiFasciaLatchCalculator.FasciaParameters;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.shou_pi.fascia_latch.tuning.ShouPiFasciaLatchTuning;
import org.junit.jupiter.api.Test;

final class ShouPiFasciaLatchCalculatorTest {

  @Test
  void computeRequiresTriggerCount() {
    assertThrows(
        IllegalArgumentException.class,
        () -> ShouPiFasciaLatchCalculator.compute(3, true, false));
  }

  @Test
  void computeWithTigerSynergyGrantsTenacity() {
    FasciaParameters params =
        ShouPiFasciaLatchCalculator.compute(5, true, false);
    assertEquals(
        ShouPiGuTuning.FASCIA_ACTIVE_REDUCTION, params.damageReduction(), 1.0E-6);
    assertEquals(
        ShouPiFasciaLatchTuning.TENACITY_KNOCKBACK_RESIST,
        params.tenacityKnockbackResist(),
        1.0E-6);
  }

  @Test
  void computeWithIronSynergyAddsShockwaveAndShield() {
    FasciaParameters params =
        ShouPiFasciaLatchCalculator.compute(5, false, true);
    assertEquals(ShouPiFasciaLatchTuning.BASE_SHIELD + ShouPiFasciaLatchTuning.IRON_EXTRA_SHIELD, params.shieldAmount(), 1.0E-6);
    assertEquals(
        ShouPiFasciaLatchTuning.SHOCKWAVE_RADIUS, params.shockwaveRadius(), 1.0E-6);
    assertEquals(
        ShouPiFasciaLatchTuning.SHOCKWAVE_STRENGTH, params.shockwaveStrength(), 1.0E-6);
  }
}
