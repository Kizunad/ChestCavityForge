package net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.trajectory.impl;

import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.AIContext;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.IntentResult;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.trajectory.Trajectory;

/**
 * 三阶贝塞尔 S 型穿刺。
 *
 * <p><b>Phase 7: 软删除标记（Soft Deletion Mark）</b> - 高级轨迹
 *
 * <p>本轨迹仅在 {@code ENABLE_ADVANCED_TRAJECTORIES=true} 时注册启用。 默认配置下不会被加载，实现零性能开销。
 *
 * @see
 *     net.tigereye.chestcavity.compat.guzhenren.flyingsword.tuning.FlyingSwordTuning#ENABLE_ADVANCED_TRAJECTORIES
 * @see net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.trajectory.Trajectories
 */
public final class BezierSTrajectory implements Trajectory {
  @Override
  public Vec3 computeDesiredVelocity(AIContext ctx, IntentResult intent) {
    var sword = ctx.sword();
    Vec3 start = sword.position();
    Vec3 end =
        intent
            .getTargetEntity()
            .map(e -> e.position().add(0.0, e.getBbHeight() * 0.4, 0.0))
            .or(() -> intent.getTargetPos())
            .orElse(start);

    Vec3 direction = end.subtract(start);
    double distance = direction.length();
    if (distance < 1.0e-3) {
      return Vec3.ZERO;
    }
    Vec3 forward = direction.normalize();

    Vec3 up = new Vec3(0.0, 1.0, 0.0);
    Vec3 side = forward.cross(up);
    if (side.lengthSqr() < 1.0e-4) {
      up = new Vec3(1.0, 0.0, 0.0);
      side = forward.cross(up);
    }
    side = side.normalize();

    double arcHeight = Math.min(3.0, distance * 0.35);
    double lateral = Math.sin(ctx.sword().tickCount * 0.15) * distance * 0.2;

    Vec3 control1 = start.add(forward.scale(distance * 0.35)).add(up.scale(arcHeight));
    Vec3 control2 =
        start
            .add(forward.scale(distance * 0.65))
            .add(side.scale(lateral))
            .add(up.scale(-arcHeight * 0.5));

    double t = Math.min(0.92, 0.4 + ctx.sword().tickCount % 12 * 0.05);
    double oneMinusT = 1.0 - t;

    Vec3 bezierPoint =
        start
            .scale(oneMinusT * oneMinusT * oneMinusT)
            .add(control1.scale(3 * oneMinusT * oneMinusT * t))
            .add(control2.scale(3 * oneMinusT * t * t))
            .add(end.scale(t * t * t));

    Vec3 desired = bezierPoint.subtract(start);
    if (desired.lengthSqr() < 1.0e-4) {
      desired = direction;
    }

    double speed = sword.getSwordAttributes().speedMax * 0.95;
    return desired.normalize().scale(speed);
  }
}
