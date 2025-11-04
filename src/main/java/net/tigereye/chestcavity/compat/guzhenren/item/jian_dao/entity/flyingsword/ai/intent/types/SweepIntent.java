package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.intent.types;

import java.util.Optional;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.behavior.TargetFinder;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.intent.AIContext;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.intent.Intent;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.intent.IntentResult;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.trajectory.TrajectoryType;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.tuning.FlyingSwordAITuning;

/** 扫荡（Sweep）：切割低血量密集区，快速收割。 */
public final class SweepIntent implements Intent {
  @Override
  public Optional<IntentResult> evaluate(AIContext ctx) {
    Vec3 cluster = TargetFinder.estimateHostileClusterCenter(
        ctx.sword(), ctx.sword().position(), FlyingSwordAITuning.HUNT_SEARCH_RANGE);
    if (cluster == null) {
      return Optional.empty();
    }

    Vec3 offset = new Vec3(1, 0, -1).normalize().scale(2.0);
    Vec3 pathStart = cluster.add(offset);

    return Optional.of(
        IntentResult.builder()
            .target(pathStart)
            .trajectory(TrajectoryType.Sawtooth)
            .priority(10.5)
            .param("sweep_center_x", cluster.x)
            .param("sweep_center_z", cluster.z)
            .build());
  }

  @Override
  public String name() { return "Sweep"; }
}
