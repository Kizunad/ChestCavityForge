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
 * 断阵（Breaker）：清理召唤物、护盾阵列或防御设施。
 *
 * <p><b>Phase 7: 软删除标记（Soft Deletion Mark）</b> - 扩展意图
 *
 * <p>本意图仅在 {@code ENABLE_EXTRA_INTENTS=true} 时实例化。 默认配置下不会被使用，降低 AI 决策复杂度。
 *
 * @see
 *     net.tigereye.chestcavity.compat.guzhenren.flyingsword.tuning.FlyingSwordTuning#ENABLE_EXTRA_INTENTS
 * @see net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.planner.IntentPlanner
 */
public final class BreakerIntent implements Intent {
  @Override
  public Optional<IntentResult> evaluate(AIContext ctx) {
    var sword = ctx.sword();
    LivingEntity target =
        TargetFinder.findBreakerTarget(
            sword, sword.position(), FlyingSwordAITuning.HUNT_SEARCH_RANGE);
    if (target == null) {
      return Optional.empty();
    }

    double distance = Math.max(1.0, sword.distanceTo(target));
    double armorScore = target.getArmorValue();
    double priority = 14.0 + armorScore + 4.0 / distance;

    return Optional.of(
        IntentResult.builder()
            .target(target)
            .trajectory(TrajectoryType.Sawtooth)
            .priority(priority)
            .param("breaker_distance", distance)
            .param("breaker_armor", armorScore)
            .build());
  }

  @Override
  public String name() {
    return "Breaker";
  }
}
