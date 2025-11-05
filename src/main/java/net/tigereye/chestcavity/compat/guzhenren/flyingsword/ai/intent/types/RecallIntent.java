package net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.types;

import java.util.Optional;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.AIContext;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.Intent;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.IntentResult;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.trajectory.TrajectoryType;

/**
 * 收锋（Recall）：回到宿主。
 */
public final class RecallIntent implements Intent {
  @Override
  public Optional<IntentResult> evaluate(AIContext ctx) {
    // 始终发出回收意图
    return Optional.of(
        IntentResult.builder()
            .trajectory(TrajectoryType.Boomerang)
            .priority(1000.0)
            .build());
  }

  @Override
  public String name() { return "Recall"; }
}

