package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.trajectory.impl;

import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.intent.AIContext;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.intent.IntentResult;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.trajectory.Trajectory;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.tuning.FlyingSwordAITuning;

/**
 * 简化的环绕轨迹：基于 Owner 与当前相对向量生成切向速度。
 */
public final class OrbitTrajectory implements Trajectory {
  @Override
  public Vec3 computeDesiredVelocity(AIContext ctx, IntentResult intent) {
    var sword = ctx.sword();
    var owner = ctx.owner();
    Vec3 ownerPos = owner.getEyePosition();
    Vec3 toOwner = ownerPos.subtract(sword.position());

    double distance = toOwner.length();
    double targetDistance = FlyingSwordAITuning.ORBIT_TARGET_DISTANCE;
    double tolerance = FlyingSwordAITuning.ORBIT_DISTANCE_TOLERANCE;

    if (distance > targetDistance + tolerance) {
      return toOwner.normalize().scale(sword.getSwordAttributes().speedBase * FlyingSwordAITuning.ORBIT_APPROACH_SPEED_FACTOR);
    } else if (distance < targetDistance - tolerance) {
      return toOwner.normalize().scale(-sword.getSwordAttributes().speedBase * FlyingSwordAITuning.ORBIT_RETREAT_SPEED_FACTOR);
    }

    Vec3 tangent = new Vec3(-toOwner.z, 0, toOwner.x).normalize();
    Vec3 radial = toOwner.normalize().scale(-FlyingSwordAITuning.ORBIT_RADIAL_PULL_IN);
    return tangent.add(radial).normalize().scale(
        sword.getSwordAttributes().speedBase * FlyingSwordAITuning.ORBIT_TANGENT_SPEED_FACTOR);
  }
}

