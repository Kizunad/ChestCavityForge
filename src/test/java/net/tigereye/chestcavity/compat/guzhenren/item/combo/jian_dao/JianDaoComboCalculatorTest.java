package net.tigereye.chestcavity.compat.guzhenren.item.combo.jian_dao;

import static org.junit.jupiter.api.Assertions.*;

import net.tigereye.chestcavity.compat.guzhenren.item.combo.jian_dao.calculator.JianDaoComboCalculator;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.jian_dao.tuning.JianDaoComboTuning;
import org.junit.jupiter.api.Test;

/**
 * JianDaoComboCalculator 纯函数单测（框架占位）。
 */
public class JianDaoComboCalculatorTest {

  @Test
  void startWindowSetsEndTick() {
    long now = 1000L;
    var state = JianDaoComboCalculator.startWindow(now);
    assertEquals(now + JianDaoComboTuning.COMBO_WINDOW_TICKS, state.windowEndTick());
    assertEquals(0, state.hits());
  }

  @Test
  void registerHitIncrementsWithinWindowAndCapsAtMax() {
    long now = 2000L;
    var st = JianDaoComboCalculator.startWindow(now);
    // 三次命中后封顶
    st = JianDaoComboCalculator.registerHit(now + 1, st);
    st = JianDaoComboCalculator.registerHit(now + 2, st);
    st = JianDaoComboCalculator.registerHit(now + 3, st);
    st = JianDaoComboCalculator.registerHit(now + 4, st);
    assertEquals(JianDaoComboTuning.MAX_HITS, st.hits());
  }

  @Test
  void computeMultiplierGrowsPerHit() {
    assertEquals(1.0D, JianDaoComboCalculator.computeMultiplier(0), 1.0E-6);
    assertEquals(1.0D, JianDaoComboCalculator.computeMultiplier(1), 1.0E-6);
    assertEquals(1.2D, JianDaoComboCalculator.computeMultiplier(2), 1.0E-6);
    assertEquals(1.4D, JianDaoComboCalculator.computeMultiplier(3), 1.0E-6);
    // 超过上限按上限计算
    assertEquals(1.4D, JianDaoComboCalculator.computeMultiplier(10), 1.0E-6);
  }
}

