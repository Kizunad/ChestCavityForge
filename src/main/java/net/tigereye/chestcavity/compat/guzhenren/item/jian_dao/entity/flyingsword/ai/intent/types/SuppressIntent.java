package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.intent.types;

import java.util.Optional;
import net.minecraft.world.entity.LivingEntity;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.behavior.TargetFinder;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.intent.AIContext;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.intent.Intent;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.intent.IntentResult;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.trajectory.TrajectoryType;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.tuning.FlyingSwordAITuning;

/** 压制（Suppress）：沿施法单位外缘缠扰，持续打断。 */
public final class SuppressIntent implements Intent {
  @Override
  public Optional<IntentResult> evaluate(AIContext ctx) {
    LivingEntity target = TargetFinder.findCasterOrChanneler(
        ctx.sword(), ctx.sword().position(), FlyingSwordAITuning.HUNT_SEARCH_RANGE);
    if (target == null) {
      return Optional.empty();
    }

    double distance = Math.max(1.0, ctx.sword().distanceTo(target));
    double range = Math.max(3.0, target.getBbWidth() * 2.0);
    double priority = 15.0 + 4.0 / distance;

    return Optional.of(
        IntentResult.builder()
            .target(target)
            .trajectory(TrajectoryType.VortexOrbit)
            .priority(priority)
            .param("suppress_radius", range)
            .build());
  }

  @Override
  public String name() { return "Suppress"; }
}
