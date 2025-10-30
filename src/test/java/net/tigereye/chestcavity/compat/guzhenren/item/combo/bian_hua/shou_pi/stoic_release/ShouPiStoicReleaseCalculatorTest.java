package net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.shou_pi.stoic_release;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.behavior.ShouPiGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.shou_pi.common.ShouPiComboLogic.BianHuaDaoSnapshot;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.shou_pi.stoic_release.calculator.ShouPiStoicReleaseCalculator;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.shou_pi.stoic_release.calculator.ShouPiStoicReleaseCalculator.StoicParameters;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.shou_pi.stoic_release.tuning.ShouPiStoicReleaseTuning;
import org.junit.jupiter.api.Test;

final class ShouPiStoicReleaseCalculatorTest {

  @Test
  void computeMatchesTierParameters() {
    var tierParams =
        new ShouPiGuOrganBehavior.TierParameters(
            ShouPiGuOrganBehavior.Tier.STAGE3,
            0.35D,
            16L,
            0.18D,
            4,
            80.0D,
            0.1D,
            160L);
    StoicParameters params =
        ShouPiStoicReleaseCalculator.compute(tierParams, new BianHuaDaoSnapshot(0, 0));
    assertEquals(0.18D, params.mitigationFraction(), 1.0E-6);
    assertEquals(4.0D, params.shieldAmount(), 1.0E-6);
    assertEquals(160L, params.lockTicks());
    assertEquals(ShouPiStoicReleaseTuning.ACTIVE_DURATION_TICKS, params.activeDurationTicks());
    assertFalse(params.applySlowAura());
    assertEquals(ShouPiStoicReleaseTuning.SOFT_REFLECT_BONUS, params.softReflectBonus(), 1.0E-6);
  }

  @Test
  void computeMarksStageFiveForSlowAura() {
    var tierParams =
        new ShouPiGuOrganBehavior.TierParameters(
            ShouPiGuOrganBehavior.Tier.STAGE5,
            0.45D,
            14L,
            0.22D,
            5,
            200.0D,
            0.45D,
            200L);
    StoicParameters params =
        ShouPiStoicReleaseCalculator.compute(tierParams, new BianHuaDaoSnapshot(0, 0));
    assertTrue(params.applySlowAura());
  }
}

