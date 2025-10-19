package net.tigereye.chestcavity.soul.fakeplayer.brain.policy;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import net.tigereye.chestcavity.soul.fakeplayer.brain.BrainMode;
import net.tigereye.chestcavity.soul.fakeplayer.brain.scoring.ModeScore;
import net.tigereye.chestcavity.soul.fakeplayer.brain.scoring.ModeScoringEngine;
import net.tigereye.chestcavity.soul.fakeplayer.brain.scoring.ScoreInputs;

/**
 * Coordinates residence, hysteresis and budget rules to decide whether the
 * brain should switch to another {@link BrainMode}.
 */
public final class ModeSelectionPolicy {

    public enum DecisionReason {
        STAY_CURRENT,
        RESIDENCE_LOCKED,
        BUDGET_EXHAUSTED,
        NO_ALTERNATIVE,
        SWITCHED_FOR_UTILITY
    }

    public record Decision(
            BrainMode chosenMode,
            ModeScore chosenScore,
            ModeScore currentScore,
            DecisionReason reason,
            List<ModeScore> orderedScores
    ) {
        public Decision {
            Objects.requireNonNull(chosenMode, "chosenMode");
            Objects.requireNonNull(chosenScore, "chosenScore");
            Objects.requireNonNull(currentScore, "currentScore");
            orderedScores = List.copyOf(orderedScores);
        }

        public boolean switched() {
            return chosenMode != currentScore.mode();
        }
    }

    private final HysteresisPolicy hysteresis;
    private final ResidencePolicy residence;
    private final ModeBudgetTracker budgetTracker;

    public ModeSelectionPolicy(HysteresisPolicy hysteresis, ResidencePolicy residence, BudgetPolicy budget) {
        this.hysteresis = Objects.requireNonNull(hysteresis, "hysteresis");
        this.residence = Objects.requireNonNull(residence, "residence");
        this.budgetTracker = new ModeBudgetTracker(Objects.requireNonNull(budget, "budget"));
    }

    public Decision decide(BrainMode currentMode, ScoreInputs inputs, ModeScoringEngine engine,
                            int ticksInCurrentMode, long gameTime) {
        Objects.requireNonNull(currentMode, "currentMode");
        Objects.requireNonNull(inputs, "inputs");
        Objects.requireNonNull(engine, "engine");

        budgetTracker.beginTick(gameTime);

        List<ModeScore> ordered = new ArrayList<>(engine.scoreAll(inputs));
        ModeScore currentScore = null;
        ModeScore bestAlternative = null;
        for (ModeScore score : ordered) {
            if (score.mode() == currentMode) {
                currentScore = score;
            } else if (bestAlternative == null) {
                bestAlternative = score;
            }
        }

        if (currentScore == null) {
            currentScore = engine.tryScore(currentMode, inputs).orElse(ModeScore.zero(currentMode, inputs));
            ordered.add(currentScore);
            ordered.sort(ModeScore.byUtilityDescending());
            bestAlternative = null;
            for (ModeScore score : ordered) {
                if (score.mode() == currentMode) {
                    continue;
                }
                bestAlternative = score;
                break;
            }
        }

        ordered = List.copyOf(ordered);

        if (!residence.canLeave(currentMode, ticksInCurrentMode)) {
            return new Decision(currentMode, currentScore, currentScore, DecisionReason.RESIDENCE_LOCKED, ordered);
        }

        if (!budgetTracker.canSwitch()) {
            return new Decision(currentMode, currentScore, currentScore, DecisionReason.BUDGET_EXHAUSTED, ordered);
        }

        if (bestAlternative == null) {
            return new Decision(currentMode, currentScore, currentScore, DecisionReason.NO_ALTERNATIVE, ordered);
        }

        if (!hysteresis.shouldSwitch(currentScore.utility(), bestAlternative.utility())) {
            return new Decision(currentMode, currentScore, currentScore, DecisionReason.STAY_CURRENT, ordered);
        }

        budgetTracker.recordSwitch();
        return new Decision(bestAlternative.mode(), bestAlternative, currentScore, DecisionReason.SWITCHED_FOR_UTILITY, ordered);
    }

    public boolean tryReservePlan(long gameTime) {
        budgetTracker.beginTick(gameTime);
        if (!budgetTracker.canPlan()) {
            return false;
        }
        budgetTracker.recordPlan();
        return true;
    }

    public void reset() {
        budgetTracker.reset();
    }

    public ModeBudgetTracker tracker() {
        return budgetTracker;
    }
}
