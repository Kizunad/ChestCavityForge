package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.calculator.JianQiaoGuCalc;
import org.junit.jupiter.api.Test;

class JianQiaoGuCalcTest {

  @Test
  void extraSlotsFromDaoHenUsesHundredStep() {
    assertEquals(0, JianQiaoGuCalc.extraSlotsFromDaoHen(0.0));
    assertEquals(0, JianQiaoGuCalc.extraSlotsFromDaoHen(99.9));
    assertEquals(1, JianQiaoGuCalc.extraSlotsFromDaoHen(100.0));
    assertEquals(2, JianQiaoGuCalc.extraSlotsFromDaoHen(250.0));
  }

  @Test
  void canOverwhelmRequiresDoubleAdvantage() {
    assertTrue(JianQiaoGuCalc.canOverwhelm(200.0, 100.0, 400.0, 100.0));
    assertFalse(JianQiaoGuCalc.canOverwhelm(199.0, 100.0, 400.0, 100.0));
    assertFalse(JianQiaoGuCalc.canOverwhelm(200.0, 100.0, 199.0, 100.0));
    assertTrue(JianQiaoGuCalc.canOverwhelm(10.0, 0.0, 5.0, 0.0));
    assertFalse(JianQiaoGuCalc.canOverwhelm(0.0, 0.0, 0.0, 0.0));
  }

  @Test
  void seizeChanceRespectsBounds() {
    assertEquals(0.05, JianQiaoGuCalc.seizeChance(0.0, 100.0), 1.0E-6);
    assertEquals(0.10, JianQiaoGuCalc.seizeChance(50.0, 0.0), 1.0E-6);
    assertEquals(0.75, JianQiaoGuCalc.seizeChance(2000.0, 100.0), 1.0E-6);
    assertEquals(0.05, JianQiaoGuCalc.seizeChance(-50.0, 100.0), 1.0E-6);
  }
}
