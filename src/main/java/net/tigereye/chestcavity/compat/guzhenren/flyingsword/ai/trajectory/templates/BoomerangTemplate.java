package net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.trajectory.templates;

import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.AIContext;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.IntentResult;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.motion.KinematicsSnapshot;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.motion.SteeringCommand;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.motion.SteeringTemplate;

/** 召回血线：高速直线回到主人。 */
public final class BoomerangTemplate implements SteeringTemplate {

  @Override
  public SteeringCommand compute(AIContext ctx, IntentResult intent, KinematicsSnapshot snapshot) {
    Vec3 ownerPos = ctx.owner().getEyePosition();
    Vec3 dir = ownerPos.subtract(ctx.sword().position());
    if (dir.lengthSqr() < 1.0e-6) {
      return SteeringCommand.of(Vec3.ZERO, 0.0);
    }

    double baseSpeed = snapshot.scaledBaseSpeed();
    double maxSpeed = snapshot.scaledMaxSpeed();
    double scale = (maxSpeed * 1.8) / Math.max(1.0e-6, baseSpeed);

    return SteeringCommand.of(dir, scale)
        .withDesiredMaxFactor(1.8)
        .withAccelFactor(1.4)
        .disableSeparation();
  }

  @Override
  public SpeedUnit speedUnit() {
    return SpeedUnit.BASE;
  }

  @Override
  public boolean enableSeparation() {
    return false;
  }
}
