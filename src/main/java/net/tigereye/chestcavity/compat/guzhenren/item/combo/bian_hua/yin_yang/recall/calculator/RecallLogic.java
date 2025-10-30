package net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yin_yang.recall.calculator;

import java.util.List;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yin_yang.recall.tuning.RecallTuning;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yu_qun.tuning.YuQunTuning.CooldownTier;

/**
 * Contains the logic for calculating the parameters of the Recall skill.
 */
public final class RecallLogic {

    private static final List<CooldownTier> FLOW_EXPERIENCE_COOLDOWN_TIERS =
            List.of(new CooldownTier(10001.0D, 20));

    private RecallLogic() {}

    /**
     * Computes the parameters for the Recall skill based on the player's stats.
     *
     * @param changeDaoMarks The player's change dao marks.
     * @param changeFlowExp The player's change flow experience.
     * @return A snapshot of the computed parameters.
     */
    public static RecallParameters computeParameters(double changeDaoMarks, double changeFlowExp) {
        int cooldownTicks = computeCooldownTicks(changeFlowExp);
        return new RecallParameters(cooldownTicks);
    }

    private static int computeCooldownTicks(double totalExperience) {
        int result = (int) RecallTuning.COOLDOWN_TICKS;
        double sanitized = Math.max(0.0D, totalExperience);
        for (CooldownTier tier : FLOW_EXPERIENCE_COOLDOWN_TIERS) {
            double threshold = Math.max(1.0D, tier.threshold());
            double ratio = Math.min(1.0D, sanitized / threshold);
            int candidate = (int) Math.round(
                    RecallTuning.COOLDOWN_TICKS - (RecallTuning.COOLDOWN_TICKS - tier.minCooldownTicks()) * ratio);
            candidate = Math.max(tier.minCooldownTicks(), candidate);
            result = Math.min(result, candidate);
        }
        return Math.max(20, result);
    }
}
