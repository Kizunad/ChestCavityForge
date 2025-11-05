package net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.types;

import java.util.Optional;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.behavior.TargetFinder;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.AIContext;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.Intent;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.IntentResult;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.trajectory.TrajectoryType;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.tuning.FlyingSwordAITuning;

/** 穿域（Pivot）：从域心突进至外域威胁再迅速回程。 */
public final class PivotIntent implements Intent {
  private static final double INNER_RADIUS = 10.0;

  @Override
  public Optional<IntentResult> evaluate(AIContext ctx) {
    var sword = ctx.sword();
    LivingEntity target = TargetFinder.findOuterThreat(
        sword, ctx.owner().position(), INNER_RADIUS, FlyingSwordAITuning.HUNT_SEARCH_RANGE * 1.5);
    if (target == null) {
      return Optional.empty();
    }

    Vec3 entry = ctx.owner().position();
    Vec3 exit = target.position();
    double distance = entry.distanceTo(exit);
    double priority = 17.0 + distance * 0.2;

    return Optional.of(
        IntentResult.builder()
            .target(target)
            .trajectory(TrajectoryType.PierceGate)
            .priority(priority)
            .param("pivot_distance", distance)
            .build());
  }

  @Override
  public String name() { return "Pivot"; }
}
