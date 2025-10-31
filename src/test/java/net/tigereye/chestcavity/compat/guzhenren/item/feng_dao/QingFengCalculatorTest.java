package net.tigereye.chestcavity.compat.guzhenren.item.feng_dao;

import static org.junit.jupiter.api.Assertions.*;

import net.tigereye.chestcavity.compat.guzhenren.item.feng_dao.calculator.QingFengCalculator;
import net.tigereye.chestcavity.compat.guzhenren.item.feng_dao.calculator.QingFengCalculator.WindStackUpdate;
import org.junit.jupiter.api.Test;

public class QingFengCalculatorTest {

  @Test
  void dodgeChance_scalesWithStageAndStacks() {
    // Stage 2: no base
    assertEquals(0.0, QingFengCalculator.dodgeChance(2, true, 0), 1e-9);
    // Stage 3 moving adds base
    double s3 = QingFengCalculator.dodgeChance(3, true, 0);
    assertTrue(s3 > 0.0);
    // Stage 5 gets stack scaling
    double s5 = QingFengCalculator.dodgeChance(5, true, 5);
    assertTrue(s5 > s3);
    assertTrue(s5 <= 1.0);
  }

  @Test
  void updateStacks_increaseOnMove_resetOnIdle() {
    long now = 1000L;
    WindStackUpdate u1 = QingFengCalculator.updateStacks(0, true, now, 0);
    assertEquals(1, u1.stacks());
    assertEquals(now, u1.lastMoveTick());
    assertTrue(u1.dirty());

    WindStackUpdate u2 =
        QingFengCalculator.updateStacks(u1.stacks(), false, now + 39, u1.lastMoveTick());
    assertEquals(u1.stacks(), u2.stacks());
    assertFalse(u2.reset());

    WindStackUpdate u3 =
        QingFengCalculator.updateStacks(u1.stacks(), false, now + 41, u1.lastMoveTick());
    assertEquals(0, u3.stacks());
    assertTrue(u3.reset());
  }

  @Test
  void runDistance_centimetersAndMilestone() {
    assertEquals(0, QingFengCalculator.deltaCentimeters(0.05));
    long cm = QingFengCalculator.deltaCentimeters(2.34);
    assertTrue(cm >= 200);
    long before = 9_900L, after = 10_100L;
    assertTrue(QingFengCalculator.hitRunFxMilestone(before, after));
  }

  @Test
  void glideClamp_appliesMaxFallSpeed() {
    assertEquals(QingFengCalculator.clampFallSpeed(-1.0), -0.25, 1e-6);
    assertEquals(QingFengCalculator.clampFallSpeed(-0.1), -0.1, 1e-6);
  }
}

