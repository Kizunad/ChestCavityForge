package net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.shou_pi.qian_jia_crash;

import static org.junit.jupiter.api.Assertions.*;

import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.shou_pi.qian_jia_crash.calculator.ShouPiQianJiaCrashCalculator;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.shou_pi.qian_jia_crash.calculator.ShouPiQianJiaCrashCalculator.CrashParameters;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.shou_pi.qian_jia_crash.tuning.ShouPiQianJiaCrashTuning;
import org.junit.jupiter.api.Test;

public class ShouPiQianJiaCrashCalculatorTest {

  @Test
  void compute_damageRespectsRatioAndCap_singleSynergy() {
    double softPool = 20.0;
    double attack = 10.0;
    CrashParameters p = ShouPiQianJiaCrashCalculator.compute(softPool, attack, 1);
    double expectedCap = ShouPiQianJiaCrashTuning.BASE_DAMAGE_CAP + attack * ShouPiQianJiaCrashTuning.ATTACK_SCALE;
    double expectedDamage = Math.min(softPool * ShouPiQianJiaCrashTuning.BASE_REFLECT_RATIO, expectedCap);
    assertEquals(expectedDamage, p.damage(), 1e-6);
    assertEquals(ShouPiQianJiaCrashTuning.BASE_SPLASH_RADIUS, p.radius(), 1e-6);
  }

  @Test
  void compute_dualSynergy_appliesDualBonuses() {
    double softPool = 50.0;
    double attack = 15.0;
    CrashParameters p = ShouPiQianJiaCrashCalculator.compute(softPool, attack, 2);
    double ratio = ShouPiQianJiaCrashTuning.BASE_REFLECT_RATIO + ShouPiQianJiaCrashTuning.DUAL_REFLECT_BONUS;
    double cap = ShouPiQianJiaCrashTuning.BASE_DAMAGE_CAP
        + ShouPiQianJiaCrashTuning.DUAL_DAMAGE_CAP_BONUS
        + attack * ShouPiQianJiaCrashTuning.ATTACK_SCALE;
    assertEquals(Math.min(softPool * ratio, cap), p.damage(), 1e-6);
    assertEquals(
        ShouPiQianJiaCrashTuning.BASE_SPLASH_RADIUS + ShouPiQianJiaCrashTuning.DUAL_RADIUS_BONUS,
        p.radius(),
        1e-6);
  }

  @Test
  void compute_requiresAtLeastOneSynergy() {
    assertThrows(IllegalArgumentException.class, () -> ShouPiQianJiaCrashCalculator.compute(10, 5, 0));
  }
}

