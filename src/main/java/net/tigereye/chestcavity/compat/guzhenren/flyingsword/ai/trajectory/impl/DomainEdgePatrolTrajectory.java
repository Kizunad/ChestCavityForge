package net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.trajectory.impl;

import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.AIContext;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.IntentResult;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.trajectory.Trajectory;

/**
 * 域边巡航：沿圆环匀速巡逻。
 *
 * <p><b>Phase 7: 软删除标记（Soft Deletion Mark）</b> - 高级轨迹
 *
 * <p>本轨迹仅在 {@code ENABLE_ADVANCED_TRAJECTORIES=true} 时注册启用。 默认配置下不会被加载，实现零性能开销。
 *
 * @see
 *     net.tigereye.chestcavity.compat.guzhenren.flyingsword.tuning.FlyingSwordTuning#ENABLE_ADVANCED_TRAJECTORIES
 * @see net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.trajectory.Trajectories
 */
public final class DomainEdgePatrolTrajectory implements Trajectory {
  @Override
  public Vec3 computeDesiredVelocity(AIContext ctx, IntentResult intent) {
    Vec3 anchor = intent.getTargetPos().orElse(ctx.owner().position());
    Vec3 offset = ctx.sword().position().subtract(anchor);
    if (offset.lengthSqr() < 1.0e-4) {
      offset = new Vec3(1, 0, 0);
    }

    Vec3 radial = offset.normalize();
    Vec3 tangent = new Vec3(-radial.z, 0, radial.x).normalize();
    double radius = intent.getParams().getOrDefault("patrol_radius", offset.length());

    Vec3 desired = tangent.add(radial.scale((radius - offset.length()) * 0.1));
    double speed = ctx.sword().getSwordAttributes().speedBase * 0.95;
    return desired.normalize().scale(speed);
  }
}
