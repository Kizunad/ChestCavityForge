package net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.trajectory.impl;

import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.AIContext;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.IntentResult;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.trajectory.Trajectory;

/**
 * 蛇形抖动：保持拉距同时横向摆动。
 *
 * <p><b>Phase 7: 软删除标记（Soft Deletion Mark）</b> - 高级轨迹
 *
 * <p>本轨迹仅在 {@code ENABLE_ADVANCED_TRAJECTORIES=true} 时注册启用。 默认配置下不会被加载，实现零性能开销。
 *
 * @see
 *     net.tigereye.chestcavity.compat.guzhenren.flyingsword.tuning.FlyingSwordTuning#ENABLE_ADVANCED_TRAJECTORIES
 * @see net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.trajectory.Trajectories
 */
public final class SerpentineTrajectory implements Trajectory {
  @Override
  public Vec3 computeDesiredVelocity(AIContext ctx, IntentResult intent) {
    var sword = ctx.sword();

    Vec3 targetPos =
        intent
            .getTargetEntity()
            .map(e -> e.position().add(0.0, e.getBbHeight() * 0.5, 0.0))
            .or(() -> intent.getTargetPos())
            .orElse(sword.position());

    Vec3 fromTarget = sword.position().subtract(targetPos);
    double distance = Math.max(1.0, fromTarget.length());
    Vec3 retreatDir = fromTarget.normalize();

    Vec3 up = new Vec3(0.0, 1.0, 0.0);
    Vec3 lateral = retreatDir.cross(up);
    if (lateral.lengthSqr() < 1.0e-4) {
      lateral = new Vec3(1.0, 0.0, 0.0);
    }
    lateral = lateral.normalize();

    double amplitude = intent.getParams().getOrDefault("serpentine_amplitude", 0.6);
    double freq = intent.getParams().getOrDefault("serpentine_frequency", 0.35);
    double oscillation = Math.sin(ctx.sword().tickCount * freq) * amplitude;
    Vec3 desiredDir = retreatDir.add(lateral.scale(oscillation)).normalize();

    double targetRadius = intent.getParams().getOrDefault("kiting_safe_radius", 7.5d);
    double bias = Math.max(0.0, targetRadius - distance) * 0.12;
    desiredDir = desiredDir.add(retreatDir.scale(bias)).normalize();

    double speed = sword.getSwordAttributes().speedBase * 1.1;
    double speedScale = intent.getParams().getOrDefault("speed_scale", 1.0);
    return desiredDir.scale(speed * Math.max(0.1, speedScale));
  }
}
