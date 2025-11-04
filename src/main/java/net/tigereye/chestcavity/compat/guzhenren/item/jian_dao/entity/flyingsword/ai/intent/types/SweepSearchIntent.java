package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.intent.types;

import java.util.Optional;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.behavior.TargetFinder;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.intent.AIContext;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.intent.Intent;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.intent.IntentResult;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.trajectory.TrajectoryType;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.tuning.FlyingSwordAITuning;

/** 搜寻（SweepSearch）：丢失目标后进行花瓣式搜索。 */
public final class SweepSearchIntent implements Intent {
  @Override
  public Optional<IntentResult> evaluate(AIContext ctx) {
    Vec3 center = TargetFinder.estimateHostileClusterCenter(
        ctx.sword(), ctx.sword().position(), FlyingSwordAITuning.HUNT_SEARCH_RANGE);
    if (center == null) {
      center = ctx.owner().position();
    }

    return Optional.of(IntentResult.builder()
        .target(center)
        .trajectory(TrajectoryType.PetalScan)
        .priority(0.12)
        .param("sweepsearch_origin_x", center.x)
        .param("sweepsearch_origin_z", center.z)
        .build());
  }

  @Override
  public String name() { return "SweepSearch"; }
}
