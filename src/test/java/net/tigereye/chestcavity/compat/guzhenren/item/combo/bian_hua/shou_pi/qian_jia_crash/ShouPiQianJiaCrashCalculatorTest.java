package net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.shou_pi.qian_jia_crash;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import net.tigereye.chestcavity.compat.common.tuning.ShouPiGuTuning;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.shou_pi.qian_jia_crash.calculator.ShouPiQianJiaCrashCalculator;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.shou_pi.qian_jia_crash.calculator.ShouPiQianJiaCrashCalculator.CrashParameters;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.shou_pi.qian_jia_crash.tuning.ShouPiQianJiaCrashTuning;
import org.junit.jupiter.api.Test;

final class ShouPiQianJiaCrashCalculatorTest {

  @Test
  void computeRequiresSynergy() {
    assertThrows(
        IllegalArgumentException.class,
        () -> ShouPiQianJiaCrashCalculator.compute(10.0D, 5.0D, 0));
  }

  @Test
  void computeCapsDamageAgainstSoftPool() {
    CrashParameters params =
        ShouPiQianJiaCrashCalculator.compute(20.0D, 6.0D, 1);
    double ratio = ShouPiQianJiaCrashTuning.BASE_REFLECT_RATIO;
    double cap =
        ShouPiQianJiaCrashTuning.BASE_DAMAGE_CAP
            + 6.0D * ShouPiQianJiaCrashTuning.ATTACK_SCALE;
    double expected = Math.min(20.0D * ratio, cap);
    assertEquals(expected, params.damage(), 1.0E-6);
    assertEquals(ShouPiGuTuning.CRASH_SPLASH_RADIUS, params.radius(), 1.0E-6);
  }

  @Test
  void computeDualSynergyIncreasesDamageAndRadius() {
    CrashParameters params =
        ShouPiQianJiaCrashCalculator.compute(15.0D, 4.0D, 2);
    double ratio =
        ShouPiQianJiaCrashTuning.BASE_REFLECT_RATIO + ShouPiQianJiaCrashTuning.DUAL_REFLECT_BONUS;
    double cap =
        ShouPiQianJiaCrashTuning.BASE_DAMAGE_CAP
            + ShouPiQianJiaCrashTuning.DUAL_DAMAGE_CAP_BONUS
            + 4.0D * ShouPiQianJiaCrashTuning.ATTACK_SCALE;
    assertEquals(Math.min(15.0D * ratio, cap), params.damage(), 1.0E-6);
    assertEquals(
        ShouPiGuTuning.CRASH_SPLASH_RADIUS + ShouPiQianJiaCrashTuning.DUAL_RADIUS_BONUS,
        params.radius(),
        1.0E-6);
  }
}
