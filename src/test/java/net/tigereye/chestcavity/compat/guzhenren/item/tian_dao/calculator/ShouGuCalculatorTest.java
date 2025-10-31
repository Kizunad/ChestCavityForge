package net.tigereye.chestcavity.compat.guzhenren.item.tian_dao.calculator;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class ShouGuCalculatorTest {

  @Test
  void interest_rate_reduces_with_marks_and_doubles_under_jinliao() {
    double base = 0.05;
    double perMark = 0.02;
    int marks = 3;
    double normal = ShouGuCalculator.interestRate(base, perMark, marks, false);
    double withJinliao = ShouGuCalculator.interestRate(base, perMark, marks, true);
    assertEquals(Math.max(0.0, base - perMark * marks), normal, 1e-9);
    assertEquals(normal * 2.0, withJinliao, 1e-9);
  }

  @Test
  void apply_interest_adds_percentage() {
    double withInterest = ShouGuCalculator.applyInterest(100.0, 0.05);
    assertEquals(105.0, withInterest, 1e-9);
  }

  @Test
  void compute_threshold_scales_with_marks() {
    double t = ShouGuCalculator.computeDebtThreshold(100, 15, 3);
    assertEquals(145.0, t, 1e-9);
  }

  @Test
  void heal_per_tick_scales_with_consumed_marks() {
    double h = ShouGuCalculator.healPerTick(4.0, 4.0, 3);
    assertEquals(16.0, h, 1e-9);
  }

  @Test
  void next_heal_tick_is_now_plus_interval() {
    long next = ShouGuCalculator.nextHealTick(100L, 20L);
    assertEquals(120L, next);
  }

  @Test
  void environment_reduction_factor_clamped() {
    assertEquals(1.0, ShouGuCalculator.environmentReductionFactor(0.02, 0), 1e-9);
    assertEquals(0.9, ShouGuCalculator.environmentReductionFactor(0.1, 1), 1e-9);
    assertEquals(0.0, ShouGuCalculator.environmentReductionFactor(1.0, 2), 1e-9);
  }

  @Test
  void repay_clamped_to_debt_and_affected_by_jinliao() {
    double repayNormal =
        ShouGuCalculator.computeRepay(12.0, 3.0, 2, false, 0.5, /*debt*/ 10.0);
    // base + perMark*marks = 12 + 3*2 = 18 -> clamp to debt 10
    assertEquals(10.0, repayNormal, 1e-9);

    double repayJinliao =
        ShouGuCalculator.computeRepay(12.0, 3.0, 2, true, 0.5, /*debt*/ 100.0);
    // 18 * 0.5 = 9
    assertEquals(9.0, repayJinliao, 1e-9);
  }

  @Test
  void next_mark_tick_picks_interval_by_combat() {
    long a = ShouGuCalculator.nextMarkTick(100L, true, 5L, 10L);
    long b = ShouGuCalculator.nextMarkTick(100L, false, 5L, 10L);
    assertEquals(105L, a);
    assertEquals(110L, b);
  }
}
