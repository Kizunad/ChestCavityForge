package net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.trajectory.impl;

import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.AIContext;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.IntentResult;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.trajectory.Trajectory;

/**
 * 贴壁滑行：沿障碍物边缘滑行，降低碰撞风险。
 *
 * <p><b>Phase 7: 软删除标记（Soft Deletion Mark）</b> - 高级轨迹
 *
 * <p>本轨迹仅在 {@code ENABLE_ADVANCED_TRAJECTORIES=true} 时注册启用。 默认配置下不会被加载，实现零性能开销。
 *
 * @see
 *     net.tigereye.chestcavity.compat.guzhenren.flyingsword.tuning.FlyingSwordTuning#ENABLE_ADVANCED_TRAJECTORIES
 * @see net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.trajectory.Trajectories
 */
public final class WallGlideTrajectory implements Trajectory {
  @Override
  public Vec3 computeDesiredVelocity(AIContext ctx, IntentResult intent) {
    var sword = ctx.sword();
    Vec3 target = intent.getTargetPos().orElseGet(() -> ctx.owner().position());
    Vec3 direction = target.subtract(sword.position());
    if (direction.lengthSqr() < 1.0e-4) {
      direction = sword.getLookAngle();
    }

    Vec3 forward = direction.normalize();
    Vec3 vertical = new Vec3(0.0, 0.25, 0.0);
    Vec3 glide = forward.add(vertical);

    double tick = ctx.sword().tickCount;
    double sway = Math.sin(tick * 0.2) * 0.2;
    Vec3 side = new Vec3(-forward.z, 0, forward.x).normalize();
    glide = glide.add(side.scale(sway));

    double speed = sword.getSwordAttributes().speedBase;
    return glide.normalize().scale(speed);
  }
}
