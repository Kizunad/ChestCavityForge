package net.tigereye.chestcavity.compat.guzhenren.item.yu_dao.behavior;

import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.guzhenren.resource.YuanLaoGuHelper;

/**
 * 三转元老蛊：大幅扩容并支持精力 ↔ 元石转化。
 */
public final class YuanLaoGuThirdTierBehavior extends AbstractYuanLaoGuBehavior {

    public static final YuanLaoGuThirdTierBehavior INSTANCE = new YuanLaoGuThirdTierBehavior();

    private static final String LOG_PREFIX = "[compat/guzhenren][yu_dao][yuan_lao_gu_3]";

    private YuanLaoGuThirdTierBehavior() {
    }

    @Override
    protected String logPrefix() {
        return LOG_PREFIX;
    }

    @Override
    protected boolean matchesOrgan(ItemStack stack) {
        return YuanLaoGuHelper.isThirdTierYuanLaoGu(stack);
    }

    @Override
    protected double zhenyuanPerStone() {
        return 400.0;
    }

    @Override
    protected double stoneRegenPerSlowTick() {
        return 8.0;
    }

    @Override
    protected double configuredStoneCap() {
        return 1_000_000.0;
    }

    @Override
    protected boolean allowAuxiliaryConversion() {
        return true;
    }

    @Override
    protected boolean isAuxiliaryResourceEnabled(AuxiliaryResource resource) {
        return resource == AuxiliaryResource.JINGLI;
    }
}
