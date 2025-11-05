package net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.trajectory.templates;

import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.AIContext;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.IntentResult;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.motion.KinematicsOps;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.motion.KinematicsSnapshot;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.motion.SteeringCommand;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.motion.SteeringTemplate;

/**
 * 曲线拦截模板：在直线提前量基础上加入侧向摆动，兼顾平滑转向与截击。
 */
public final class CurvedInterceptTemplate implements SteeringTemplate {

  private static final double CURVE_ALPHA_MIN = 0.0;
  private static final double CURVE_ALPHA_MAX = 0.35;
  private static final double CURVE_ALPHA_GAIN = 0.9;
  private static final double SPEED_ALPHA_MIN = 0.6;
  private static final double SPEED_ALPHA_MAX = 1.0;
  private static final double ANGLE_GATE = Math.toRadians(20.0);
  private static final double SWEEP_FREQUENCY = 0.4;
  private static final double SWEEP_MAGNETUDE = 1.0;

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
    double speedRatio = computeSpeedRatio(ctx, snapshot);
    double steeringAngle =
        computeSteeringAngle(ctx, forward, blended, curvature, speedRatio);
    Vec3 desiredDir =
        composeDesiredDirection(blended, lateral, steeringAngle, forward);

    double speedScale = computeSpeedScale(intent, snapshot);
    TurnParams p = computeDynamicTurnParams(speedRatio, curvature, intent);

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
      net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.IntentResult intent,
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
      Vec3 base, Vec3 lateral, double angle, Vec3 fallback) {
    if (Math.abs(angle) < 1.0e-6) {
      return base;
    }
    double cos = Math.cos(angle);
    double sin = Math.sin(angle);
    Vec3 rotated = base.scale(cos).add(lateral.scale(sin));
    if (rotated.lengthSqr() < 1.0e-6) {
      return fallback;
    }
    return rotated.normalize();
  }

  private static double computeSpeedScale(IntentResult intent, KinematicsSnapshot snapshot) {
    double speedScaleParam = intent.getParams().getOrDefault("speed_scale", 1.0);
    double baseSpeed = snapshot.scaledBaseSpeed();
    double maxSpeed = snapshot.scaledMaxSpeed();
    double baseToMaxRatio = maxSpeed / Math.max(1.0e-6, baseSpeed);
    return Math.max(0.1, speedScaleParam) * baseToMaxRatio;
  }

  private static double computeSpeedRatio(AIContext ctx, KinematicsSnapshot snapshot) {
    double maxSpeed = Math.max(1.0e-6, snapshot.scaledMaxSpeed());
    double curSpeed = ctx.sword().getDeltaMovement().length();
    return Math.max(0.0, Math.min(1.0, curSpeed / maxSpeed));
  }

  private static double computeSteeringAngle(
      AIContext ctx, Vec3 forward, Vec3 blended, double curvature, double speedRatio) {
    double baseAlpha = clamp(CURVE_ALPHA_MIN, CURVE_ALPHA_MAX, curvature * CURVE_ALPHA_GAIN);
    double speedFactor = lerp(SPEED_ALPHA_MIN, SPEED_ALPHA_MAX, speedRatio);

    Vec3 currentVel = ctx.sword().getDeltaMovement();
    double alignmentGate = 1.0;
    if (currentVel.lengthSqr() > 1.0e-6) {
      Vec3 currentDir = KinematicsOps.normaliseSafe(currentVel);
      double dot = Math.max(-1.0, Math.min(1.0, currentDir.dot(forward)));
      double angleErr = Math.acos(dot);
      alignmentGate = angleErr >= ANGLE_GATE ? 0.0 : 1.0 - (angleErr / ANGLE_GATE);
    }

    if (alignmentGate <= 0.0 || baseAlpha <= 1.0e-6) {
      return 0.0;
    }

    double phase = computePhase(ctx);
    double signedAlpha = baseAlpha * speedFactor * alignmentGate * phase * SWEEP_MAGNETUDE;
    return clamp(-CURVE_ALPHA_MAX, CURVE_ALPHA_MAX, signedAlpha);
  }

  private static double computePhase(AIContext ctx) {
    long seed = ctx.sword().getUUID().getLeastSignificantBits() & 0xFFFFL;
    double phaseOffset = (seed / 65535.0) * Math.PI * 2.0;
    return Math.sin(ctx.sword().tickCount * SWEEP_FREQUENCY + phaseOffset);
  }

  private static double lerp(double a, double b, double t) {
    return a + (b - a) * Math.max(0.0, Math.min(1.0, t));
  }

  private static TurnParams computeDynamicTurnParams(
      double speedRatio, double curvature, IntentResult intent) {
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
        clamp(
            turnMin,
            turnMax,
            turnBase + turnSpeedGain * speedRatio + turnCurvatureGain * curvature);
    double headingKp =
        clamp(
            kpMin,
            kpMax,
            kpBase + kpSpeedGain * speedRatio + kpCurvatureGain * curvature);
    double minFloor = clamp(floorMin, floorMax, floorBase + floorSpeedGain * speedRatio);
    double accelMul =
        clamp(accelMin, accelMax, accelBase + accelSpeedGain * (1.0 - speedRatio));
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
