package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.trajectory.impl;

import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.intent.AIContext;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.intent.IntentResult;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.trajectory.Trajectory;

/**
 * 远离→回返二段式（召回/回收）。此处作为简化：直接朝向 Owner 进行加速回收。
 */
public final class BoomerangTrajectory implements Trajectory {
  @Override
  public Vec3 computeDesiredVelocity(AIContext ctx, IntentResult intent) {
    var sword = ctx.sword();
    var ownerPos = ctx.owner().getEyePosition();
    var dir = ownerPos.subtract(sword.position());
    if (dir.lengthSqr() < 1.0e-4) return Vec3.ZERO;
    return dir.normalize().scale(sword.getSwordAttributes().speedMax * 1.8);
  }
}

