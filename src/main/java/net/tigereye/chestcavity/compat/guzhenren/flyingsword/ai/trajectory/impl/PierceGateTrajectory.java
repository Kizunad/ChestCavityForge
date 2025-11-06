package net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.trajectory.impl;

import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.AIContext;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.IntentResult;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.trajectory.Trajectory;

/**
 * 穿门直刺：直线突进，穿越裂隙锚点。
 *
 * <p><b>Phase 7: 软删除标记（Soft Deletion Mark）</b> - 高级轨迹
 *
 * <p>本轨迹仅在 {@code ENABLE_ADVANCED_TRAJECTORIES=true} 时注册启用。
 * 默认配置下不会被加载，实现零性能开销。
 *
 * @see net.tigereye.chestcavity.compat.guzhenren.flyingsword.tuning.FlyingSwordTuning#ENABLE_ADVANCED_TRAJECTORIES
 * @see net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.trajectory.Trajectories
 */
public final class PierceGateTrajectory implements Trajectory {
  @Override
  public Vec3 computeDesiredVelocity(AIContext ctx, IntentResult intent) {
    Vec3 target = intent.getTargetEntity()
        .map(e -> e.position())
        .or(() -> intent.getTargetPos())
        .orElse(ctx.owner().position());

    Vec3 direction = target.subtract(ctx.sword().position());
    if (direction.lengthSqr() < 1.0e-4) {
      return Vec3.ZERO;
    }

    double speed = ctx.sword().getSwordAttributes().speedMax * 1.3;
    return direction.normalize().scale(speed);
  }
}

