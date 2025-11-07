package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.calculator.JianYingCalculator;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.tuning.JianYingTuning;
import org.junit.jupiter.api.Test;

public class JianYingCalculatorTest {

  @Test
  void passiveMultiplier_resetsAfterWindow() {
    long last = 0L;
    long now = JianYingTuning.PASSIVE_RESET_WINDOW_TICKS + 1L;
    float m = JianYingCalculator.passiveMultiplier(last, now, 0.21f);
    assertEquals(JianYingTuning.PASSIVE_INITIAL_MULTIPLIER, m, 1e-6);
  }

  @Test
  void passiveMultiplier_decaysWithinWindow() {
    long now = 100L;
    long last = now - 5L; // within reset window
    float prev = JianYingTuning.PASSIVE_INITIAL_MULTIPLIER;
    float m = JianYingCalculator.passiveMultiplier(last, now, prev);
    assertEquals(
        Math.max(JianYingTuning.PASSIVE_MIN_MULTIPLIER, prev - JianYingTuning.PASSIVE_DECAY_STEP),
        m,
        1e-6);
  }

  @Test
  void damage_calculations_scaleWithEfficiency() {
    double eff = 1.5; // +50%
    float clone = JianYingCalculator.cloneDamage(eff);
    float after = JianYingCalculator.afterimageDamage(eff);
    assertEquals(
        (float) (JianYingTuning.BASE_DAMAGE * JianYingTuning.CLONE_DAMAGE_RATIO * eff),
        clone,
        1e-6);
    assertEquals(
        (float) (JianYingTuning.BASE_DAMAGE * JianYingTuning.AFTERIMAGE_DAMAGE_RATIO * eff),
        after,
        1e-6);
  }
}

