package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.trajectory.impl;

import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.intent.AIContext;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.intent.IntentResult;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.trajectory.Trajectory;

/** 贴壁滑行：沿障碍物边缘滑行，降低碰撞风险。 */
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

