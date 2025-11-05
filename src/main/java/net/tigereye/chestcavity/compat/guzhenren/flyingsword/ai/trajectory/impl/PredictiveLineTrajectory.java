package net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.trajectory.impl;

import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.AIContext;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.IntentResult;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.trajectory.Trajectory;

/**
 * 基于目标速度的提前量直线拦截（简化版）。
 */
public final class PredictiveLineTrajectory implements Trajectory {
  @Override
  public Vec3 computeDesiredVelocity(AIContext ctx, IntentResult intent) {
    var sword = ctx.sword();
    var from = sword.position();

    Vec3 targetPos = intent.getTargetEntity().map(e -> e.getEyePosition())
        .or(() -> intent.getTargetPos())
        .orElse(from);

    Vec3 targetVel = intent.getTargetEntity().map(e -> e.getDeltaMovement()).orElse(Vec3.ZERO);

    double speed = sword.getSwordAttributes().speedMax;
    double leadT = Math.max(0.0, Math.min(0.6, targetVel.length() / Math.max(0.001, speed)));
    double overrideLead = intent.getParams().getOrDefault("lead_time", Double.NaN);
    if (Double.isFinite(overrideLead)) {
      leadT = Math.max(0.0, overrideLead);
    }

    Vec3 aimPoint = targetPos.add(targetVel.scale(leadT * 10.0));
    Vec3 dir = aimPoint.subtract(from).normalize();
    double speedScale = intent.getParams().getOrDefault("speed_scale", 1.0);
    return dir.scale(speed * Math.max(0.1, speedScale));
  }
}
