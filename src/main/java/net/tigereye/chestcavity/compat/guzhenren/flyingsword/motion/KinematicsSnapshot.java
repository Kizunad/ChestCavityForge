package net.tigereye.chestcavity.compat.guzhenren.flyingsword.motion;

import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.FlyingSwordEntity;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.calculator.context.CalcContexts;

/**
 * 每 tick 由实体生成的运动学快照，缓存常用计算结果，避免重复计算。
 */
public record KinematicsSnapshot(
    Vec3 currentVelocity,
    double effectiveBase,
    double effectiveMax,
    double effectiveAccel,
    double turnRate,
    double domainScale) {

  /**
    * 构建快照。
    */
  public static KinematicsSnapshot capture(FlyingSwordEntity sword) {
    var ctx = CalcContexts.from(sword);
    double base =
        net.tigereye.chestcavity.compat.guzhenren.flyingsword.calculator
            .FlyingSwordCalculator.effectiveSpeedBase(sword.getSwordAttributes().speedBase, ctx);
    double max =
        net.tigereye.chestcavity.compat.guzhenren.flyingsword.calculator
            .FlyingSwordCalculator.effectiveSpeedMax(sword.getSwordAttributes().speedMax, ctx);
    double accel =
        net.tigereye.chestcavity.compat.guzhenren.flyingsword.calculator
            .FlyingSwordCalculator.effectiveAccel(sword.getSwordAttributes().accel, ctx);
    double turn = sword.getSwordAttributes().turnRate;
    double domainScale =
        net.tigereye.chestcavity.compat.guzhenren.flyingsword.integration.domain
            .SwordSpeedModifiers.computeDomainSpeedScale(sword);
    return new KinematicsSnapshot(sword.getDeltaMovement(), base, max, accel, turn, domainScale);
  }

  /**
   * 计算基于 base 的缩放系数。
   */
  public double baseScale(double rawBase) {
    return rawBase <= 1.0e-8 ? 1.0 : effectiveBase / rawBase;
  }

  /**
   * 返回领域缩放后的最大速度。
   */
  public double scaledMaxSpeed() {
    return effectiveMax * domainScale;
  }

  /**
   * 返回领域缩放后的基础速度。
   */
  public double scaledBaseSpeed() {
    return effectiveBase * domainScale;
  }

  /**
   * 返回领域缩放后的加速度。
   */
  public double scaledAccel() {
    return effectiveAccel * Math.max(0.1, domainScale);
  }

  /**
   * 返回领域缩放后的最大转向速度（仍为角度/ tick）。
   */
  public double scaledTurnRate() {
    return turnRate * Math.max(0.1, domainScale);
  }
}
