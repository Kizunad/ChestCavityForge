package net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.types;

import java.util.Optional;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.AIContext;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.Intent;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.IntentResult;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.trajectory.TrajectoryType;

/** 守点（Hold）：定点小半径转圈（占位：环绕主人）。 */
public final class HoldIntent implements Intent {
  @Override
  public Optional<IntentResult> evaluate(AIContext ctx) {
    Vec3 anchor = ctx.owner().position().add(0.0, 1.0, 0.0);
    return Optional.of(IntentResult.builder()
        .target(anchor)
        .trajectory(TrajectoryType.Orbit)
        .priority(0.3)
        .param("hold_radius", 2.0)
        .build());
  }
  @Override
  public String name() { return "Hold"; }
}
