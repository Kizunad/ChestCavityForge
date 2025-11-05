package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.steering;

import net.minecraft.world.phys.Vec3;

/**
 * 承载轨迹或行为层输出的“期望运动命令”。
 *
 * <p>核心思想：策略层只负责给出期望方向和速度倍率，最终由运动层统一处理
 * 加速度、角速度、领域缩放等细节，确保全局一致。
 */
public record SteeringCommand(
    Vec3 direction,
    double speedScale,
    Double desiredMaxFactor,
    Double accelOverride,
    Double turnOverride,
    Double turnPerTick,
    Double headingKp,
    Double minTurnFloor,
    boolean suppressSeparation) {

  /**
   * 构造一个基本命令，方向必须是非零向量。
   *
   * <p>speedScale 基于“base 速度”尺度（1.0 = speedBase）。
   *
   * @param direction 期望方向（无需归一化，内部会处理）
   * @param speedScale 与 base 速度的倍率
   * @return 命令
   */
  public static SteeringCommand of(Vec3 direction, double speedScale) {
    return new SteeringCommand(direction, speedScale, null, null, null, null, null, null, false);
  }

  /**
   * 调整期望速度上限倍率（相对 effectiveMax）。
   *
   * @param factor 希望的最大速度倍率
   * @return 带有覆盖的命令
   */
  public SteeringCommand withDesiredMaxFactor(double factor) {
    return new SteeringCommand(direction, speedScale, factor, accelOverride, turnOverride, turnPerTick, headingKp, minTurnFloor, suppressSeparation);
  }

  /**
   * 调整加速度覆盖倍率（相对 effectiveAccel）。
   *
   * @param factor 希望的加速度倍率
   * @return 带有覆盖的命令
   */
  public SteeringCommand withAccelFactor(double factor) {
    return new SteeringCommand(direction, speedScale, desiredMaxFactor, factor, turnOverride, turnPerTick, headingKp, minTurnFloor, suppressSeparation);
  }

  /**
   * 标记禁用分离力（用于冲刺/影步等场景）。
   *
   * @return 标记后的命令
   */
  public SteeringCommand disableSeparation() {
    return new SteeringCommand(direction, speedScale, desiredMaxFactor, accelOverride, turnOverride, turnPerTick, headingKp, minTurnFloor, true);
  }

  /**
   * 设置转向角速度覆盖值。
   */
  public SteeringCommand withTurnOverride(double radians) {
    return new SteeringCommand(direction, speedScale, desiredMaxFactor, accelOverride, radians, turnPerTick, headingKp, minTurnFloor, suppressSeparation);
  }

  /**
   * 设置每 tick 目标转向角（弧度）。
   */
  public SteeringCommand withTurnPerTick(double radians) {
    return new SteeringCommand(direction, speedScale, desiredMaxFactor, accelOverride, turnOverride, radians, headingKp, minTurnFloor, suppressSeparation);
  }

  /**
   * 设置基于角误差的比例转向（PD 的 P 项），单位：rad_out per (rad_error)。
   */
  public SteeringCommand withHeadingKp(double kp) {
    return new SteeringCommand(direction, speedScale, desiredMaxFactor, accelOverride, turnOverride, turnPerTick, kp, minTurnFloor, suppressSeparation);
  }

  /**
   * 设置最小转向地板（避免小角误差时完全不转）。
   */
  public SteeringCommand withMinTurnFloor(double radians) {
    return new SteeringCommand(direction, speedScale, desiredMaxFactor, accelOverride, turnOverride, turnPerTick, headingKp, radians, suppressSeparation);
  }
}
