package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.trajectory.impl;

import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.intent.AIContext;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.intent.IntentResult;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.trajectory.Trajectory;

/** 双螺旋：与其他飞剑协同的对称螺旋。 */
public final class HelixPairTrajectory implements Trajectory {
  @Override
  public Vec3 computeDesiredVelocity(AIContext ctx, IntentResult intent) {
    Vec3 target = intent.getTargetEntity()
        .map(e -> e.position().add(0.0, e.getBbHeight() * 0.6, 0.0))
        .or(() -> intent.getTargetPos())
        .orElse(ctx.sword().position());

    Vec3 offset = ctx.sword().position().subtract(target);
    double distance = Math.max(0.8, offset.length());
    Vec3 radial = offset.normalize();
    Vec3 tangent = new Vec3(-radial.z, 0, radial.x).normalize();

    double spin = ctx.sword().tickCount * 0.45;
    double polarity = (ctx.sword().getUUID().getLeastSignificantBits() & 1L) == 0L ? 1.0 : -1.0;
    Vec3 helix = tangent.scale(polarity).add(radial.scale(-0.3)).add(new Vec3(0, Math.cos(spin) * 0.3, 0));

    double speed = ctx.sword().getSwordAttributes().speedMax * 1.05;
    return helix.normalize().scale(speed);
  }
}

