package net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.trajectory.impl;

import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.AIContext;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.IntentResult;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.trajectory.Trajectory;

/** 曲率受限的拦截弧，兼顾提前量与平滑转向。 */
public final class CurvedInterceptTrajectory implements Trajectory {
  @Override
  public Vec3 computeDesiredVelocity(AIContext ctx, IntentResult intent) {
    var sword = ctx.sword();

    Vec3 intercept = intent.getTargetEntity()
        .map(e -> e.position().add(e.getDeltaMovement().normalize().scale(0.5)))
        .or(() -> intent.getTargetPos())
        .orElse(sword.position());

    Vec3 toIntercept = intercept.subtract(sword.position());
    double distance = toIntercept.length();
    if (distance < 1.0e-4) {
      return Vec3.ZERO;
    }

    Vec3 forward = toIntercept.normalize();
    Vec3 currentVel = sword.getDeltaMovement();

    Vec3 blended = forward;
    if (currentVel.lengthSqr() > 1.0e-4) {
      Vec3 currentDir = currentVel.normalize();
      blended = currentDir.scale(0.4).add(forward.scale(0.6)).normalize();
    }

    Vec3 up = new Vec3(0.0, 1.0, 0.0);
    Vec3 lateral = blended.cross(up);
    if (lateral.lengthSqr() < 1.0e-6) {
      lateral = new Vec3(1.0, 0.0, 0.0);
    }
    lateral = lateral.normalize();

    double baseCurvature = Math.min(0.4, Math.max(0.12, 2.0 / Math.max(1.0, distance)));
    double curvature = baseCurvature;
    double curvatureScale = intent.getParams().getOrDefault("curvature_scale", 1.0);
    curvature = Math.min(0.6, Math.max(0.05, curvature * curvatureScale));
    double sweep = Math.sin(ctx.sword().tickCount * 0.4) * curvature;
    Vec3 desired = blended.add(lateral.scale(sweep)).normalize();

    double speed = sword.getSwordAttributes().speedMax;
    double speedScale = intent.getParams().getOrDefault("speed_scale", 1.0);
    return desired.scale(speed * Math.max(0.1, speedScale));
  }
}
