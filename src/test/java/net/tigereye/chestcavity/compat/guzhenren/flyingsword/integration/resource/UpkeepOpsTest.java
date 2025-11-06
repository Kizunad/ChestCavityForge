package net.tigereye.chestcavity.compat.guzhenren.flyingsword.integration.resource;

import static org.junit.jupiter.api.Assertions.*;

import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.AIMode;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.tuning.FlyingSwordTuning;
import org.junit.jupiter.api.Test;

/**
 * 维持消耗操作单元测试
 *
 * <p>测试覆盖：
 * - 区间消耗计算（时间窗口转换）
 * - 不同模式下的消耗率
 * - 速度百分比影响
 * - 边界条件处理
 */
public class UpkeepOpsTest {

  // ===== 区间消耗计算测试 =====

  @Test
  public void testComputeIntervalUpkeepCost_Basic() {
    // 基础测试：1秒（20 ticks）窗口
    double baseRate = 1.0; // 每秒1单位
    AIMode mode = AIMode.ORBIT;
    boolean sprinting = false;
    boolean breaking = false;
    double speedPercent = 0.5;
    int intervalTicks = 20; // 1秒

    double result = UpkeepOps.computeIntervalUpkeepCost(
        baseRate, mode, sprinting, breaking, speedPercent, intervalTicks);

    // 1秒窗口，消耗应该约等于每秒消耗
    double perSecond = baseRate * FlyingSwordTuning.UPKEEP_ORBIT_MULT;
    double expected = perSecond * (intervalTicks / 20.0);
    assertEquals(expected, result, 0.001);
  }

  @Test
  public void testComputeIntervalUpkeepCost_HalfSecond() {
    // 半秒（10 ticks）窗口
    double baseRate = 2.0;
    AIMode mode = AIMode.GUARD;
    boolean sprinting = false;
    boolean breaking = false;
    double speedPercent = 0.5;
    int intervalTicks = 10; // 0.5秒

    double result = UpkeepOps.computeIntervalUpkeepCost(
        baseRate, mode, sprinting, breaking, speedPercent, intervalTicks);

    // 0.5秒窗口，消耗应该是每秒消耗的一半
    double perSecond = baseRate * FlyingSwordTuning.UPKEEP_GUARD_MULT;
    double expected = perSecond * (intervalTicks / 20.0);
    assertEquals(expected, result, 0.001);
  }

  @Test
  public void testComputeIntervalUpkeepCost_DefaultInterval() {
    // 使用默认检查间隔（UPKEEP_CHECK_INTERVAL）
    double baseRate = 1.0;
    AIMode mode = AIMode.HUNT;
    boolean sprinting = false;
    boolean breaking = false;
    double speedPercent = 0.5;
    int intervalTicks = FlyingSwordTuning.UPKEEP_CHECK_INTERVAL;

    double result = UpkeepOps.computeIntervalUpkeepCost(
        baseRate, mode, sprinting, breaking, speedPercent, intervalTicks);

    double perSecond = baseRate * FlyingSwordTuning.UPKEEP_HUNT_MULT;
    double expected = perSecond * (intervalTicks / 20.0);
    assertEquals(expected, result, 0.001);
  }

  // ===== 不同模式测试 =====

  @Test
  public void testComputeIntervalUpkeepCost_OrbitMode() {
    double baseRate = 1.0;
    AIMode mode = AIMode.ORBIT;
    int intervalTicks = 20;

    double result = UpkeepOps.computeIntervalUpkeepCost(
        baseRate, mode, false, false, 0.5, intervalTicks);

    double expected = baseRate * FlyingSwordTuning.UPKEEP_ORBIT_MULT;
    assertEquals(expected, result, 0.001);
  }

  @Test
  public void testComputeIntervalUpkeepCost_GuardMode() {
    double baseRate = 1.0;
    AIMode mode = AIMode.GUARD;
    int intervalTicks = 20;

    double result = UpkeepOps.computeIntervalUpkeepCost(
        baseRate, mode, false, false, 0.5, intervalTicks);

    double expected = baseRate * FlyingSwordTuning.UPKEEP_GUARD_MULT;
    assertEquals(expected, result, 0.001);
  }

  @Test
  public void testComputeIntervalUpkeepCost_HuntMode() {
    double baseRate = 1.0;
    AIMode mode = AIMode.HUNT;
    int intervalTicks = 20;

    double result = UpkeepOps.computeIntervalUpkeepCost(
        baseRate, mode, false, false, 0.5, intervalTicks);

    double expected = baseRate * FlyingSwordTuning.UPKEEP_HUNT_MULT;
    assertEquals(expected, result, 0.001);
  }

  @Test
  public void testComputeIntervalUpkeepCost_AllModes() {
    // 验证所有模式都有消耗
    double baseRate = 1.0;
    int intervalTicks = 20;

    for (AIMode mode : AIMode.values()) {
      double result = UpkeepOps.computeIntervalUpkeepCost(
          baseRate, mode, false, false, 0.5, intervalTicks);

      // 所有模式都应该有正的消耗
      assertTrue(result > 0, "Mode " + mode + " should have positive upkeep");
    }
  }

  // ===== 状态倍率测试 =====

  @Test
  public void testComputeIntervalUpkeepCost_WithSprinting() {
    double baseRate = 1.0;
    AIMode mode = AIMode.ORBIT;
    int intervalTicks = 20;

    double normalCost = UpkeepOps.computeIntervalUpkeepCost(
        baseRate, mode, false, false, 0.5, intervalTicks);

    double sprintingCost = UpkeepOps.computeIntervalUpkeepCost(
        baseRate, mode, true, false, 0.5, intervalTicks);

    // 冲刺时消耗应该更高
    assertTrue(sprintingCost > normalCost);

    // 应该正好是 UPKEEP_SPRINT_MULT 倍
    double ratio = sprintingCost / normalCost;
    assertEquals(FlyingSwordTuning.UPKEEP_SPRINT_MULT, ratio, 0.001);
  }

  @Test
  public void testComputeIntervalUpkeepCost_WithBreaking() {
    double baseRate = 1.0;
    AIMode mode = AIMode.ORBIT;
    int intervalTicks = 20;

    double normalCost = UpkeepOps.computeIntervalUpkeepCost(
        baseRate, mode, false, false, 0.5, intervalTicks);

    double breakingCost = UpkeepOps.computeIntervalUpkeepCost(
        baseRate, mode, false, true, 0.5, intervalTicks);

    // 破块时消耗应该更高
    assertTrue(breakingCost > normalCost);

    // 应该正好是 UPKEEP_BREAK_MULT 倍
    double ratio = breakingCost / normalCost;
    assertEquals(FlyingSwordTuning.UPKEEP_BREAK_MULT, ratio, 0.001);
  }

  @Test
  public void testComputeIntervalUpkeepCost_SprintingAndBreaking() {
    double baseRate = 1.0;
    AIMode mode = AIMode.HUNT;
    int intervalTicks = 20;

    double normalCost = UpkeepOps.computeIntervalUpkeepCost(
        baseRate, mode, false, false, 0.5, intervalTicks);

    double bothCost = UpkeepOps.computeIntervalUpkeepCost(
        baseRate, mode, true, true, 0.5, intervalTicks);

    // 同时冲刺和破块时消耗应该是两个倍率的乘积
    double expectedRatio = FlyingSwordTuning.UPKEEP_SPRINT_MULT * FlyingSwordTuning.UPKEEP_BREAK_MULT;
    double actualRatio = bothCost / normalCost;
    assertEquals(expectedRatio, actualRatio, 0.001);
  }

  // ===== 速度百分比测试 =====

  @Test
  public void testComputeIntervalUpkeepCost_LowSpeed() {
    double baseRate = 1.0;
    AIMode mode = AIMode.ORBIT;
    int intervalTicks = 20;

    double lowSpeedCost = UpkeepOps.computeIntervalUpkeepCost(
        baseRate, mode, false, false, 0.0, intervalTicks);

    double normalSpeedCost = UpkeepOps.computeIntervalUpkeepCost(
        baseRate, mode, false, false, 0.5, intervalTicks);

    // 低速时消耗应该更低（但有下限0.5）
    assertTrue(lowSpeedCost <= normalSpeedCost);
  }

  @Test
  public void testComputeIntervalUpkeepCost_HighSpeed() {
    double baseRate = 1.0;
    AIMode mode = AIMode.ORBIT;
    int intervalTicks = 20;

    double normalSpeedCost = UpkeepOps.computeIntervalUpkeepCost(
        baseRate, mode, false, false, 0.5, intervalTicks);

    double highSpeedCost = UpkeepOps.computeIntervalUpkeepCost(
        baseRate, mode, false, false, 1.0, intervalTicks);

    // 高速时消耗应该更高
    assertTrue(highSpeedCost > normalSpeedCost);
  }

  @Test
  public void testComputeIntervalUpkeepCost_SpeedProgression() {
    // 测试速度从0%到100%的消耗渐进
    double baseRate = 1.0;
    AIMode mode = AIMode.ORBIT;
    int intervalTicks = 20;

    double[] speeds = {0.0, 0.25, 0.5, 0.75, 1.0};
    double previousCost = 0.0;

    for (double speed : speeds) {
      double cost = UpkeepOps.computeIntervalUpkeepCost(
          baseRate, mode, false, false, speed, intervalTicks);

      // 速度越高，消耗应该单调增加（除了下限）
      if (speed > 0.0) {
        assertTrue(cost >= previousCost,
            String.format("Cost at speed %.2f (%.3f) should be >= cost at previous speed (%.3f)",
                speed, cost, previousCost));
      }

      previousCost = cost;
    }
  }

  // ===== 边界条件测试 =====

  @Test
  public void testComputeIntervalUpkeepCost_ZeroInterval() {
    // 0 tick窗口，消耗应该为0
    double baseRate = 1.0;
    AIMode mode = AIMode.ORBIT;
    int intervalTicks = 0;

    double result = UpkeepOps.computeIntervalUpkeepCost(
        baseRate, mode, false, false, 0.5, intervalTicks);

    assertEquals(0.0, result, 0.001);
  }

  @Test
  public void testComputeIntervalUpkeepCost_LargeInterval() {
    // 大窗口（10秒 = 200 ticks）
    double baseRate = 1.0;
    AIMode mode = AIMode.ORBIT;
    int intervalTicks = 200; // 10秒

    double result = UpkeepOps.computeIntervalUpkeepCost(
        baseRate, mode, false, false, 0.5, intervalTicks);

    // 10秒窗口，消耗应该是每秒消耗的10倍
    double perSecond = baseRate * FlyingSwordTuning.UPKEEP_ORBIT_MULT;
    double expected = perSecond * 10.0;
    assertEquals(expected, result, 0.001);
  }

  @Test
  public void testComputeIntervalUpkeepCost_SpeedOverRange() {
    // 速度百分比超出 [0,1] 范围（应该被正确处理）
    double baseRate = 1.0;
    AIMode mode = AIMode.ORBIT;
    int intervalTicks = 20;

    // 超过100%速度
    double overSpeedCost = UpkeepOps.computeIntervalUpkeepCost(
        baseRate, mode, false, false, 1.5, intervalTicks);

    // 应该仍然能计算（不会崩溃）
    assertTrue(overSpeedCost > 0);

    // 负速度
    double negativeSpeedCost = UpkeepOps.computeIntervalUpkeepCost(
        baseRate, mode, false, false, -0.5, intervalTicks);

    // 应该有下限保护（speedFactor >= 0.5）
    assertTrue(negativeSpeedCost > 0);
  }

  @Test
  public void testComputeIntervalUpkeepCost_ZeroBaseRate() {
    // 基础消耗率为0
    double baseRate = 0.0;
    AIMode mode = AIMode.ORBIT;
    int intervalTicks = 20;

    double result = UpkeepOps.computeIntervalUpkeepCost(
        baseRate, mode, false, false, 0.5, intervalTicks);

    assertEquals(0.0, result, 0.001);
  }

  @Test
  public void testComputeIntervalUpkeepCost_HighBaseRate() {
    // 极高基础消耗率
    double baseRate = 100.0;
    AIMode mode = AIMode.HUNT;
    int intervalTicks = 20;

    double result = UpkeepOps.computeIntervalUpkeepCost(
        baseRate, mode, false, false, 0.5, intervalTicks);

    // 应该是基础率 * 模式倍率
    double expected = baseRate * FlyingSwordTuning.UPKEEP_HUNT_MULT;
    assertEquals(expected, result, 0.001);
  }

  // ===== 综合场景测试 =====

  @Test
  public void testRealWorldScenario_QuickCheck() {
    // 场景：快速检查（5 ticks = 0.25秒）
    double baseRate = FlyingSwordTuning.UPKEEP_BASE_RATE;
    AIMode mode = AIMode.ORBIT;
    int intervalTicks = 5; // 快速检查

    double result = UpkeepOps.computeIntervalUpkeepCost(
        baseRate, mode, false, false, 0.5, intervalTicks);

    // 应该是很小的消耗（0.25秒）
    double perSecond = baseRate * FlyingSwordTuning.UPKEEP_ORBIT_MULT;
    double expected = perSecond * 0.25;
    assertEquals(expected, result, 0.001);
  }

  @Test
  public void testRealWorldScenario_StandardCheck() {
    // 场景：标准维持检查（使用默认间隔）
    double baseRate = FlyingSwordTuning.UPKEEP_BASE_RATE;
    AIMode mode = AIMode.GUARD;
    int intervalTicks = FlyingSwordTuning.UPKEEP_CHECK_INTERVAL;
    double speedPercent = 0.7; // 70% 速度

    double result = UpkeepOps.computeIntervalUpkeepCost(
        baseRate, mode, false, false, speedPercent, intervalTicks);

    // 验证结果是正数且合理
    assertTrue(result > 0);

    // 应该在合理范围内（不会过高或过低）
    double perSecond = baseRate * FlyingSwordTuning.UPKEEP_GUARD_MULT;
    double windowSeconds = intervalTicks / 20.0;
    assertTrue(result <= perSecond * windowSeconds * 2.0); // 考虑速度加成
  }

  @Test
  public void testRealWorldScenario_ExtremeCombat() {
    // 场景：极端战斗（HUNT模式，冲刺，高速）
    double baseRate = FlyingSwordTuning.UPKEEP_BASE_RATE;
    AIMode mode = AIMode.HUNT;
    boolean sprinting = true;
    boolean breaking = false;
    double speedPercent = 0.95; // 接近最大速度
    int intervalTicks = FlyingSwordTuning.UPKEEP_CHECK_INTERVAL;

    double result = UpkeepOps.computeIntervalUpkeepCost(
        baseRate, mode, sprinting, breaking, speedPercent, intervalTicks);

    // 极端情况下消耗应该很高
    double normalCost = UpkeepOps.computeIntervalUpkeepCost(
        baseRate, AIMode.ORBIT, false, false, 0.5, intervalTicks);

    // 应该至少是正常消耗的2倍
    assertTrue(result > normalCost * 2.0);
  }

  @Test
  public void testRealWorldScenario_IdleOrbit() {
    // 场景：空闲环绕（ORBIT模式，低速）
    double baseRate = FlyingSwordTuning.UPKEEP_BASE_RATE;
    AIMode mode = AIMode.ORBIT;
    boolean sprinting = false;
    boolean breaking = false;
    double speedPercent = 0.3; // 低速
    int intervalTicks = FlyingSwordTuning.UPKEEP_CHECK_INTERVAL;

    double result = UpkeepOps.computeIntervalUpkeepCost(
        baseRate, mode, sprinting, breaking, speedPercent, intervalTicks);

    // 空闲时消耗应该较低
    assertTrue(result > 0); // 但不会为0

    // 应该接近基础消耗（考虑速度下限）
    double perSecond = baseRate * FlyingSwordTuning.UPKEEP_ORBIT_MULT;
    double windowSeconds = intervalTicks / 20.0;
    assertTrue(result >= perSecond * windowSeconds * 0.5); // 考虑速度下限
  }

  // ===== 精度测试 =====

  @Test
  public void testComputeIntervalUpkeepCost_Precision() {
    // 测试浮点精度
    double baseRate = 1.0 / 3.0; // 0.333...
    AIMode mode = AIMode.ORBIT;
    int intervalTicks = 7; // 不是20的整数倍

    double result = UpkeepOps.computeIntervalUpkeepCost(
        baseRate, mode, false, false, 0.5, intervalTicks);

    // 应该能正确处理浮点计算
    assertTrue(result > 0);
    assertTrue(Double.isFinite(result));
  }

  @Test
  public void testComputeIntervalUpkeepCost_Consistency() {
    // 多次调用应该返回相同结果
    double baseRate = 1.0;
    AIMode mode = AIMode.GUARD;
    int intervalTicks = 20;

    double result1 = UpkeepOps.computeIntervalUpkeepCost(
        baseRate, mode, false, false, 0.5, intervalTicks);

    double result2 = UpkeepOps.computeIntervalUpkeepCost(
        baseRate, mode, false, false, 0.5, intervalTicks);

    // 应该是确定性的
    assertEquals(result1, result2, 0.0);
  }
}
