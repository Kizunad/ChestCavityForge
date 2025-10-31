package net.tigereye.chestcavity.compat.guzhenren.util.behavior;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class ScalingTest {

  private final Scaling S = Scaling.DEFAULT; // Standard implementation

  @Test
  void testScaleByDaoBasic() {
    double v = S.scaleByDao(10.0, 2.0, 0.5, 0.0, 100.0);
    assertEquals(20.0, v, 1e-6);
  }

  @Test
  void testScaleByDaoClampMax() {
    double v = S.scaleByDao(10.0, 10.0, 1.0, 0.0, 25.0);
    assertEquals(25.0, v, 1e-6);
  }

  @Test
  void testCooldownBasic() {
    double cd = S.cooldown(10.0, 0.3, 1.0, 0.5);
    assertEquals(7.0, cd, 1e-6);
  }

  @Test
  void testCooldownCap() {
    double cd = S.cooldown(10.0, 0.8, 0.0, 0.5);
    assertEquals(5.0, cd, 1e-6);
  }

  @Test
  void testCooldownMin() {
    double cd = S.cooldown(1.0, 0.9, 0.5, 0.9);
    assertEquals(0.5, cd, 1e-6);
  }

  @Test
  void testWithDaoCdr() {
    double cd = S.withDaoCdr(20.0, 2.0, 0.1, 0.15, 1.0, 0.5);
    // totalCdr = 0.15 + 0.2 = 0.35 -> cd = 20 * 0.65 = 13
    assertEquals(13.0, cd, 1e-6);
  }

  @Test
  void testTicksConversions() {
    assertEquals(30L, S.secondsToTicks(1.5));
    assertEquals(2.25, S.ticksToSeconds(45L), 1e-6);
  }

  @Test
  void testCooldownTicks() {
    double ticks = S.cooldownTicks(200.0, 0.5, 40.0, 0.9); // 10s base -> 5s -> 100 ticks
    assertEquals(100.0, ticks, 1e-6);
  }
}

