package net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.trajectory.impl;

import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.AIContext;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.IntentResult;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.trajectory.Trajectory;

/**
 * 影步：短距高速冲刺，模拟瞬移。
 *
 * <p><b>Phase 7: 软删除标记（Soft Deletion Mark）</b> - 高级轨迹
 *
 * <p>本轨迹仅在 {@code ENABLE_ADVANCED_TRAJECTORIES=true} 时注册启用。
 * 默认配置下不会被加载，实现零性能开销。
 *
 * @see net.tigereye.chestcavity.compat.guzhenren.flyingsword.tuning.FlyingSwordTuning#ENABLE_ADVANCED_TRAJECTORIES
 * @see net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.trajectory.Trajectories
 */
public final class ShadowStepTrajectory implements Trajectory {
  @Override
  public Vec3 computeDesiredVelocity(AIContext ctx, IntentResult intent) {
    Vec3 target = intent.getTargetEntity()
        .map(e -> e.position().add(0.0, e.getBbHeight() * 0.4, 0.0))
        .or(() -> intent.getTargetPos())
        .orElse(ctx.sword().position());

    Vec3 dash = target.subtract(ctx.sword().position());
    if (dash.lengthSqr() < 1.0e-4) {
      return Vec3.ZERO;
    }

    double speed = ctx.sword().getSwordAttributes().speedMax * 1.6;
    return dash.normalize().scale(speed);
  }
}

