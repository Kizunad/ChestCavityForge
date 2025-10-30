package net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yin_yang.tai_ji_swap.calculator;

import java.util.List;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yin_yang.tai_ji_swap.tuning.TaiJiSwapTuning;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yu_qun.tuning.YuQunTuning.CooldownTier;

/**
 * Contains the logic for calculating the parameters of the Tai Ji Swap skill.
 */
public final class TaiJiSwapLogic {

    private static final List<CooldownTier> FLOW_EXPERIENCE_COOLDOWN_TIERS =
            List.of(new CooldownTier(10001.0D, 20));

    private TaiJiSwapLogic() {}

    /**
     * Computes the parameters for the Tai Ji Swap skill based on the player's stats.
     *
     * @param changeDaoMarks The player's change dao marks.
     * @param changeFlowExp The player's change flow experience.
     * @return A snapshot of the computed parameters.
     */
    public static TaiJiSwapParameters computeParameters(double changeDaoMarks, double changeFlowExp) {
        float fallDamageReduction = (float) Math.min(1.0, TaiJiSwapTuning.FALL_DAMAGE_REDUCTION * (1.0 + changeDaoMarks));
        int cooldownTicks = computeCooldownTicks(changeFlowExp);
        return new TaiJiSwapParameters(fallDamageReduction, cooldownTicks);
    }

    private static int computeCooldownTicks(double totalExperience) {
        int result = (int) TaiJiSwapTuning.COOLDOWN_TICKS;
        double sanitized = Math.max(0.0D, totalExperience);
        for (CooldownTier tier : FLOW_EXPERIENCE_COOLDOWN_TIERS) {
            double threshold = Math.max(1.0D, tier.threshold());
            double ratio = Math.min(1.0D, sanitized / threshold);
            int candidate = (int) Math.round(
                    TaiJiSwapTuning.COOLDOWN_TICKS - (TaiJiSwapTuning.COOLDOWN_TICKS - tier.minCooldownTicks()) * ratio);
            candidate = Math.max(tier.minCooldownTicks(), candidate);
            result = Math.min(result, candidate);
        }
        return Math.max(20, result);
    }
}
