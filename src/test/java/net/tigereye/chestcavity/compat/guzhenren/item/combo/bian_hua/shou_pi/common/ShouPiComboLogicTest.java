package net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.shou_pi.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ShouPiComboLogicTest {

  @Test
  void testApplyDaoHenBuff() {
    assertEquals(110.0, ShouPiComboLogic.applyDaoHenBuff(100.0, 10.0), 1e-6);
    assertEquals(100.0, ShouPiComboLogic.applyDaoHenBuff(100.0, 0.0), 1e-6);
    assertEquals(0.0, ShouPiComboLogic.applyDaoHenBuff(0.0, 10.0), 1e-6);
  }

  @Test
  void testComputeCooldown() {
    assertEquals(50, ShouPiComboLogic.computeCooldown(100, 100001));
    assertEquals(50, ShouPiComboLogic.computeCooldown(100, 10000), 1);
    assertEquals(100, ShouPiComboLogic.computeCooldown(100, 0));
    assertEquals(20, ShouPiComboLogic.computeCooldown(10, 0));
  }
}