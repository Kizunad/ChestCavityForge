package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.trajectory.templates;

import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.intent.AIContext;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.intent.IntentResult;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.steering.KinematicsOps;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.steering.KinematicsSnapshot;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.steering.SteeringCommand;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.steering.SteeringTemplate;

/**
 * 曲线拦截模板：在直线提前量基础上加入侧向摆动，兼顾平滑转向与截击。
 */
public final class CurvedInterceptTemplate implements SteeringTemplate {

  @Override
  public SteeringCommand compute(
      AIContext ctx, IntentResult intent, KinematicsSnapshot snapshot) {
    Vec3 intercept = calculateInterceptPoint(ctx, intent);
    Vec3 toIntercept = intercept.subtract(ctx.sword().position());
    if (toIntercept.lengthSqr() < 1.0e-6) {
      return SteeringCommand.of(Vec3.ZERO, 0.0);
    }

    Vec3 forward = KinematicsOps.normaliseSafe(toIntercept);
    Vec3 blended = blendDirection(forward, ctx.sword().getDeltaMovement());
    Vec3 lateral = computeLateral(blended);

    double distance = toIntercept.length();
    double curvature = computeCurvature(distance, intent);
    Vec3 desiredDir = composeDesiredDirection(blended, lateral, ctx.sword().tickCount, curvature, forward);

    double speedScale = computeSpeedScale(intent, snapshot);
    TurnParams p = computeDynamicTurnParams(ctx, snapshot, intent, curvature);

    return SteeringCommand.of(desiredDir, speedScale)
        .withDesiredMaxFactor(Math.min(1.25, 0.95 + curvature * 0.5))
        .withTurnPerTick(p.turnPerTick)
        .withHeadingKp(p.headingKp)
        .withMinTurnFloor(p.minFloor)
        .withAccelFactor(p.accelMul)
        .disableSeparation();
  }

  @Override
  public SpeedUnit speedUnit() {
    return SpeedUnit.BASE;
  }

  @Override
  public boolean enableSeparation() {
    return false;
  }

  private static double clamp(double lo, double hi, double v) {
    return Math.max(lo, Math.min(hi, v));
  }

  private static double getOrDefault(
      net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.intent.IntentResult intent,
      String key,
      double def) {
    Double v = intent.getParams().get(key);
    return v == null ? def : v;
  }

  private static Vec3 calculateInterceptPoint(AIContext ctx, IntentResult intent) {
    return intent
        .getTargetEntity()
        .map(e -> e.position().add(KinematicsOps.normaliseSafe(e.getDeltaMovement()).scale(0.5)))
        .or(() -> intent.getTargetPos())
        .orElse(ctx.sword().position());
  }

  private static Vec3 blendDirection(Vec3 forward, Vec3 currentVel) {
    if (currentVel.lengthSqr() <= 1.0e-6) return forward;
    Vec3 currentDir = KinematicsOps.normaliseSafe(currentVel);
    Vec3 blended = currentDir.scale(0.4).add(forward.scale(0.6));
    return blended.lengthSqr() > 1.0e-6 ? blended.normalize() : forward;
  }

  private static Vec3 computeLateral(Vec3 blended) {
    Vec3 up = new Vec3(0.0, 1.0, 0.0);
    Vec3 lateral = blended.cross(up);
    if (lateral.lengthSqr() < 1.0e-6) {
      lateral = new Vec3(1.0, 0.0, 0.0);
    }
    return lateral.normalize();
  }

  private static double computeCurvature(double distance, IntentResult intent) {
    double base = Math.min(0.4, Math.max(0.12, 2.0 / Math.max(1.0, distance)));
    double scale = intent.getParams().getOrDefault("curvature_scale", 1.0);
    return Math.min(0.6, Math.max(0.05, base * scale));
  }

  private static Vec3 composeDesiredDirection(
      Vec3 blended, Vec3 lateral, int tickCount, double curvature, Vec3 fallback) {
    double sweep = Math.sin(tickCount * 0.4) * curvature;
    Vec3 dir = blended.add(lateral.scale(sweep));
    return dir.lengthSqr() < 1.0e-6 ? fallback : dir;
  }

  private static double computeSpeedScale(IntentResult intent, KinematicsSnapshot snapshot) {
    double speedScaleParam = intent.getParams().getOrDefault("speed_scale", 1.0);
    double baseSpeed = snapshot.scaledBaseSpeed();
    double maxSpeed = snapshot.scaledMaxSpeed();
    double baseToMaxRatio = maxSpeed / Math.max(1.0e-6, baseSpeed);
    return Math.max(0.1, speedScaleParam) * baseToMaxRatio;
  }

  private static TurnParams computeDynamicTurnParams(
      AIContext ctx, KinematicsSnapshot snapshot, IntentResult intent, double curvature) {
    double maxSpeed = Math.max(1.0e-6, snapshot.scaledMaxSpeed());
    double curSpeed = ctx.sword().getDeltaMovement().length();
    double sr = Math.max(0.0, Math.min(1.0, curSpeed / maxSpeed));
    double turnMin = 0.22;
    double turnMax = 0.48;
    double turnBase = 0.24;
    double turnSpeedGain = 0.14;
    double turnCurvatureGain = 0.18;
    double kpMin = 0.45;
    double kpMax = 0.9;
    double kpBase = 0.5;
    double kpSpeedGain = 0.25;
    double kpCurvatureGain = 0.2;
    double floorMin = 0.03;
    double floorMax = 0.08;
    double floorBase = 0.04;
    double floorSpeedGain = 0.03;
    double accelMin = 1.05;
    double accelMax = 1.3;
    double accelBase = 1.05;
    double accelSpeedGain = 0.15;

    double turnPerTick =
        clamp(turnMin, turnMax, turnBase + turnSpeedGain * sr + turnCurvatureGain * curvature);
    double headingKp =
        clamp(kpMin, kpMax, kpBase + kpSpeedGain * sr + kpCurvatureGain * curvature);
    double minFloor = clamp(floorMin, floorMax, floorBase + floorSpeedGain * sr);
    double accelMul = clamp(accelMin, accelMax, accelBase + accelSpeedGain * (1.0 - sr));
    turnPerTick = getOrDefault(intent, "turn_pt", turnPerTick);
    headingKp = getOrDefault(intent, "heading_kp", headingKp);
    minFloor = getOrDefault(intent, "turn_floor", minFloor);
    accelMul = getOrDefault(intent, "accel_mult", accelMul);
    return new TurnParams(turnPerTick, headingKp, minFloor, accelMul);
  }

  private static final class TurnParams {
    final double turnPerTick;
    final double headingKp;
    final double minFloor;
    final double accelMul;
    TurnParams(double t, double k, double f, double a) {
      this.turnPerTick = t;
      this.headingKp = k;
      this.minFloor = f;
      this.accelMul = a;
    }
  }
}
