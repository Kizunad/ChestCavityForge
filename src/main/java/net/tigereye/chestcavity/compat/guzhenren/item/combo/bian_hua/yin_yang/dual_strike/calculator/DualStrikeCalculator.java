package net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yin_yang.dual_strike.calculator;

import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yin_yang.dual_strike.tuning.DualStrikeTuning;

/**
 * 两界同击 纯逻辑计算
 */
public final class DualStrikeCalculator {
    private DualStrikeCalculator() {}

    public static double calculateBonusDamage(double baseAttackYin, double baseAttackYang, double damageFactor) {
        double base = Math.min(baseAttackYin, baseAttackYang) * damageFactor;
        return Math.max(0.0D, base);
    }
}
