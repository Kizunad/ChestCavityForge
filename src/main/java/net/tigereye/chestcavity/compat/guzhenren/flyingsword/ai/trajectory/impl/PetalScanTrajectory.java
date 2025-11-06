package net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.trajectory.impl;

import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.AIContext;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.IntentResult;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.trajectory.Trajectory;

/**
 * 花瓣式扫描：围绕中心做渐进花瓣摆动，用于搜索目标。
 *
 * <p><b>Phase 7: 软删除标记（Soft Deletion Mark）</b> - 高级轨迹
 *
 * <p>本轨迹仅在 {@code ENABLE_ADVANCED_TRAJECTORIES=true} 时注册启用。
 * 默认配置下不会被加载，实现零性能开销。
 *
 * @see net.tigereye.chestcavity.compat.guzhenren.flyingsword.tuning.FlyingSwordTuning#ENABLE_ADVANCED_TRAJECTORIES
 * @see net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.trajectory.Trajectories
 */
public final class PetalScanTrajectory implements Trajectory {
  @Override
  public Vec3 computeDesiredVelocity(AIContext ctx, IntentResult intent) {
    Vec3 origin = intent.getTargetPos().orElse(ctx.owner().position());
    Vec3 pos = ctx.sword().position();

    Vec3 toOrigin = origin.subtract(pos);
    Vec3 radial = toOrigin.normalize();
    Vec3 tangent = new Vec3(-radial.z, 0, radial.x).normalize();

    double time = ctx.sword().tickCount * 0.25;
    double petalFactor = Math.sin(time) * 0.8;
    Vec3 desiredDir = radial.scale(0.35).add(tangent.scale(1.0 + petalFactor));
    if (desiredDir.lengthSqr() < 1.0e-4) {
      desiredDir = tangent;
    }

    double speed = ctx.sword().getSwordAttributes().speedBase * 0.9;
    return desiredDir.normalize().scale(speed);
  }
}

