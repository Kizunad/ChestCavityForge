package net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.types;

import java.util.Optional;
import net.minecraft.world.entity.LivingEntity;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.behavior.TargetFinder;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.AIContext;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.Intent;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.IntentResult;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.trajectory.TrajectoryType;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.tuning.FlyingSwordAITuning;

/**
 * 猎首（Assassin）：优先针对低血量/高威胁目标。
 */
public final class AssassinIntent implements Intent {
  @Override
  public Optional<IntentResult> evaluate(AIContext ctx) {
    var sword = ctx.sword();

    LivingEntity target = TargetFinder.findLowestHealthHostile(
        sword, sword.position(), FlyingSwordAITuning.HUNT_SEARCH_RANGE);
    if (target == null) {
      return Optional.empty();
    }

    double maxHealth = Math.max(1.0, target.getMaxHealth());
    double healthRatio = Math.min(1.0, target.getHealth() / maxHealth);
    double distance = Math.max(1.0, sword.distanceTo(target));

    double lowHealthScore = (1.0 - healthRatio) * 12.0;
    double proximityScore = 3.0 / distance;
    double priority = lowHealthScore + proximityScore;

    return Optional.of(
        IntentResult.builder()
            .target(target)
            .trajectory(TrajectoryType.PredictiveLine)
            .priority(priority)
            .param("assassin_health_ratio", healthRatio)
            .param("assassin_distance", distance)
            .build());
  }

  @Override
  public String name() { return "Assassin"; }
}
