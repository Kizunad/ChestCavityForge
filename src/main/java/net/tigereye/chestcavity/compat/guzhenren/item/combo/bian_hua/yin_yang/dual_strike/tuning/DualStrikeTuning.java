package net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yin_yang.dual_strike.tuning;

import net.tigereye.chestcavity.compat.guzhenren.item.combo.framework.ComboSkillUtil.ResourceCost;

/**
 * 两界同击 调参常量
 */
public final class DualStrikeTuning {
    private DualStrikeTuning() {}

    public static final long COOLDOWN_TICKS = 35L * 20L;
    public static final long STRIKE_WINDOW_TICKS = 5L * 20L;

    public static final ResourceCost COST = new ResourceCost(120.0, 6.0, 0.0, 4.0, 0, 0.0f);
    
    public static final double DAMAGE_FACTOR = 0.8D;
}
