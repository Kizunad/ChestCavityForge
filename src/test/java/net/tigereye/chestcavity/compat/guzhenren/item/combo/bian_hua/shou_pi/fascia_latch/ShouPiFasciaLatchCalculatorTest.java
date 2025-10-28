package net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.shou_pi.fascia_latch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
  void computeBaseValuesWithoutTieGuGu() {
    FasciaParameters params = ShouPiFasciaLatchCalculator.compute(5, true, false);
    assertEquals(ShouPiFasciaLatchTuning.BASE_SHIELD, params.shieldAmount(), 1.0E-6);
    assertTrue(params.grantTenacity());
    assertFalse(params.applyShockwave());
    assertEquals(
        ShouPiFasciaLatchTuning.TENACITY_DURATION_TICKS, params.tenacityDurationTicks());
  }

  @Test
  void computeWithTieGuGuAddsShieldAndShockwave() {
    FasciaParameters params = ShouPiFasciaLatchCalculator.compute(5, false, true);
    assertEquals(
        ShouPiFasciaLatchTuning.BASE_SHIELD + ShouPiFasciaLatchTuning.IRON_EXTRA_SHIELD,
        params.shieldAmount(),
        1.0E-6);
    assertTrue(params.applyShockwave());
    assertFalse(params.grantTenacity());
  }
}

