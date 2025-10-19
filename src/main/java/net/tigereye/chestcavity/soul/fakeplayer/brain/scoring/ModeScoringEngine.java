package net.tigereye.chestcavity.soul.fakeplayer.brain.scoring;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import net.tigereye.chestcavity.soul.fakeplayer.brain.BrainMode;

/**
 * Provides high-level scoring for each {@link BrainMode} using registered
 * {@link ModeUtilityProfile}s. The engine is stateless and can be shared.
 */
public final class ModeScoringEngine {

    private final Map<BrainMode, ModeUtilityProfile> profiles;

    public ModeScoringEngine(Map<BrainMode, ModeUtilityProfile> profiles) {
        Objects.requireNonNull(profiles, "profiles");
        this.profiles = Map.copyOf(profiles);
    }

    public Optional<ModeScore> tryScore(BrainMode mode, ScoreInputs inputs) {
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(inputs, "inputs");
        ModeUtilityProfile profile = profiles.get(mode);
        return profile == null ? Optional.empty() : Optional.of(profile.score(inputs));
    }

    public ModeScore score(BrainMode mode, ScoreInputs inputs) {
        return tryScore(mode, inputs)
                .orElseThrow(() -> new IllegalArgumentException("No profile registered for mode " + mode));
    }

    public List<ModeScore> scoreAll(ScoreInputs inputs) {
        Objects.requireNonNull(inputs, "inputs");
        List<ModeScore> scores = new ArrayList<>(profiles.size());
        for (ModeUtilityProfile profile : profiles.values()) {
            scores.add(profile.score(inputs));
        }
        scores.sort(ModeScore.byUtilityDescending());
        return List.copyOf(scores);
    }

    public Map<BrainMode, ModeUtilityProfile> profiles() {
        return profiles;
    }

    public static ModeScoringEngine createDefault() {
        Map<BrainMode, ModeUtilityProfile> map = new EnumMap<>(BrainMode.class);

        map.put(BrainMode.AUTO, ModeUtilityProfile.builder(BrainMode.AUTO)
                .scorer(new WeightedUtilityScorer(0.30, 0.25, 0.25, 0.10, 0.10))
                .maxRelevantDistance(14.0)
                .absorptionAsHealth(18.0)
                .baseBias(0.05)
                .dangerThreatFloor(0.45)
                .build());

        map.put(BrainMode.COMBAT, ModeUtilityProfile.builder(BrainMode.COMBAT)
                .scorer(new WeightedUtilityScorer(0.45, 0.30, 0.15, 0.05, 0.05))
                .maxRelevantDistance(12.0)
                .absorptionAsHealth(16.0)
                .baseBias(0.10)
                .dangerThreatFloor(0.70)
                .build());

        map.put(BrainMode.SURVIVAL, ModeUtilityProfile.builder(BrainMode.SURVIVAL)
                .scorer(new WeightedUtilityScorer(0.10, 0.10, 0.65, -0.10, 0.25))
                .maxRelevantDistance(10.0)
                .absorptionAsHealth(12.0)
                .baseBias(0.05)
                .dangerThreatFloor(0.60)
                .build());

        map.put(BrainMode.IDLE, ModeUtilityProfile.builder(BrainMode.IDLE)
                .scorer(new WeightedUtilityScorer(-0.55, -0.15, 0.35, -0.10, -0.35))
                .maxRelevantDistance(20.0)
                .absorptionAsHealth(24.0)
                .baseBias(0.15)
                .dangerThreatFloor(0.20)
                .build());

        map.put(BrainMode.LLM, ModeUtilityProfile.builder(BrainMode.LLM)
                .scorer(new WeightedUtilityScorer(0.05, 0.05, 0.10, 0.10, 0.05))
                .maxRelevantDistance(12.0)
                .absorptionAsHealth(18.0)
                .baseBias(0.0)
                .dangerThreatFloor(0.30)
                .build());

        return new ModeScoringEngine(map);
    }
}
