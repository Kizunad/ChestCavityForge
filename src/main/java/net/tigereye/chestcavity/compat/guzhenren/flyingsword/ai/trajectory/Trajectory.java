package net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.trajectory;

import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.AIContext;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent.IntentResult;

/** 轨迹生成器：根据 IntentResult 生成下一帧“期望速度向量”。 */
public interface Trajectory {
  Vec3 computeDesiredVelocity(AIContext ctx, IntentResult intent);
}
