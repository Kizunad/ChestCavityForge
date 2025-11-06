package net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.trajectory.impl;

import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.AIContext;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.IntentResult;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.trajectory.Trajectory;

/**
 * 涡旋收缩绕背：逐渐降低半径并施加垂直摆动。
 *
 * <p><b>Phase 7: 软删除标记（Soft Deletion Mark）</b> - 高级轨迹
 *
 * <p>本轨迹仅在 {@code ENABLE_ADVANCED_TRAJECTORIES=true} 时注册启用。
 * 默认配置下不会被加载，实现零性能开销。
 *
 * @see net.tigereye.chestcavity.compat.guzhenren.flyingsword.tuning.FlyingSwordTuning#ENABLE_ADVANCED_TRAJECTORIES
 * @see net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.trajectory.Trajectories
 */
public final class VortexOrbitTrajectory implements Trajectory {
  @Override
  public Vec3 computeDesiredVelocity(AIContext ctx, IntentResult intent) {
    var sword = ctx.sword();
    Vec3 target = intent.getTargetEntity()
        .map(e -> e.position().add(0.0, e.getBbHeight() * 0.5, 0.0))
        .or(() -> intent.getTargetPos())
        .orElse(sword.position());

    Vec3 offset = sword.position().subtract(target);
    double minRadius = intent.getParams().getOrDefault("orbit_radius", 0.8);
    double distance = Math.max(minRadius, offset.length());
    Vec3 radial = offset.normalize();
    Vec3 tangent = new Vec3(-radial.z, 0, radial.x).normalize();

    double shrinkRate = Math.min(0.25, 2.5 / distance);
    double shrinkScale = intent.getParams().getOrDefault("orbit_shrink", 1.0);
    shrinkRate = Math.min(0.4, Math.max(0.05, shrinkRate * shrinkScale));
    Vec3 inward = radial.scale(-shrinkRate);

    double vertical = Math.sin(ctx.sword().tickCount * 0.45) * 0.4;
    Vec3 verticalVec = new Vec3(0.0, vertical, 0.0);

    Vec3 desired = tangent.add(inward).add(verticalVec);
    if (desired.lengthSqr() < 1.0e-4) {
      desired = tangent;
    }

    double speed = sword.getSwordAttributes().speedBase * 1.15;
    double speedScale = intent.getParams().getOrDefault("speed_scale", 1.0);
    return desired.normalize().scale(speed * Math.max(0.1, speedScale));
  }
}
