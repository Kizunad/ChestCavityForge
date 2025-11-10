package net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.types;

import java.util.Optional;
import net.minecraft.world.entity.LivingEntity;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.behavior.TargetFinder;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.AIContext;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.Intent;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.IntentResult;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.trajectory.TrajectoryType;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.tuning.FlyingSwordAITuning;

/** 缠斗（Duel）：针对指定标记目标，保持胶着并持续施压。 */
public final class DuelIntent implements Intent {
  @Override
  public Optional<IntentResult> evaluate(AIContext ctx) {
    var sword = ctx.sword();
    LivingEntity target =
        TargetFinder.findMarkedTarget(
            sword, sword.position(), FlyingSwordAITuning.HUNT_SEARCH_RANGE);
    if (target == null) {
      return Optional.empty();
    }

    double distance = Math.max(1.0, sword.distanceTo(target));
    double healthRatio = Math.min(1.0, target.getHealth() / Math.max(1.0f, target.getMaxHealth()));
    double aggressionFactor = 1.0 - healthRatio; // 敌方越虚弱越容易被压制

    double basePriority = 18.0 + (aggressionFactor * 6.0) + (2.0 / distance);

    return Optional.of(
        IntentResult.builder()
            .target(target)
            .trajectory(TrajectoryType.Corkscrew)
            .priority(basePriority)
            .param("duel_distance", distance)
            .param("duel_health_ratio", healthRatio)
            .build());
  }

  @Override
  public String name() {
    return "Duel";
  }
}
