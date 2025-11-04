package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.trajectory.impl;

import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.intent.AIContext;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.intent.IntentResult;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.trajectory.Trajectory;

/** 反弹线：模拟亮刃摆动的短时折返。 */
public final class RicochetTrajectory implements Trajectory {
  @Override
  public Vec3 computeDesiredVelocity(AIContext ctx, IntentResult intent) {
    Vec3 target = intent.getTargetPos().orElse(ctx.owner().position());
    Vec3 toTarget = target.subtract(ctx.sword().position());
    if (toTarget.lengthSqr() < 1.0e-4) {
      toTarget = ctx.owner().getLookAngle();
    }

    Vec3 forward = toTarget.normalize();
    Vec3 side = new Vec3(-forward.z, 0, forward.x).normalize();
    double phase = ctx.sword().tickCount % 8;
    double flip = phase < 4 ? 1.0 : -1.0;
    Vec3 desired = forward.scale(0.6).add(side.scale(0.8 * flip));

    double speed = ctx.sword().getSwordAttributes().speedBase * 1.2;
    return desired.normalize().scale(speed);
  }
}

