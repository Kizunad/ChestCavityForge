package net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.shou_pi.stoic_release;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.tuning.ShouPiGuTuning;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.shou_pi.stoic_release.calculator.ShouPiStoicReleaseCalculator;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.shou_pi.stoic_release.calculator.ShouPiStoicReleaseCalculator.StoicParameters;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.shou_pi.stoic_release.tuning.ShouPiStoicReleaseTuning;
import org.junit.jupiter.api.Test;

final class ShouPiStoicReleaseCalculatorTest {

  @Test
  void computeMatchesTierParameters() {
    var tierParams = ShouPiGuTuning.TIER3;
    StoicParameters params =
        ShouPiStoicReleaseCalculator.compute(tierParams);
    assertEquals(tierParams.stoicMitigation(), params.mitigationFraction(), 1.0E-6);
    assertEquals(tierParams.stoicShield(), params.shieldAmount(), 1.0E-6);
    assertEquals(tierParams.lockTicks(), params.lockTicks());
    assertEquals(ShouPiStoicReleaseTuning.ACTIVE_DURATION_TICKS, params.activeDurationTicks());
    assertFalse(params.applySlowAura());
    assertEquals(ShouPiStoicReleaseTuning.SOFT_REFLECT_BONUS, params.softReflectBonus(), 1.0E-6);
  }

  @Test
  void computeMarksStageFiveForSlowAura() {
    var tierParams = ShouPiGuTuning.TIER5;
    StoicParameters params =
        ShouPiStoicReleaseCalculator.compute(tierParams);
    assertTrue(params.applySlowAura());
  }
}
