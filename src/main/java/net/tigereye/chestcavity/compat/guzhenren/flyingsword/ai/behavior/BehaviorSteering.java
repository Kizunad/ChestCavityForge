package net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.behavior;

import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.FlyingSwordEntity;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.motion.KinematicsSnapshot;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.motion.LegacySteeringAdapter;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.motion.SteeringCommand;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.motion.SteeringOps;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.behavior.SeparationBehavior;

/**
 * 行为层共用的转向提交工具，统一走 SteeringOps 流程。
 */
public final class BehaviorSteering {

  private BehaviorSteering() {}

  public static void commit(FlyingSwordEntity sword, Vec3 desiredVelocity) {
    commit(sword, desiredVelocity, true);
  }

  public static void commit(
      FlyingSwordEntity sword, Vec3 desiredVelocity, boolean applySeparation) {
    Vec3 processed = desiredVelocity;
    if (applySeparation) {
      processed = SeparationBehavior.applySeparation(sword, processed);
    }

    var snapshot = KinematicsSnapshot.capture(sword);
    SteeringCommand command = LegacySteeringAdapter.fromDesiredVelocity(processed, snapshot);
    Vec3 newVelocity = SteeringOps.computeNewVelocity(sword, command, snapshot);
    sword.setDeltaMovement(newVelocity);
  }
}
