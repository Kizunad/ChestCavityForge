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
 * 断阵（Breaker）：清理召唤物、护盾阵列或防御设施。
 */
public final class BreakerIntent implements Intent {
  @Override
  public Optional<IntentResult> evaluate(AIContext ctx) {
    var sword = ctx.sword();
    LivingEntity target = TargetFinder.findBreakerTarget(
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
  public String name() { return "Breaker"; }
}
