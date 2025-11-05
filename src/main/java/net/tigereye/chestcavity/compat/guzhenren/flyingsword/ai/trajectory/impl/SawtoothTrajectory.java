package net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.trajectory.impl;

import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.AIContext;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.IntentResult;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.trajectory.Trajectory;

/** 锯齿横切：以锯齿形态横扫目标区域。 */
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

