package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.intent.types;

import java.util.Optional;
import net.minecraft.world.entity.LivingEntity;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.behavior.TargetFinder;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.intent.AIContext;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.intent.Intent;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.intent.IntentResult;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.trajectory.TrajectoryType;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.tuning.FlyingSwordAITuning;

/** 放风筝（Kiting）：保持安全半径，优先拖延高威胁近战。 */
public final class KitingIntent implements Intent {
  private static final double SAFE_RADIUS = 7.5;

  @Override
  public Optional<IntentResult> evaluate(AIContext ctx) {
    var sword = ctx.sword();
    LivingEntity target = TargetFinder.findHighThreatMelee(
        sword, sword.position(), FlyingSwordAITuning.HUNT_SEARCH_RANGE);
    if (target == null) {
      return Optional.empty();
    }

    double distance = Math.max(1.0, sword.distanceTo(target));
    double threatScore = target.getMaxHealth() * 0.05 + target.getArmorValue();
    double priority = 12.0 + threatScore + Math.max(0.0, SAFE_RADIUS - distance) * 2.0;

    return Optional.of(
        IntentResult.builder()
            .target(target)
            .trajectory(TrajectoryType.Serpentine)
            .priority(priority)
            .param("kiting_safe_radius", SAFE_RADIUS)
            .param("kiting_distance", distance)
            .build());
  }

  @Override
  public String name() { return "Kiting"; }
}
