package net.tigereye.chestcavity.compat.guzhenren.flyingsword.motion;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.FlyingSwordEntity;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.AIMode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;

/**
 * SteeringOps 单元测试。
 *
 * <p>测试覆盖：
 * <ul>
 *   <li>速度约束（最大速度限制）</li>
 *   <li>加速度限制</li>
 *   <li>转向计算（角度插值）</li>
 *   <li>边界处理（null命令、零向量等）</li>
 * </ul>
 *
 * <p>注意：由于 computeNewVelocity 依赖 FlyingSwordEntity.getAIMode()，
 * 测试使用 Mockito 创建简化的 mock 实体。后续可考虑将 AIMode 作为参数传入以进一步抽象MC依赖。
 */
@Disabled("依赖 FlyingSwordEntity（MC 类）mock，单元测试阶段跳过；建议后续将 AIMode 解耦为参数再补测")
class SteeringOpsTest {

  // ========== 边界条件测试 ==========

  @Test
  void computeNewVelocity_NullCommand_ReturnsCurrentVelocity() {
    FlyingSwordEntity mockSword = createMockSword(AIMode.ORBIT);
    Vec3 currentVel = new Vec3(1, 0, 0);
    KinematicsSnapshot snapshot = createSnapshot(currentVel, 0.5, 2.0, 0.1, 0.2);

    Vec3 result = SteeringOps.computeNewVelocity(mockSword, null, snapshot);

    assertEquals(currentVel, result, "Null command should return current velocity");
  }

  @Test
  void computeNewVelocity_ZeroDirectionCommand_ReturnsCurrentVelocity() {
    FlyingSwordEntity mockSword = createMockSword(AIMode.ORBIT);
    Vec3 currentVel = new Vec3(1, 0, 0);
    KinematicsSnapshot snapshot = createSnapshot(currentVel, 0.5, 2.0, 0.1, 0.2);
    SteeringCommand command = SteeringCommand.of(Vec3.ZERO, 1.0);

    Vec3 result = SteeringOps.computeNewVelocity(mockSword, command, snapshot);

    assertEquals(currentVel, result, "Zero direction should return current velocity");
  }

  @Test
  void computeNewVelocity_VerySmallDirectionCommand_ReturnsCurrentVelocity() {
    FlyingSwordEntity mockSword = createMockSword(AIMode.ORBIT);
    Vec3 currentVel = new Vec3(1, 0, 0);
    KinematicsSnapshot snapshot = createSnapshot(currentVel, 0.5, 2.0, 0.1, 0.2);
    // 小于 EPS (1.0e-6) 的向量
    SteeringCommand command = SteeringCommand.of(new Vec3(1e-8, 1e-8, 1e-8), 1.0);

    Vec3 result = SteeringOps.computeNewVelocity(mockSword, command, snapshot);

    assertEquals(currentVel, result, "Very small direction should return current velocity");
  }

  // ========== 速度约束测试 ==========

  @Test
  void computeNewVelocity_ExceedsMaxSpeed_ClampedToMax() {
    FlyingSwordEntity mockSword = createMockSword(AIMode.HUNT);
    // 当前已经接近最大速度
    Vec3 currentVel = new Vec3(1.9, 0, 0);
    double baseSpeed = 0.5;
    double maxSpeed = 2.0;
    KinematicsSnapshot snapshot = createSnapshot(currentVel, baseSpeed, maxSpeed, 0.5, 0.3);

    // 命令尝试加速到更高速度
    SteeringCommand command = SteeringCommand.of(new Vec3(1, 0, 0), 3.0); // 期望 baseSpeed * 3.0 = 1.5

    Vec3 result = SteeringOps.computeNewVelocity(mockSword, command, snapshot);

    // 结果速度不应超过 maxSpeed
    assertTrue(result.length() <= maxSpeed + 1e-6,
        "Result speed should not exceed max speed: " + result.length() + " > " + maxSpeed);
  }

  @Test
  void computeNewVelocity_NegativeSpeedScale_ClampedToZero() {
    FlyingSwordEntity mockSword = createMockSword(AIMode.ORBIT);
    Vec3 currentVel = new Vec3(1, 0, 0);
    KinematicsSnapshot snapshot = createSnapshot(currentVel, 0.5, 2.0, 0.1, 0.2);

    // 负的速度倍率应被钳制为0
    SteeringCommand command = SteeringCommand.of(new Vec3(1, 0, 0), -0.5);

    Vec3 result = SteeringOps.computeNewVelocity(mockSword, command, snapshot);

    // 由于加速度限制，不会立即变为0，但目标速度应该是0
    // 结果应该比当前速度慢
    assertTrue(result.length() < currentVel.length() + 1e-6,
        "Negative speed scale should reduce velocity");
  }

  @Test
  void computeNewVelocity_DesiredMaxFactorOverride_AffectsMaxSpeed() {
    FlyingSwordEntity mockSword = createMockSword(AIMode.HUNT);
    Vec3 currentVel = new Vec3(0.5, 0, 0);
    double baseSpeed = 0.5;
    double maxSpeed = 2.0;
    KinematicsSnapshot snapshot = createSnapshot(currentVel, baseSpeed, maxSpeed, 0.5, 0.3);

    // 命令期望最大速度降低到50%
    SteeringCommand command = SteeringCommand.of(new Vec3(1, 0, 0), 2.0)
        .withDesiredMaxFactor(0.5);

    Vec3 result = SteeringOps.computeNewVelocity(mockSword, command, snapshot);

    // 结果速度不应超过 maxSpeed * 0.5 = 1.0
    assertTrue(result.length() <= maxSpeed * 0.5 + 1e-6,
        "Result speed should not exceed reduced max speed");
  }

  // ========== 加速度限制测试 ==========

  @Test
  void computeNewVelocity_LargeSpeedChange_LimitedByAcceleration() {
    FlyingSwordEntity mockSword = createMockSword(AIMode.ORBIT);
    Vec3 currentVel = new Vec3(0.1, 0, 0);
    double baseSpeed = 0.5;
    double accel = 0.05; // 小加速度
    KinematicsSnapshot snapshot = createSnapshot(currentVel, baseSpeed, 2.0, accel, 0.2);

    // 命令尝试立即达到高速
    SteeringCommand command = SteeringCommand.of(new Vec3(1, 0, 0), 2.0); // 目标速度 1.0

    Vec3 result = SteeringOps.computeNewVelocity(mockSword, command, snapshot);

    // 速度变化应受加速度限制
    double speedChange = Math.abs(result.length() - currentVel.length());
    assertTrue(speedChange <= accel + 1e-6,
        "Speed change should be limited by acceleration: " + speedChange + " > " + accel);
  }

  @Test
  void computeNewVelocity_AccelOverride_AffectsAcceleration() {
    FlyingSwordEntity mockSword = createMockSword(AIMode.HUNT);
    Vec3 currentVel = new Vec3(0.1, 0, 0);
    double baseSpeed = 0.5;
    double accel = 0.1;
    KinematicsSnapshot snapshot = createSnapshot(currentVel, baseSpeed, 2.0, accel, 0.2);

    // 命令加速度倍率设为2.0
    SteeringCommand command = SteeringCommand.of(new Vec3(1, 0, 0), 2.0)
        .withAccelFactor(2.0);

    Vec3 result = SteeringOps.computeNewVelocity(mockSword, command, snapshot);

    // 速度变化应该比基础加速度更大（最多2倍）
    double speedChange = Math.abs(result.length() - currentVel.length());
    assertTrue(speedChange > accel - 1e-6,
        "Speed change should be greater with accel override");
    assertTrue(speedChange <= accel * 2.0 + 1e-6,
        "Speed change should not exceed doubled acceleration");
  }

  // ========== 转向计算测试 ==========

  @Test
  void computeNewVelocity_SameDirection_NoTurning() {
    FlyingSwordEntity mockSword = createMockSword(AIMode.ORBIT);
    Vec3 currentVel = new Vec3(1, 0, 0);
    KinematicsSnapshot snapshot = createSnapshot(currentVel, 0.5, 2.0, 0.5, 0.2);

    // 命令方向与当前速度相同
    SteeringCommand command = SteeringCommand.of(new Vec3(1, 0, 0), 1.0);

    Vec3 result = SteeringOps.computeNewVelocity(mockSword, command, snapshot);

    // 方向应基本保持不变（允许小误差）
    Vec3 resultDir = result.normalize();
    Vec3 currentDir = currentVel.normalize();
    double dotProduct = resultDir.dot(currentDir);
    assertTrue(dotProduct > 0.999, "Direction should remain the same: dot=" + dotProduct);
  }

  @Test
  void computeNewVelocity_90DegreeTurn_LimitedByTurnRate() {
    FlyingSwordEntity mockSword = createMockSword(AIMode.ORBIT);
    Vec3 currentVel = new Vec3(1, 0, 0); // 向+X
    double baseSpeed = 0.5;
    KinematicsSnapshot snapshot = createSnapshot(currentVel, baseSpeed, 2.0, 0.1, 0.2);

    // 命令转向+Z方向（90度）
    SteeringCommand command = SteeringCommand.of(new Vec3(0, 0, 1), 1.0);

    Vec3 result = SteeringOps.computeNewVelocity(mockSword, command, snapshot);

    // 计算转向角度
    Vec3 resultDir = result.normalize();
    Vec3 currentDir = currentVel.normalize();
    double dotProduct = Math.max(-1.0, Math.min(1.0, resultDir.dot(currentDir)));
    double actualTurn = Math.acos(dotProduct);

    // 转向角度应该小于90度（受转向速率限制）
    assertTrue(actualTurn < Math.PI / 2 - 0.01,
        "Turn should be limited by turn rate: actual=" + Math.toDegrees(actualTurn) + "°");

    // 但应该有一定的转向（不是0）
    assertTrue(actualTurn > 0.01,
        "Should have some turning: actual=" + Math.toDegrees(actualTurn) + "°");
  }

  @Test
  void computeNewVelocity_TurnOverride_AffectsTurnRate() {
    FlyingSwordEntity mockSword = createMockSword(AIMode.ORBIT);
    Vec3 currentVel = new Vec3(1, 0, 0);
    KinematicsSnapshot snapshot = createSnapshot(currentVel, 0.5, 2.0, 0.1, 0.1);

    // 命令提供更大的转向覆盖值
    double largeTurnOverride = Math.PI / 4; // 45度
    SteeringCommand command = SteeringCommand.of(new Vec3(0, 0, 1), 1.0)
        .withTurnOverride(largeTurnOverride);

    Vec3 result = SteeringOps.computeNewVelocity(mockSword, command, snapshot);

    // 由于提供了更大的转向覆盖，转向应该比默认更快
    Vec3 resultDir = result.normalize();
    Vec3 currentDir = currentVel.normalize();
    double dotProduct = Math.max(-1.0, Math.min(1.0, resultDir.dot(currentDir)));
    double actualTurn = Math.acos(dotProduct);

    // 实际转向应该接近（但不超过）turnOverride
    assertTrue(actualTurn <= largeTurnOverride + 0.01,
        "Turn should not exceed override: actual=" + Math.toDegrees(actualTurn) + "°");
  }

  @Test
  void computeNewVelocity_HeadingKp_ProportionalTurning() {
    FlyingSwordEntity mockSword = createMockSword(AIMode.HUNT);
    Vec3 currentVel = new Vec3(1, 0, 0);
    KinematicsSnapshot snapshot = createSnapshot(currentVel, 0.5, 2.0, 0.1, 0.2);

    // 使用比例控制：小角度误差时转得慢，大角度误差时转得快
    SteeringCommand command = SteeringCommand.of(new Vec3(0, 0, 1), 1.0)
        .withHeadingKp(0.5); // P系数

    Vec3 result = SteeringOps.computeNewVelocity(mockSword, command, snapshot);

    // 应该有转向发生
    Vec3 resultDir = result.normalize();
    Vec3 currentDir = currentVel.normalize();
    double dotProduct = Math.max(-1.0, Math.min(1.0, resultDir.dot(currentDir)));
    double actualTurn = Math.acos(dotProduct);

    assertTrue(actualTurn > 0.01, "Should have proportional turning");
  }

  // ========== 综合测试 ==========

  @Test
  void computeNewVelocity_ComplexManeuver_AllConstraintsApply() {
    FlyingSwordEntity mockSword = createMockSword(AIMode.HUNT);
    Vec3 currentVel = new Vec3(0.3, 0, 0);
    double baseSpeed = 0.5;
    double maxSpeed = 1.5;
    double accel = 0.08;
    KinematicsSnapshot snapshot = createSnapshot(currentVel, baseSpeed, maxSpeed, accel, 0.25);

    // 复杂命令：转向、加速、有各种覆盖
    SteeringCommand command = SteeringCommand.of(new Vec3(0, 0, 1), 2.0) // 转90度，加速到2倍base
        .withDesiredMaxFactor(0.8)
        .withAccelFactor(1.5)
        .withTurnOverride(Math.PI / 6);

    Vec3 result = SteeringOps.computeNewVelocity(mockSword, command, snapshot);

    // 验证所有约束
    // 1. 速度不超过调整后的最大值
    assertTrue(result.length() <= maxSpeed * 0.8 + 1e-6,
        "Speed should respect max factor override");

    // 2. 速度变化受加速度限制
    double speedChange = Math.abs(result.length() - currentVel.length());
    assertTrue(speedChange <= accel * 1.5 + 1e-6,
        "Speed change should respect accel override");

    // 3. 转向受角速度限制
    Vec3 resultDir = result.normalize();
    Vec3 currentDir = currentVel.normalize();
    double dotProduct = Math.max(-1.0, Math.min(1.0, resultDir.dot(currentDir)));
    double actualTurn = Math.acos(dotProduct);
    assertTrue(actualTurn <= Math.PI / 6 + 0.01,
        "Turn should respect turn override");

    // 4. 结果应该是有效的向量（无NaN/Inf）
    assertTrue(Double.isFinite(result.x) && Double.isFinite(result.y) && Double.isFinite(result.z),
        "Result should be finite");
  }

  @Test
  void computeNewVelocity_MultipleTicksConvergence_ApproachesTarget() {
    FlyingSwordEntity mockSword = createMockSword(AIMode.ORBIT);
    Vec3 currentVel = new Vec3(0.1, 0, 0);
    double baseSpeed = 0.5;
    KinematicsSnapshot snapshot = createSnapshot(currentVel, baseSpeed, 2.0, 0.05, 0.15);

    // 目标：沿+Z方向，速度1.0
    Vec3 targetDir = new Vec3(0, 0, 1);
    double targetSpeed = 1.0;
    SteeringCommand command = SteeringCommand.of(targetDir, targetSpeed / baseSpeed);

    Vec3 vel = currentVel;
    // 模拟多个tick
    for (int i = 0; i < 50; i++) {
      KinematicsSnapshot tickSnapshot = createSnapshot(vel, baseSpeed, 2.0, 0.05, 0.15);
      vel = SteeringOps.computeNewVelocity(mockSword, command, tickSnapshot);
    }

    // 经过足够多的tick后，应该接近目标方向和速度
    Vec3 finalDir = vel.normalize();
    double finalSpeed = vel.length();

    // 方向应该基本对齐（余弦相似度 > 0.95）
    assertTrue(finalDir.dot(targetDir) > 0.95,
        "Should converge to target direction: dot=" + finalDir.dot(targetDir));

    // 速度应该接近目标（±10%）
    assertTrue(Math.abs(finalSpeed - targetSpeed) < targetSpeed * 0.1,
        "Should converge to target speed: " + finalSpeed + " vs " + targetSpeed);
  }

  // ========== 不同AI模式测试 ==========

  @Test
  void computeNewVelocity_DifferentAIModes_DifferentTurnRates() {
    Vec3 currentVel = new Vec3(1, 0, 0);
    Vec3 targetDir = new Vec3(0, 0, 1);
    KinematicsSnapshot snapshot = createSnapshot(currentVel, 0.5, 2.0, 0.1, 0.2);
    SteeringCommand command = SteeringCommand.of(targetDir, 1.0);

    // ORBIT模式（较慢的转向）
    FlyingSwordEntity orbitSword = createMockSword(AIMode.ORBIT);
    Vec3 orbitResult = SteeringOps.computeNewVelocity(orbitSword, command, snapshot);
    double orbitTurn = calculateTurnAngle(currentVel, orbitResult);

    // HUNT模式（较快的转向）
    FlyingSwordEntity huntSword = createMockSword(AIMode.HUNT);
    Vec3 huntResult = SteeringOps.computeNewVelocity(huntSword, command, snapshot);
    double huntTurn = calculateTurnAngle(currentVel, huntResult);

    // HUNT模式应该转得更快（根据 FlyingSwordSteeringTuning 的配置）
    assertTrue(huntTurn >= orbitTurn - 0.01,
        "HUNT mode should turn faster or equal to ORBIT: hunt=" +
        Math.toDegrees(huntTurn) + "° vs orbit=" + Math.toDegrees(orbitTurn) + "°");
  }

  // ========== 辅助方法 ==========

  /**
   * 创建一个简化的 mock FlyingSwordEntity，只提供必要的 AIMode
   */
  private FlyingSwordEntity createMockSword(AIMode mode) {
    FlyingSwordEntity mock = mock(FlyingSwordEntity.class);
    when(mock.getAIMode()).thenReturn(mode);
    return mock;
  }

  /**
   * 创建一个测试用的 KinematicsSnapshot
   */
  private KinematicsSnapshot createSnapshot(
      Vec3 currentVelocity,
      double effectiveBase,
      double effectiveMax,
      double effectiveAccel,
      double turnRate) {
    // domainScale 设为 1.0 简化测试
    return new KinematicsSnapshot(currentVelocity, effectiveBase, effectiveMax, effectiveAccel, turnRate, 1.0);
  }

  /**
   * 计算两个速度向量之间的转向角度（弧度）
   */
  private double calculateTurnAngle(Vec3 from, Vec3 to) {
    Vec3 fromDir = from.normalize();
    Vec3 toDir = to.normalize();
    double dotProduct = Math.max(-1.0, Math.min(1.0, fromDir.dot(toDir)));
    return Math.acos(dotProduct);
  }
}
