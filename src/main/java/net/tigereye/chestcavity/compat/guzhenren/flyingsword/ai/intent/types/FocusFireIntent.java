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
 * 协同（FocusFire）：针对队伍标记的目标，统一入射角。
 *
 * <p><b>Phase 7: 软删除标记（Soft Deletion Mark）</b> - 扩展意图
 *
 * <p>本意图仅在 {@code ENABLE_EXTRA_INTENTS=true} 时实例化。
 * 默认配置下不会被使用，降低 AI 决策复杂度。
 *
 * @see net.tigereye.chestcavity.compat.guzhenren.flyingsword.tuning.FlyingSwordTuning#ENABLE_EXTRA_INTENTS
 * @see net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.planner.IntentPlanner
 */
public final class FocusFireIntent implements Intent {
  @Override
  public Optional<IntentResult> evaluate(AIContext ctx) {
    LivingEntity target = TargetFinder.findMarkedTarget(
        ctx.sword(), ctx.sword().position(), FlyingSwordAITuning.HUNT_SEARCH_RANGE);
    if (target == null) {
      target = TargetFinder.findLowestHealthHostile(
          ctx.sword(), ctx.sword().position(), FlyingSwordAITuning.HUNT_SEARCH_RANGE);
      if (target == null) {
        return Optional.empty();
      }
    }

    double distance = Math.max(1.0, ctx.sword().distanceTo(target));
    double priority = 16.0 + 5.0 / distance;

    return Optional.of(
        IntentResult.builder()
            .target(target)
            .trajectory(TrajectoryType.HelixPair)
            .priority(priority)
            .param("focusfire_distance", distance)
            .build());
  }

  @Override
  public String name() { return "FocusFire"; }
}
