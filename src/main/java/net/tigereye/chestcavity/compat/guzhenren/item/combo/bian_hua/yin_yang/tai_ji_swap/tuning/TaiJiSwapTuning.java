package net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yin_yang.tai_ji_swap.tuning;

import net.tigereye.chestcavity.compat.guzhenren.item.combo.framework.ComboSkillUtil.ResourceCost;

/**
 * 太极错位 调参常量
 */
public final class TaiJiSwapTuning {
    private TaiJiSwapTuning() {}

    public static final long COOLDOWN_TICKS = 25L * 20L;
    public static final long CONSECUTIVE_SWAP_WINDOW_TICKS = 40L; // 2 秒
    public static final long FALL_GUARD_TICKS = 60L; // 3 秒
    public static final float FALL_DAMAGE_REDUCTION = 0.7f;

    public static final ResourceCost COST = new ResourceCost(80.0, 8.0, 0.0, 3.0, 0, 0.0f);
}
