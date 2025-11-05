package net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.trajectory.templates;

import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.AIContext;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.IntentResult;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.motion.KinematicsSnapshot;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.motion.SteeringCommand;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.motion.SteeringTemplate;

/**
 * 线性拦截模板，支持参数化提前量与速度倍率。
 */
public final class PredictiveLineTemplate implements SteeringTemplate {

  private static final double DEFAULT_LEAD_SEC = 0.3;
  private static final double MAX_LEAD_SEC = 0.6;

  @Override
  public SteeringCommand compute(
      AIContext ctx, IntentResult intent, KinematicsSnapshot snapshot) {
    var sword = ctx.sword();
    Vec3 from = sword.position();

    Vec3 targetPos =
        intent
            .getTargetEntity()
            .map(e -> e.getEyePosition())
            .or(() -> intent.getTargetPos())
            .orElse(from);

    Vec3 targetVel = intent.getTargetEntity().map(e -> e.getDeltaMovement()).orElse(Vec3.ZERO);

    double leadSec = DEFAULT_LEAD_SEC;
    if (targetVel.length() > 1.0e-3) {
      double candidate = targetVel.length() / Math.max(1.0e-3, snapshot.scaledMaxSpeed());
      leadSec = Math.min(MAX_LEAD_SEC, Math.max(0.05, candidate));
    }

    double overrideLead = intent.getParams().getOrDefault("lead_time", Double.NaN);
    if (Double.isFinite(overrideLead)) {
      leadSec = Math.min(MAX_LEAD_SEC, Math.max(0.0, overrideLead));
    }

    Vec3 aimPoint = targetPos.add(targetVel.scale(leadSec * 20.0));
    Vec3 dir = aimPoint.subtract(from);
    if (dir.lengthSqr() < 1.0e-6) {
      dir = sword.getLookAngle();
    }

    double speedScale = intent.getParams().getOrDefault("speed_scale", 1.0);
    TurnParams p = computeDynamicTurnParams(ctx, snapshot, intent);
    return SteeringCommand.of(dir, Math.max(0.1, speedScale))
        .withDesiredMaxFactor(1.05)
        .withTurnPerTick(p.turnPerTick)
        .withHeadingKp(p.headingKp)
        .withMinTurnFloor(p.minFloor)
        .withAccelFactor(p.accelMul);
  }

  @Override
  public SpeedUnit speedUnit() {
    return SpeedUnit.MAX;
  }

  @Override
  public boolean enableSeparation() {
    return false;
  }

  private static double lerp(double a, double b, double t) {
    return a + (b - a) * Math.max(0.0, Math.min(1.0, t));
  }

  private static double getOrDefault(
      net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.IntentResult intent,
      String key,
      double def) {
    Double v = intent.getParams().get(key);
    return v == null ? def : v;
  }

  private static TurnParams computeDynamicTurnParams(
      AIContext ctx, KinematicsSnapshot snapshot, IntentResult intent) {
    double maxSpeed = Math.max(1.0e-6, snapshot.scaledMaxSpeed());
    double curSpeed = ctx.sword().getDeltaMovement().length();
    double sr = Math.max(0.0, Math.min(1.0, curSpeed / maxSpeed));

    double turnMin = 0.28;
    double turnMax = 0.45;
    double kpMin = 0.60;
    double kpMax = 0.95;
    double floorMin = 0.04;
    double floorMax = 0.08;
    double accelMin = 1.15;
    double accelMax = 1.35;

    double turnPerTick = lerp(turnMin, turnMax, sr);
    double headingKp = lerp(kpMin, kpMax, sr);
    double minFloor = lerp(floorMin, floorMax, sr);
    double accelMul = lerp(accelMin, accelMax, sr);

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
