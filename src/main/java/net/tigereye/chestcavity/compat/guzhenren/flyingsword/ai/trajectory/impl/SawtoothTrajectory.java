package net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.trajectory.impl;

import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.AIContext;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.IntentResult;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.trajectory.Trajectory;

/**
 * 锯齿横切：以锯齿形态横扫目标区域。
 *
 * <p><b>Phase 7: 软删除标记（Soft Deletion Mark）</b> - 高级轨迹
 *
 * <p>本轨迹仅在 {@code ENABLE_ADVANCED_TRAJECTORIES=true} 时注册启用。 默认配置下不会被加载，实现零性能开销。
 *
 * @see
 *     net.tigereye.chestcavity.compat.guzhenren.flyingsword.tuning.FlyingSwordTuning#ENABLE_ADVANCED_TRAJECTORIES
 * @see net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.trajectory.Trajectories
 */
public final class SawtoothTrajectory implements Trajectory {
  @Override
  public Vec3 computeDesiredVelocity(AIContext ctx, IntentResult intent) {
    var sword = ctx.sword();
    Vec3 anchor = intent.getTargetPos().orElseGet(() -> sword.position());
    Vec3 direction = anchor.subtract(sword.position());
    if (direction.lengthSqr() < 1.0e-4) {
      direction = sword.getLookAngle();
    }
    Vec3 forward = direction.normalize();

    Vec3 lateral = new Vec3(-forward.z, 0, forward.x).normalize();
    double wave = (ctx.sword().tickCount % 6 - 3) * 0.4;
    Vec3 desired = forward.add(lateral.scale(wave)).normalize();

    double speed = sword.getSwordAttributes().speedMax * 0.9;
    return desired.scale(speed);
  }
}
