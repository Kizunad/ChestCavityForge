package net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.types;

import java.util.Optional;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.AIContext;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.Intent;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.IntentResult;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.trajectory.TrajectoryType;

/**
 * 巡域（Patrol）：沿域边巡航，维持对“剑域”边界的覆盖。
 */
public final class PatrolIntent implements Intent {
  private static final double DEFAULT_DOMAIN_RADIUS = 12.0;

  @Override
  public Optional<IntentResult> evaluate(AIContext ctx) {
    var owner = ctx.owner();
    Vec3 ownerPos = owner.position();
    double radius = DEFAULT_DOMAIN_RADIUS;
    double yawRad = Math.toRadians(owner.getYRot());

    Vec3 anchor = ownerPos.add(Math.cos(yawRad) * radius, 0.5, Math.sin(yawRad) * radius);

    return Optional.of(
        IntentResult.builder()
            .target(anchor)
            .trajectory(TrajectoryType.DomainEdgePatrol)
            .priority(0.22)
            .param("patrol_radius", radius)
            .param("patrol_owner_yaw", owner.getYRot())
            .build());
  }

  @Override
  public String name() { return "Patrol"; }
}

