package net.tigereye.chestcavity.soul.fakeplayer.brain.scoring;

import net.minecraft.world.entity.LivingEntity;
import net.tigereye.chestcavity.soul.fakeplayer.brain.model.SurvivalSnapshot;

/**
 * Computes survival oriented scores that drive retreat and sustain behaviour.
 */
public final class SurvivalScorecard {

    private final WeightedUtilityScorer fleeScorer;
    private final double retreatThreshold;
    private final double holdThreshold;

    public SurvivalScorecard(WeightedUtilityScorer fleeScorer, double retreatThreshold, double holdThreshold) {
        this.fleeScorer = fleeScorer;
        this.retreatThreshold = clamp01(retreatThreshold);
        this.holdThreshold = clamp01(holdThreshold);
    }

    public SurvivalSnapshot evaluate(ScoreInputs inputs) {
        LivingEntity threat = inputs.primaryTarget();
        double closeness = closeness(inputs.distanceToTarget());
        double health = clamp01(inputs.healthRatio());
        double fleeScore = fleeScorer.score(threatScore(threat), closeness, health, inputs.hasRegen(), inputs.inDanger());
        boolean shouldRetreat = fleeScore >= retreatThreshold || (health < 0.35 && !inputs.hasRegen());
        boolean shouldHold = fleeScore <= holdThreshold && health >= 0.6;
        return new SurvivalSnapshot(
            threat,
            fleeScore,
            shouldRetreat,
            shouldHold,
            health,
            inputs.absorption(),
            inputs.hasRegen(),
            inputs.inDanger(),
            inputs.distanceToTarget()
        );
    }

    private static double threatScore(LivingEntity threat) {
        if (threat == null) {
            return 0.0;
        }
        return threat.isAlive() ? 1.0 : 0.0;
    }

    private static double closeness(double distance) {
        if (Double.isNaN(distance) || Double.isInfinite(distance)) {
            return 0.0;
        }
        double normalized = 1.0 - Math.min(1.0, distance / 12.0);
        return clamp01(normalized);
    }

    private static double clamp01(double value) {
        return value < 0.0 ? 0.0 : Math.min(1.0, value);
    }
}
