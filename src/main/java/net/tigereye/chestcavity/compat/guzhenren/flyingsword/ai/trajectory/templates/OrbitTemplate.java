package net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.trajectory.templates;

import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.AIContext;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.IntentResult;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.motion.KinematicsSnapshot;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.motion.SteeringCommand;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.motion.SteeringTemplate;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.tuning.FlyingSwordAITuning;

/**
 * 新版环绕模板：共用 SteeringCommand 输出。
 */
public final class OrbitTemplate implements SteeringTemplate {

  @Override
  public SteeringCommand compute(
      AIContext ctx, IntentResult intent, KinematicsSnapshot snapshot) {
    var sword = ctx.sword();
    var owner = ctx.owner();
    Vec3 ownerPos = owner.getEyePosition();
    Vec3 toOwner = ownerPos.subtract(sword.position());

    double distance = toOwner.length();
    double targetDistance = FlyingSwordAITuning.ORBIT_TARGET_DISTANCE;
    double tolerance = FlyingSwordAITuning.ORBIT_DISTANCE_TOLERANCE;

    Vec3 dir;
    double speedFactor;

    if (distance > targetDistance + tolerance) {
      dir = toOwner;
      speedFactor = FlyingSwordAITuning.ORBIT_APPROACH_SPEED_FACTOR;
    } else if (distance < targetDistance - tolerance) {
      dir = toOwner.scale(-1.0);
      speedFactor = FlyingSwordAITuning.ORBIT_RETREAT_SPEED_FACTOR;
    } else {
      Vec3 tangent = new Vec3(-toOwner.z, 0, toOwner.x).normalize();
      Vec3 radial = toOwner.normalize().scale(-FlyingSwordAITuning.ORBIT_RADIAL_PULL_IN);
      dir = tangent.add(radial);
      speedFactor = FlyingSwordAITuning.ORBIT_TANGENT_SPEED_FACTOR;
    }

    return SteeringCommand.of(dir, speedFactor);
  }
}
