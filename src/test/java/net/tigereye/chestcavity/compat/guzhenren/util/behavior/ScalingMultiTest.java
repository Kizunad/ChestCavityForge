package net.tigereye.chestcavity.compat.guzhenren.util.behavior;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class ScalingMultiTest {
  private final Scaling S = Scaling.DEFAULT;

  @Test
  void testScaleByDaoMulti() {
    double[] dao = {1.0, 2.0, 0.5};
    double[] k = {0.2, 0.1, 0.4}; // sum = 1*0.2 + 2*0.1 + 0.5*0.4 = 0.2 + 0.2 + 0.2 = 0.6
    double v = S.scaleByDaoMulti(10.0, dao, k, 0.0, 100.0);
    assertEquals(16.0, v, 1e-6);
  }

  @Test
  void testScaleByDaoMultiClamp() {
    double[] dao = {10.0};
    double[] k = {1.0}; // factor = 1 + 10 = 11 -> 110
    double v = S.scaleByDaoMulti(10.0, dao, k, 0.0, 50.0);
    assertEquals(50.0, v, 1e-6);
  }

  @Test
  void testWithDaoCdrMulti() {
    double[] dao = {1.0, 2.0};
    double[] k = {0.1, 0.05}; // sum = 1*0.1 + 2*0.05 = 0.2
    double cd = S.withDaoCdrMulti(20.0, dao, k, 0.1, 1.0, 0.5); // totalCdr=0.3 -> 14.0s
    assertEquals(14.0, cd, 1e-6);
  }
}

