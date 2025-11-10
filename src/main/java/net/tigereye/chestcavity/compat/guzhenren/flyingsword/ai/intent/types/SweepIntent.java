package net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.types;

import java.util.Optional;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.behavior.TargetFinder;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.AIContext;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.Intent;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.IntentResult;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.trajectory.TrajectoryType;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.tuning.FlyingSwordAITuning;

/**
 * 扫荡（Sweep）：切割低血量密集区，快速收割。
 *
 * <p><b>Phase 7: 软删除标记（Soft Deletion Mark）</b> - 扩展意图
 *
 * <p>本意图仅在 {@code ENABLE_EXTRA_INTENTS=true} 时实例化。 默认配置下不会被使用，降低 AI 决策复杂度。
 *
 * @see
 *     net.tigereye.chestcavity.compat.guzhenren.flyingsword.tuning.FlyingSwordTuning#ENABLE_EXTRA_INTENTS
 * @see net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.planner.IntentPlanner
 */
public final class SweepIntent implements Intent {
  @Override
  public Optional<IntentResult> evaluate(AIContext ctx) {
    Vec3 cluster =
        TargetFinder.estimateHostileClusterCenter(
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
  public String name() {
    return "Sweep";
  }
}
