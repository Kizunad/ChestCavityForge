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

/**
 * 诱饵（Decoy）：快速切入仇恨最高者侧后方，制造转向机会。
 *
 * <p><b>Phase 7: 软删除标记（Soft Deletion Mark）</b> - 扩展意图
 *
 * <p>本意图仅在 {@code ENABLE_EXTRA_INTENTS=true} 时实例化。 默认配置下不会被使用，降低 AI 决策复杂度。
 *
 * @see
 *     net.tigereye.chestcavity.compat.guzhenren.flyingsword.tuning.FlyingSwordTuning#ENABLE_EXTRA_INTENTS
 * @see net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.planner.IntentPlanner
 */
public final class DecoyIntent implements Intent {
  @Override
  public Optional<IntentResult> evaluate(AIContext ctx) {
    var sword = ctx.sword();
    LivingEntity target =
        TargetFinder.findHighThreatMelee(
            sword, sword.position(), FlyingSwordAITuning.HUNT_SEARCH_RANGE);
    if (target == null) {
      target =
          TargetFinder.findMarkedTarget(
              sword, sword.position(), FlyingSwordAITuning.HUNT_SEARCH_RANGE);
      if (target == null) {
        return Optional.empty();
      }
    }

    Vec3 targetPos = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
    Vec3 forward = target.getLookAngle().normalize();
    Vec3 side = new Vec3(-forward.z, 0, forward.x).normalize();
    Vec3 anchor = targetPos.add(side.scale(1.5)).add(forward.scale(-1.2));

    double distance = Math.max(1.0, sword.position().distanceTo(anchor));
    double priority = 13.0 + 6.0 / distance;

    return Optional.of(
        IntentResult.builder()
            .target(anchor)
            .trajectory(TrajectoryType.Ricochet)
            .priority(priority)
            .param("decoy_distance", distance)
            .build());
  }

  @Override
  public String name() {
    return "Decoy";
  }
}
