package net.tigereye.chestcavity.soul.fakeplayer.brain.scoring;

import java.util.Objects;

import net.tigereye.chestcavity.soul.fakeplayer.brain.BrainMode;

/**
 * Encapsulates the heuristics for turning {@link ScoreInputs} into
 * {@link ModeScore} instances for a specific {@link BrainMode}.
 */
public final class ModeUtilityProfile {

    private final BrainMode mode;
    private final WeightedUtilityScorer scorer;
    private final double maxRelevantDistance;
    private final double absorptionAsHealth;
    private final double baseBias;
    private final double dangerThreatFloor;

    private ModeUtilityProfile(Builder builder) {
        this.mode = builder.mode;
        this.scorer = builder.scorer;
        this.maxRelevantDistance = builder.maxRelevantDistance;
        this.absorptionAsHealth = builder.absorptionAsHealth;
        this.baseBias = builder.baseBias;
        this.dangerThreatFloor = builder.dangerThreatFloor;
    }

    public static Builder builder(BrainMode mode) {
        return new Builder(mode);
    }

    public BrainMode mode() {
        return mode;
    }

    public double baseBias() {
        return baseBias;
    }

    public double maxRelevantDistance() {
        return maxRelevantDistance;
    }

    public double absorptionAsHealth() {
        return absorptionAsHealth;
    }

    public double dangerThreatFloor() {
        return dangerThreatFloor;
    }

    public ModeScore score(ScoreInputs inputs) {
        Objects.requireNonNull(inputs, "inputs");
        double threat = computeThreat(inputs);
        double proximity = computeProximity(inputs);
        double effectiveHealth = computeEffectiveHealth(inputs);
        double raw = scorer.score(threat, proximity, effectiveHealth, inputs.hasRegen(), inputs.inDanger());
        double biased = clamp01(raw + baseBias);
        return new ModeScore(mode, biased, raw, threat, proximity, effectiveHealth, inputs.hasRegen(), inputs.inDanger(), baseBias);
    }

    private double computeThreat(ScoreInputs inputs) {
        double threat = inputs.primaryTarget() != null ? 1.0 : 0.0;
        if (inputs.inDanger()) {
            threat = Math.max(threat, dangerThreatFloor);
        }
        return clamp01(threat);
    }

    private double computeProximity(ScoreInputs inputs) {
        if (inputs.primaryTarget() == null) {
            return 0.0;
        }
        double distance = inputs.distanceToTarget();
        if (Double.isNaN(distance) || Double.isInfinite(distance)) {
            return 0.0;
        }
        if (distance <= 0.0) {
            return 1.0;
        }
        double closeness = 1.0 - (distance / maxRelevantDistance);
        return clamp01(closeness);
    }

    private double computeEffectiveHealth(ScoreInputs inputs) {
        double ratio = clamp01(inputs.healthRatio());
        double absorption = Math.max(0.0, inputs.absorption());
        double absorptionContribution = absorptionAsHealth <= 0.0 ? 0.0 : Math.min(1.0, absorption / absorptionAsHealth);
        return clamp01(ratio + absorptionContribution);
    }

    private static double clamp01(double value) {
        if (Double.isNaN(value)) {
            return 0.0;
        }
        return value < 0.0 ? 0.0 : Math.min(1.0, value);
    }

    public static final class Builder {
        private final BrainMode mode;
        private WeightedUtilityScorer scorer;
        private double maxRelevantDistance = 12.0;
        private double absorptionAsHealth = 20.0;
        private double baseBias = 0.0;
        private double dangerThreatFloor = 0.35;

        private Builder(BrainMode mode) {
            this.mode = Objects.requireNonNull(mode, "mode");
            this.scorer = new WeightedUtilityScorer(0.25, 0.25, 0.25, 0.15, 0.10);
        }

        public Builder scorer(WeightedUtilityScorer scorer) {
            this.scorer = Objects.requireNonNull(scorer, "scorer");
            return this;
        }

        public Builder maxRelevantDistance(double value) {
            this.maxRelevantDistance = Math.max(1.0, value);
            return this;
        }

        public Builder absorptionAsHealth(double value) {
            this.absorptionAsHealth = value <= 0.0 ? 0.0 : value;
            return this;
        }

        public Builder baseBias(double value) {
            this.baseBias = value;
            return this;
        }

        public Builder dangerThreatFloor(double value) {
            this.dangerThreatFloor = clamp01(value);
            return this;
        }

        public ModeUtilityProfile build() {
            return new ModeUtilityProfile(this);
        }
    }
}
