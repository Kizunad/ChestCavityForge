package net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.trajectory.impl;

import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.AIContext;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.IntentResult;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.trajectory.Trajectory;

/**
 * 螺旋逼近：围绕目标进行收缩式缠斗。
 *
 * <p><b>Phase 7: 软删除标记（Soft Deletion Mark）</b> - 高级轨迹
 *
 * <p>本轨迹仅在 {@code ENABLE_ADVANCED_TRAJECTORIES=true} 时注册启用。
 * 默认配置下不会被加载，实现零性能开销。
 *
 * @see net.tigereye.chestcavity.compat.guzhenren.flyingsword.tuning.FlyingSwordTuning#ENABLE_ADVANCED_TRAJECTORIES
 * @see net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.trajectory.Trajectories
 */
public final class CorkscrewTrajectory implements Trajectory {

  @Override
  public Vec3 computeDesiredVelocity(AIContext ctx, IntentResult intent) {
    var sword = ctx.sword();

    Vec3 targetPos = intent.getTargetEntity()
        .map(e -> e.position().add(0.0, e.getBbHeight() * 0.5, 0.0))
        .or(() -> intent.getTargetPos())
        .orElse(sword.position());

    Vec3 toTarget = targetPos.subtract(sword.position());
    double distance = toTarget.length();
    if (distance < 1.0e-3) {
      return Vec3.ZERO;
    }

    Vec3 forward = toTarget.normalize();

    Vec3 globalUp = new Vec3(0.0, 1.0, 0.0);
    Vec3 side = forward.cross(globalUp);
    if (side.lengthSqr() < 1.0e-4) {
      globalUp = new Vec3(1.0, 0.0, 0.0);
      side = forward.cross(globalUp);
    }
    side = side.normalize();
    Vec3 binormal = forward.cross(side).normalize();

    double tick = sword.tickCount;
    double angularSpeed = intent.getParams().getOrDefault("corkscrew_frequency", 0.32);
    double radiusBase = Math.min(4.0, Math.max(1.2, distance * 0.45));
    double radiusScale = intent.getParams().getOrDefault("corkscrew_radius", 1.0);
    double radius = Math.min(6.0, Math.max(0.8, radiusBase * radiusScale));
    double shrink = Math.max(0.45, Math.min(1.0, distance / 10.0));

    double sin = Math.sin(tick * angularSpeed);
    double cos = Math.cos(tick * angularSpeed);

    Vec3 spiralOffset = side.scale(sin * radius).add(binormal.scale(cos * radius * 0.6));
    Vec3 spiralTarget = targetPos.add(spiralOffset.scale(shrink));

    Vec3 desiredDir = spiralTarget.subtract(sword.position());
    if (desiredDir.lengthSqr() < 1.0e-4) {
      desiredDir = toTarget;
    }
    desiredDir = desiredDir.normalize();

    double baseSpeed = sword.getSwordAttributes().speedMax * 0.95;
    if (distance < 3.0) {
      baseSpeed = sword.getSwordAttributes().speedBase * 0.8;
      Vec3 tangent = binormal.cross(desiredDir).normalize();
      desiredDir = desiredDir.add(tangent.scale(0.18)).normalize();
    }
    double speedScale = intent.getParams().getOrDefault("speed_scale", 1.0);
    return desiredDir.scale(baseSpeed * Math.max(0.1, speedScale));
  }
}
