package net.tigereye.chestcavity.soul.fakeplayer.brain.scoring;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.soul.fakeplayer.brain.model.ExplorationTarget;

/** 依据当前输入计算探索候选点的效用评分，返回 [0,1] 区间。 */
public final class ExplorationOpportunityScorer {

  public double score(ScoreInputs inputs, ExplorationTarget candidate) {
    if (inputs == null || candidate == null) {
      return 0.0;
    }
    double healthFactor = inputs.healthRatio();
    double regenPenalty = inputs.hasRegen() ? 0.2 : 0.0;
    double dangerPenalty = inputs.inDanger() ? 0.35 : 0.0;
    double novelty = candidate.novelty();
    double ownerFactor = ownerDistanceFactor(candidate, inputs.ownerPosition());
    double base = 0.5 * ownerFactor + 0.3 * novelty + 0.2 * healthFactor;
    double score = base - regenPenalty - dangerPenalty;
    if (inputs.primaryTarget() != null) {
      score -= 0.1; // 战斗中稍微抑制探索意图
    }
    return Mth.clamp(score, 0.0, 1.0);
  }

  private double ownerDistanceFactor(ExplorationTarget candidate, Vec3 owner) {
    if (owner == null) {
      return 0.6 + 0.4 * candidate.novelty();
    }
    double hint = Math.max(1.0, candidate.distanceHint());
    double dist = candidate.distanceTo(owner);
    double normalized = 1.0 - Mth.clamp(dist / hint, 0.0, 1.0);
    return 0.2 + 0.8 * normalized;
  }
}
