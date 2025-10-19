package net.tigereye.chestcavity.soul.fakeplayer.brain.model;

/**
 * 简单的探索计划结果，记录选择的目标与评分。
 */
public record ExplorationPlan(ExplorationTarget target, double score) {
    public ExplorationPlan {
        if (target == null) throw new IllegalArgumentException("target");
        score = clamp01(score);
    }

    private static double clamp01(double value) {
        if (value < 0.0) return 0.0;
        if (value > 1.0) return 1.0;
        return value;
    }
}
