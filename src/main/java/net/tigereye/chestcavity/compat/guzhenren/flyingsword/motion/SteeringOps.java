package net.tigereye.chestcavity.compat.guzhenren.flyingsword.motion;

import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.FlyingSwordEntity;

/**
 * 统一处理飞剑运动命令的工具。
 */
public final class SteeringOps {

  private static final double EPS = 1.0e-6;

  private SteeringOps() {}

  /**
   * 应用命令到实体，返回新的速度向量。
   */
  public static Vec3 computeNewVelocity(
      FlyingSwordEntity sword, SteeringCommand command, KinematicsSnapshot snapshot) {
    if (command == null || command.direction().lengthSqr() < EPS) {
      return snapshot.currentVelocity();
    }

    Vec3 desiredDir = command.direction();
    // 单位转换：BASE or MAX
    double speedScale = command.speedScale();
    if (speedScale < 0.0) {
      speedScale = 0.0;
    }

    double targetSpeed = snapshot.scaledBaseSpeed() * speedScale;
    Vec3 desiredVelocity = KinematicsOps.normaliseSafe(desiredDir).scale(targetSpeed);

    // 角速度限制
    double maxTurnRad =
        net.tigereye.chestcavity.compat.guzhenren.flyingsword.tuning
            .FlyingSwordSteeringTuning.defaultTurnLimitRadians(sword.getAIMode());
    // 允许以“较大的限制”提升转向响应（模式/属性/命令 三者取最大）
    maxTurnRad = Math.max(maxTurnRad, snapshot.scaledTurnRate());
    if (command.turnOverride() != null) {
      maxTurnRad = Math.max(maxTurnRad, Math.max(0.0, command.turnOverride()));
    }

    // 计算角误差
    Vec3 curDir = snapshot.currentVelocity();
    Vec3 curDirN = KinematicsOps.normaliseSafe(curDir);
    Vec3 desDirN = KinematicsOps.normaliseSafe(desiredVelocity);
    double dot = Math.max(-1.0, Math.min(1.0, curDirN.dot(desDirN)));
    double angleErr = Math.acos(dot);

    // 期望的本帧转角：默认取基础上限，可被 command 提供的策略覆盖
    double desiredTurn = maxTurnRad;
    if (command.turnPerTick() != null) {
      desiredTurn = Math.min(desiredTurn, Math.max(0.0, command.turnPerTick()));
    }
    if (command.headingKp() != null) {
      double kp = Math.max(0.0, command.headingKp());
      double floor = Math.max(0.0, command.minTurnFloor() == null ? 0.0 : command.minTurnFloor());
      double pTurn = Math.max(floor, kp * angleErr);
      desiredTurn = Math.min(desiredTurn, pTurn);
    }
    desiredTurn = Math.max(0.0, Math.min(desiredTurn, maxTurnRad));

    Vec3 limitedDir = KinematicsOps.limitTurn(snapshot.currentVelocity(), desiredVelocity, desiredTurn);
    Vec3 reprojectedDesired = limitedDir.scale(targetSpeed);

    // 线速度加速度限制
    double accel = snapshot.scaledAccel();
    if (command.accelOverride() != null) {
      accel *= Math.max(
          net.tigereye.chestcavity.compat.guzhenren.flyingsword.tuning
              .FlyingSwordSteeringTuning.minimumAccelerationFactor(),
          command.accelOverride());
    }

    Vec3 delta = reprojectedDesired.subtract(snapshot.currentVelocity());
    Vec3 limitedDelta = KinematicsOps.limitDelta(delta, accel);
    Vec3 newVelocity = snapshot.currentVelocity().add(limitedDelta);

    // 最大速度限制
    double maxSpeed = snapshot.scaledMaxSpeed();
    if (command.desiredMaxFactor() != null) {
      maxSpeed *= Math.max(0.1, command.desiredMaxFactor());
    }
    double speed = newVelocity.length();
    if (speed > maxSpeed && speed > EPS) {
      newVelocity = newVelocity.scale(maxSpeed / speed);
    }

    return newVelocity;
  }

}
