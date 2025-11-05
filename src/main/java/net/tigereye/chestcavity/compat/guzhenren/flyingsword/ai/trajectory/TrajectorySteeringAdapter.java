package net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.trajectory;

import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.AIContext;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.IntentResult;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.motion.KinematicsSnapshot;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.motion.SteeringCommand;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.motion.SteeringTemplate;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.motion.SteeringTemplate.SpeedUnit;

/**
 * 将旧的 {@link Trajectory} 适配到新的 {@link SteeringTemplate}。
 */
public final class TrajectorySteeringAdapter implements SteeringTemplate {

  private final Trajectory delegate;
  private final TrajectoryMeta meta;

  public TrajectorySteeringAdapter(Trajectory delegate, TrajectoryMeta meta) {
    this.delegate = delegate;
    this.meta = meta;
  }

  @Override
  public SteeringCommand compute(
      AIContext ctx, IntentResult intent, KinematicsSnapshot snapshot) {
    Vec3 desired = delegate.computeDesiredVelocity(ctx, intent);
    double reference =
        meta.speedUnit() == SpeedUnit.MAX
            ? snapshot.scaledMaxSpeed()
            : snapshot.scaledBaseSpeed();
    double speedScale = desired.length() / Math.max(1.0e-6, reference);

    SteeringCommand cmd = SteeringCommand.of(desired, speedScale);
    if (meta.accelOverride() != null) {
      cmd = cmd.withAccelFactor(meta.accelOverride());
    }
    if (meta.maxTurnOverride() != null) {
      cmd = cmd.withTurnOverride(meta.maxTurnOverride());
    }
    if (!meta.enableSeparation()) {
      cmd = cmd.disableSeparation();
    }
    return cmd;
  }

  @Override
  public SpeedUnit speedUnit() {
    return meta.speedUnit();
  }

  @Override
  public boolean enableSeparation() {
    return meta.enableSeparation();
  }

  @Override
  public Double maxTurnRadiansOverride() {
    return meta.maxTurnOverride();
  }
}
