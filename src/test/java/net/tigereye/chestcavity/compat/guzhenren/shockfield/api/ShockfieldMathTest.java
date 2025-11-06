package net.tigereye.chestcavity.compat.guzhenren.shockfield.api;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** 剑荡蛊数学函数单元测试。 */
class ShockfieldMathTest {

  private static final double EPSILON = 1e-6;

  @Test
  void testApplyDamping() {
    double initialAmplitude = 1.0;
    double deltaSeconds = 1.0;
    double expected = Math.exp(-ShockfieldMath.DAMPING_PER_SEC);
    double actual = ShockfieldMath.applyDamping(initialAmplitude, deltaSeconds);
    assertEquals(expected, actual, EPSILON, "振幅衰减计算错误");
  }

  @Test
  void testStretchPeriod() {
    double initialPeriod = 1.0;
    double deltaSeconds = 1.0;
    double expected = 1.0 * (1.0 + ShockfieldMath.PERIOD_STRETCH_PER_SEC);
    double actual = ShockfieldMath.stretchPeriod(initialPeriod, deltaSeconds);
    assertEquals(expected, actual, EPSILON, "周期拉伸计算错误");
  }

  @Test
  void testPhaseMultiplier_Constructive() {
    double phaseDiff = Math.toRadians(30.0); // 30度，同相
    double expected = ShockfieldMath.CONSTRUCT_MULT;
    double actual = ShockfieldMath.phaseMultiplier(phaseDiff);
    assertEquals(expected, actual, EPSILON, "同相倍率错误");
  }

  @Test
  void testPhaseMultiplier_Normal() {
    double phaseDiff = Math.toRadians(90.0); // 90度，正常
    double expected = 1.0;
    double actual = ShockfieldMath.phaseMultiplier(phaseDiff);
    assertEquals(expected, actual, EPSILON, "正常倍率错误");
  }

  @Test
  void testPhaseMultiplier_Destructive() {
    double phaseDiff = Math.toRadians(150.0); // 150度，反相
    double expected = ShockfieldMath.DESTRUCT_MULT;
    double actual = ShockfieldMath.phaseMultiplier(phaseDiff);
    assertEquals(expected, actual, EPSILON, "反相倍率错误");
  }

  @Test
  void testComputeCoreDamage() {
    double amplitude = 0.10;
    double phaseMult = 1.0;
    double jd = 100.0;
    double str = 50.0;
    double flow = 200.0;
    double tier = 3.0;

    double expected =
        amplitude
            * phaseMult
            * (ShockfieldMath.BASE_DMG
                + jd * ShockfieldMath.K_JD
                + str * ShockfieldMath.K_STR
                + flow * ShockfieldMath.K_FLOW
                + tier * ShockfieldMath.K_TIER);

    double actual = ShockfieldMath.computeCoreDamage(amplitude, phaseMult, jd, str, flow, tier);
    assertEquals(expected, actual, EPSILON, "核心伤害计算错误");
  }

  @Test
  void testComputeFinalDamage_WithResist() {
    double coreDamage = 10.0;
    double resist = 0.40; // 40%减伤
    double armor = 0.0;

    double expected = 10.0 * (1.0 - 0.40); // = 6.0
    double actual = ShockfieldMath.computeFinalDamage(coreDamage, resist, armor);
    assertEquals(expected, actual, EPSILON, "百分比减伤计算错误");
  }

  @Test
  void testComputeFinalDamage_WithArmor() {
    double coreDamage = 10.0;
    double resist = 0.0;
    double armor = 50.0; // 50点护甲

    double expected = 10.0 - 50.0 * ShockfieldMath.ARMOR_FLAT; // = 10.0 - 2.0 = 8.0
    double actual = ShockfieldMath.computeFinalDamage(coreDamage, resist, armor);
    assertEquals(expected, actual, EPSILON, "护甲减伤计算错误");
  }

  @Test
  void testComputeFinalDamage_Floor() {
    double coreDamage = 0.1;
    double resist = 0.99; // 99%减伤
    double armor = 100.0;

    double actual = ShockfieldMath.computeFinalDamage(coreDamage, resist, armor);
    assertEquals(
        ShockfieldMath.DMG_FLOOR, actual, EPSILON, "伤害地板未生效");
  }

  @Test
  void testComputeFinalDamage_ResistCap() {
    double coreDamage = 10.0;
    double resist = 0.99; // 99%减伤，应被限制到60%
    double armor = 0.0;

    double expected = 10.0 * (1.0 - ShockfieldMath.RESIST_PCT_CAP); // = 10.0 * 0.4 = 4.0
    double actual = ShockfieldMath.computeFinalDamage(coreDamage, resist, armor);
    assertEquals(expected, actual, EPSILON, "减伤上限未生效");
  }

  @Test
  void testApplySoftCap_BelowCap() {
    double rawDps = 20.0;
    double jd = 0.0;
    double cap = ShockfieldMath.DPS_CAP_BASE; // = 30.0

    assertTrue(rawDps < cap, "测试用例设置错误：rawDps应小于cap");
    double actual = ShockfieldMath.applySoftCap(rawDps, jd);
    assertEquals(rawDps, actual, EPSILON, "低于封顶时DPS应不变");
  }

  @Test
  void testApplySoftCap_AboveCap() {
    double rawDps = 50.0;
    double jd = 0.0;
    double cap = ShockfieldMath.DPS_CAP_BASE; // = 30.0

    double excess = rawDps - cap; // = 20.0
    double expected = cap + excess * 0.5; // = 30.0 + 10.0 = 40.0
    double actual = ShockfieldMath.applySoftCap(rawDps, jd);
    assertEquals(expected, actual, EPSILON, "超过封顶时DPS软封顶计算错误");
  }

  @Test
  void testApplySoftCap_WithJD() {
    double rawDps = 50.0;
    double jd = 500.0;
    double cap = ShockfieldMath.DPS_CAP_BASE * (1.0 + jd / 500.0); // = 30.0 * 2.0 = 60.0

    assertTrue(rawDps < cap, "测试用例设置错误：rawDps应小于动态cap");
    double actual = ShockfieldMath.applySoftCap(rawDps, jd);
    assertEquals(rawDps, actual, EPSILON, "有JD加成时封顶应提高");
  }

  @Test
  void testShouldExtinguish() {
    assertTrue(
        ShockfieldMath.shouldExtinguish(0.01),
        "振幅低于阈值应熄灭");
    assertFalse(
        ShockfieldMath.shouldExtinguish(0.03),
        "振幅高于阈值不应熄灭");
  }

  @Test
  void testHasExceededLifetime() {
    assertTrue(
        ShockfieldMath.hasExceededLifetime(11.0),
        "寿命超过上限应熄灭");
    assertFalse(
        ShockfieldMath.hasExceededLifetime(9.0),
        "寿命未超上限不应熄灭");
  }

  @Test
  void testComputeRadius() {
    double ageSeconds = 2.0;
    double expected = ShockfieldMath.RADIAL_SPEED * ageSeconds; // = 8.0 * 2.0 = 16.0
    double actual = ShockfieldMath.computeRadius(ageSeconds);
    assertEquals(expected, actual, EPSILON, "波前半径计算错误");
  }
}
