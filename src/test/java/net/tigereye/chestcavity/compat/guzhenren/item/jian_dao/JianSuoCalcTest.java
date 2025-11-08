package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.calculator.JianSuoCalc;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.tuning.JianSuoGuTuning;
import org.junit.jupiter.api.Test;

class JianSuoCalcTest {

  private static final double DELTA = 1.0E-6;

  @Test
  void dashDistanceScalesWithDaohenByHundredStep() {
    // 0 道痕 -> 基础距离
    assertEquals(JianSuoGuTuning.BASE_DASH_DISTANCE, JianSuoCalc.dashDistance(0.0), DELTA);

    // 99 道痕 -> 仍是基础距离（未达 100）
    assertEquals(JianSuoGuTuning.BASE_DASH_DISTANCE, JianSuoCalc.dashDistance(99.9), DELTA);

    // 100 道痕 -> 基础 * (1 + 0.25 * 1) = 5.0 * 1.25 = 6.25
    assertEquals(6.25, JianSuoCalc.dashDistance(100.0), DELTA);

    // 200 道痕 -> 基础 * (1 + 0.25 * 2) = 5.0 * 1.5 = 7.5
    assertEquals(7.5, JianSuoCalc.dashDistance(200.0), DELTA);

    // 500 道痕 -> 基础 * (1 + 0.25 * 5) = 5.0 * 2.25 = 11.25
    assertEquals(11.25, JianSuoCalc.dashDistance(500.0), DELTA);
  }

  @Test
  void dashDistanceRespectsCap() {
    // 高道痕应该触及上限
    double uncapped = JianSuoGuTuning.BASE_DASH_DISTANCE * (1.0 + JianSuoGuTuning.DASH_DIST_PER_100_DAOHEN * 100.0);
    assertTrue(uncapped > JianSuoGuTuning.MAX_DASH_DISTANCE, "Test assumption: uncapped exceeds max");

    assertEquals(JianSuoGuTuning.MAX_DASH_DISTANCE, JianSuoCalc.dashDistance(10000.0), DELTA);
  }

  @Test
  void pathDamageScalesWithDaohenAndVelocity() {
    // 0 道痕, 0 速度 -> 基础伤害
    assertEquals(JianSuoGuTuning.BASE_ATK, JianSuoCalc.pathDamage(0.0, 0.0), DELTA);

    // 100 道痕, 0 速度 -> BASE_ATK * (1 + 0.35 * 1) = 1.0 * 1.35 = 1.35
    assertEquals(1.35, JianSuoCalc.pathDamage(100.0, 0.0), DELTA);

    // 0 道痕, 10 速度 -> BASE_ATK * (1 + min(10/10, 0.25)) = 1.0 * 1.25 = 1.25
    assertEquals(1.25, JianSuoCalc.pathDamage(0.0, 10.0), DELTA);

    // 100 道痕, 10 速度 -> 1.0 * 1.35 * 1.25 = 1.6875
    assertEquals(1.6875, JianSuoCalc.pathDamage(100.0, 10.0), DELTA);
  }

  @Test
  void pathDamageVelocityScaleIsCapped() {
    // 速度超过 10 * VELOCITY_SCALE_MAX 时应该被限制
    double highVelocity = 100.0;
    double cappedScale = JianSuoGuTuning.VELOCITY_SCALE_MAX;
    double expected = JianSuoGuTuning.BASE_ATK * (1.0 + cappedScale);
    assertEquals(expected, JianSuoCalc.pathDamage(0.0, highVelocity), DELTA);
  }

  @Test
  void evadeChanceScalesWithDaohenByHundredStep() {
    // 0 道痕 -> 基础几率
    assertEquals(JianSuoGuTuning.EVADE_CHANCE_BASE, JianSuoCalc.evadeChance(0.0), DELTA);

    // 100 道痕 -> 0.10 + 0.06 * 1 = 0.16
    assertEquals(0.16, JianSuoCalc.evadeChance(100.0), DELTA);

    // 500 道痕 -> 0.10 + 0.06 * 5 = 0.40
    assertEquals(0.40, JianSuoCalc.evadeChance(500.0), DELTA);
  }

  @Test
  void evadeChanceRespectsCap() {
    // 高道痕应该触及上限
    assertEquals(JianSuoGuTuning.EVADE_CHANCE_MAX, JianSuoCalc.evadeChance(10000.0), DELTA);
  }

  @Test
  void evadeReduceScalesWithDaohenByHundredStep() {
    // 0 道痕 -> 最小减伤
    assertEquals(JianSuoGuTuning.EVADE_REDUCE_MIN, JianSuoCalc.evadeReduce(0.0), DELTA);

    // 100 道痕 -> 0.10 + 0.08 * 1 = 0.18
    assertEquals(0.18, JianSuoCalc.evadeReduce(100.0), DELTA);

    // 500 道痕 -> 0.10 + 0.08 * 5 = 0.50
    assertEquals(0.50, JianSuoCalc.evadeReduce(500.0), DELTA);
  }

  @Test
  void evadeReduceRespectsCap() {
    // 高道痕应该触及上限
    assertEquals(JianSuoGuTuning.EVADE_REDUCE_MAX, JianSuoCalc.evadeReduce(10000.0), DELTA);
  }

  @Test
  void activeCooldownDecreasesWithDaohen() {
    // 0 道痕 -> 最大冷却
    assertEquals(JianSuoGuTuning.ACTIVE_COOLDOWN_MAX_TICKS, JianSuoCalc.activeCooldown(0.0));

    // 500 道痕 -> 插值 (ratio = 5 / 10 = 0.5)
    // cd = 120 - 0.5 * (120 - 60) = 120 - 30 = 90
    assertEquals(90, JianSuoCalc.activeCooldown(500.0));

    // 1000 道痕 -> 最小冷却 (ratio = 10 / 10 = 1.0)
    assertEquals(JianSuoGuTuning.ACTIVE_COOLDOWN_MIN_TICKS, JianSuoCalc.activeCooldown(1000.0));

    // 更高道痕仍然是最小冷却
    assertEquals(JianSuoGuTuning.ACTIVE_COOLDOWN_MIN_TICKS, JianSuoCalc.activeCooldown(2000.0));
  }

  @Test
  void secondsToTicksConversion() {
    assertEquals(20, JianSuoCalc.secondsToTicks(1.0));
    assertEquals(60, JianSuoCalc.secondsToTicks(3.0));
    assertEquals(120, JianSuoCalc.secondsToTicks(6.0));
  }

  @Test
  void ticksToSecondsConversion() {
    assertEquals(1.0, JianSuoCalc.ticksToSeconds(20), DELTA);
    assertEquals(3.0, JianSuoCalc.ticksToSeconds(60), DELTA);
    assertEquals(6.0, JianSuoCalc.ticksToSeconds(120), DELTA);
  }

  @Test
  void negativeDaohenHandledGracefully() {
    // 负道痕应该回退到基础值/最小值
    assertTrue(JianSuoCalc.dashDistance(-100.0) >= 0.0);
    assertTrue(JianSuoCalc.evadeChance(-100.0) >= 0.0);
    assertTrue(JianSuoCalc.evadeReduce(-100.0) >= JianSuoGuTuning.EVADE_REDUCE_MIN);
  }
}
