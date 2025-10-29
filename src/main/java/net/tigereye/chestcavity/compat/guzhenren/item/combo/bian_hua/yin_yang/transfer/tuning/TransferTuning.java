package net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yin_yang.transfer.tuning;

import net.tigereye.chestcavity.compat.guzhenren.item.combo.framework.ComboSkillUtil.ResourceCost;

/**
 * 阴阳互渡 调参常量
 */
public final class TransferTuning {
    private TransferTuning() {}

    public static final long COOLDOWN_TICKS = 40L * 20L;
    public static final double TRANSFER_RATIO = 0.3D;

    public static final ResourceCost COST = new ResourceCost(60.0, 0.0, 0.0, 6.0, 0, 0.0f);
}
