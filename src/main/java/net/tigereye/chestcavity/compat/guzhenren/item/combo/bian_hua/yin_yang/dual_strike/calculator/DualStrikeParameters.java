package net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yin_yang.dual_strike.calculator;

/**
 * A snapshot of the computed parameters for the Dual Strike skill.
 *
 * @param damageFactor The factor to apply to the bonus damage.
 * @param cooldownTicks The cooldown of the skill in ticks.
 */
public record DualStrikeParameters(double damageFactor, int cooldownTicks) {}
