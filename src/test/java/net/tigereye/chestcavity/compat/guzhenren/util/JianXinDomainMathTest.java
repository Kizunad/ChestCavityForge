package net.tigereye.chestcavity.compat.guzhenren.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.tigereye.chestcavity.compat.guzhenren.domain.impl.jianxin.tuning.JianXinDomainMath;
import org.junit.jupiter.api.Test;

public class JianXinDomainMathTest {

  private static final double EPS = 1e-6;

  @Test
  void testRmaxBounds() {
    assertEquals(5, JianXinDomainMath.computeRmax(0, 0));
    assertEquals(11, JianXinDomainMath.computeRmax(10_000, 100));
  }

  @Test
  void testCoefficientsTypicalRmax10() {
    double Rmax = 10.0;

    // R=1.0
    double s1 = JianXinDomainMath.computeS(1.0, Rmax);
    double pOut1 = JianXinDomainMath.computePout(s1);
    double pIn1 = JianXinDomainMath.computePin(s1);
    double e1 = JianXinDomainMath.computeEntityGate(s1);
    double pOutEnt1 = JianXinDomainMath.computePoutEntity(pOut1, e1);
    assertEquals(1.0, s1, EPS);
    assertEquals(5.0, pOut1, 1e-2);
    assertTrue(pIn1 <= 0.40 + 1e-6); // 实伤系数≈0.40
    assertEquals(0.8, e1, 1e-6); // s-0.2=0.8；实体是否启用由上层逻辑决定
    assertEquals(1.8, pOutEnt1, 1e-6); // 受上限1.8钳制

    // R=2.0
    double s2 = JianXinDomainMath.computeS(2.0, Rmax);
    double pOut2 = JianXinDomainMath.computePout(s2);
    double e2 = JianXinDomainMath.computeEntityGate(s2);
    double pOutEnt2 = JianXinDomainMath.computePoutEntity(pOut2, e2);
    assertEquals(1.0, s2, 1e-6);
    assertEquals(5.0, pOut2, 1e-2);
    assertEquals(Math.min(1.8, 1.0 + (pOut2 - 1.0) * e2), pOutEnt2, 1e-6);
    assertTrue(pOutEnt2 <= 1.8 + 1e-6);

    // R=4.0
    double s4 = JianXinDomainMath.computeS(4.0, Rmax);
    double pOut4 = JianXinDomainMath.computePout(s4);
    assertTrue(pOut4 > 3.5 && pOut4 < 3.8);

    // R=7.0
    double s7 = JianXinDomainMath.computeS(7.0, Rmax);
    double pOut7 = JianXinDomainMath.computePout(s7);
    assertTrue(pOut7 > 1.7 && pOut7 < 1.9);

    // R=10.0（接近下限）
    double s10 = JianXinDomainMath.computeS(10.0, Rmax);
    double pOut10 = JianXinDomainMath.computePout(s10);
    assertTrue(pOut10 >= 0.3 && pOut10 <= 0.5);
  }

  @Test
  void testDrainFormulas() {
    double R = 4.0;
    double s = 0.67; // 代表性
    double pass2s = JianXinDomainMath.computePassiveDrainPer2s(R, s);
    double act = JianXinDomainMath.computeActiveDrainPerSec(R, s);
    assertTrue(pass2s > 0.0);
    assertTrue(act > 0.0);
  }

  @Test
  void testActiveTimeAndCooldown() {
    assertEquals(8.0, JianXinDomainMath.computeActiveDurationSec(0), 1e-6);
    assertEquals(14.0, JianXinDomainMath.computeActiveDurationSec(20), 1e-6);
    assertEquals(25.0, JianXinDomainMath.computeActiveCooldownSec(0), 1e-6);
    assertEquals(16.0, JianXinDomainMath.computeActiveCooldownSec(20), 1e-6);
  }

  @Test
  void testSmallDomainBias() {
    double biased = JianXinDomainMath.applySmallDomainBias(2.0, 2.0, 5);
    assertEquals(2.0 * (1.0 + 0.03 * 5), biased, 1e-6);
    // 上限15%
    double capped = JianXinDomainMath.applySmallDomainBias(2.0, 1.0, 100);
    assertEquals(2.0 * 1.15, capped, 1e-6);
  }
}
