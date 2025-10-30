package net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yin_yang.transfer.calculator;

/**
 * A snapshot of the computed parameters for the Transfer skill.
 *
 * @param transferRatio The ratio of the resource to transfer.
 * @param cooldownTicks The cooldown of the skill in ticks.
 */
public record TransferParameters(double transferRatio, int cooldownTicks) {}
