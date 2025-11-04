package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.intent.types;

import java.util.Optional;
import net.minecraft.world.entity.LivingEntity;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.behavior.TargetFinder;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.intent.AIContext;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.intent.Intent;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.intent.IntentResult;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.trajectory.TrajectoryType;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.tuning.FlyingSwordAITuning;

/**
 * 拦截（Intercept）：对高速/飞行/冲锋单位进行提前量拦截（占位：使用与 Guard 相同目标）。
 */
public final class InterceptIntent implements Intent {
  @Override
  public Optional<IntentResult> evaluate(AIContext ctx) {
    var sword = ctx.sword();
    var candidate = TargetFinder.findInterceptCandidate(
        sword, sword.position(), FlyingSwordAITuning.HUNT_SEARCH_RANGE);
    if (candidate == null) {
      return Optional.empty();
    }

    IntentResult.Builder builder = IntentResult.builder()
        .trajectory(TrajectoryType.CurvedIntercept)
        .priority(22.0 + candidate.speed() * 6.0);

    LivingEntity living = candidate.livingTarget();
    if (living != null) {
      builder.target(living)
          .param("intercept_target_speed", candidate.speed());
    } else {
      builder.target(candidate.interceptPoint())
          .param("intercept_eta", candidate.eta())
          .param("intercept_speed", candidate.speed());
    }

    return Optional.of(builder.build());
  }

  @Override
  public String name() { return "Intercept"; }
}
