package net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.types;

import java.util.Optional;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.behavior.TargetFinder;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.AIContext;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.Intent;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.IntentResult;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.trajectory.TrajectoryType;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.tuning.FlyingSwordAITuning;

/** 牧群（Shepherd）：驱赶多目标至陷阱区或域边。 */
public final class ShepherdIntent implements Intent {
  @Override
  public Optional<IntentResult> evaluate(AIContext ctx) {
    Vec3 cluster = TargetFinder.estimateHostileClusterCenter(
        ctx.sword(), ctx.sword().position(), FlyingSwordAITuning.HUNT_SEARCH_RANGE);
    if (cluster == null) {
      return Optional.empty();
    }

    Vec3 pushVector = cluster.subtract(ctx.owner().position()).normalize();
    Vec3 shepherdAnchor = cluster.add(pushVector.scale(2.5));

    return Optional.of(
        IntentResult.builder()
            .target(shepherdAnchor)
            .trajectory(TrajectoryType.VortexOrbit)
            .priority(11.0)
            .param("shepherd_anchor_x", shepherdAnchor.x)
            .param("shepherd_anchor_z", shepherdAnchor.z)
            .build());
  }

  @Override
  public String name() { return "Shepherd"; }
}
