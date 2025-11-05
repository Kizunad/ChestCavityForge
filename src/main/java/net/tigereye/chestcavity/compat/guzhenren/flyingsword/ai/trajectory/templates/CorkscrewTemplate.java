package net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.trajectory.templates;

import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.AIContext;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.IntentResult;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.motion.KinematicsOps;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.motion.KinematicsSnapshot;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.motion.SteeringCommand;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.motion.SteeringTemplate;

/**
 * 螺旋逼近模板：根据目标距离调整半径与速度。
 */
public final class CorkscrewTemplate implements SteeringTemplate {

  @Override
  public SteeringCommand compute(
      AIContext ctx, IntentResult intent, KinematicsSnapshot snapshot) {
    Vec3 targetPos =
        intent
            .getTargetEntity()
            .map(e -> e.position().add(0.0, e.getBbHeight() * 0.5, 0.0))
            .or(() -> intent.getTargetPos())
            .orElse(ctx.sword().position());

    Vec3 toTarget = targetPos.subtract(ctx.sword().position());
    double distance = toTarget.length();
    if (distance < 1.0e-6) {
      return SteeringCommand.of(Vec3.ZERO, 0.0);
    }

    Vec3 forward = KinematicsOps.normaliseSafe(toTarget);
    Vec3 globalUp = new Vec3(0.0, 1.0, 0.0);
    Vec3 side = forward.cross(globalUp);
    if (side.lengthSqr() < 1.0e-6) {
      globalUp = new Vec3(1.0, 0.0, 0.0);
      side = forward.cross(globalUp);
    }
    side = side.normalize();
    Vec3 binormal = forward.cross(side).normalize();

    double tick = ctx.sword().tickCount;
    double angularSpeed = intent.getParams().getOrDefault("corkscrew_frequency", 0.32);
    double radiusBase = Math.min(4.0, Math.max(1.2, distance * 0.45));
    double radiusScale = intent.getParams().getOrDefault("corkscrew_radius", 1.0);
    double radius = Math.min(6.0, Math.max(0.8, radiusBase * radiusScale));
    double shrink = Math.max(0.45, Math.min(1.0, distance / 10.0));

    double sin = Math.sin(tick * angularSpeed);
    double cos = Math.cos(tick * angularSpeed);

    Vec3 spiralOffset = side.scale(sin * radius).add(binormal.scale(cos * radius * 0.6));
    Vec3 spiralTarget = targetPos.add(spiralOffset.scale(shrink));

    Vec3 desiredDir = spiralTarget.subtract(ctx.sword().position());
    if (desiredDir.lengthSqr() < 1.0e-6) {
      desiredDir = toTarget;
    }
    desiredDir = desiredDir.normalize();

    if (distance < 3.0) {
      Vec3 tangent = binormal.cross(desiredDir).normalize();
      desiredDir = desiredDir.add(tangent.scale(0.18)).normalize();
    }

    double baseSpeed = snapshot.scaledBaseSpeed();
    double maxSpeed = snapshot.scaledMaxSpeed();
    double speedScaleParam = intent.getParams().getOrDefault("speed_scale", 1.0);

    double desiredSpeed;
    if (distance < 3.0) {
      desiredSpeed = baseSpeed * 0.8;
    } else {
      desiredSpeed = maxSpeed * 0.95;
    }
    desiredSpeed *= Math.max(0.1, speedScaleParam);

    double scale = desiredSpeed / Math.max(1.0e-6, baseSpeed);

    SteeringCommand command = SteeringCommand.of(desiredDir, scale);
    if (distance < 3.0) {
      command = command.withDesiredMaxFactor(1.05);
    } else {
      command = command.withDesiredMaxFactor(1.2).withAccelFactor(1.15);
    }
    return command;
  }

  @Override
  public SpeedUnit speedUnit() {
    return SpeedUnit.BASE;
  }
}
