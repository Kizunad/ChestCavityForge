package net.tigereye.chestcavity.compat.guzhenren.item.yu_dao.behavior;

import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.guzhenren.resource.YuanLaoGuHelper;

/**
 * 元老蛊（一转）行为：1 元石 = 100 基础真元。
 */
public final class YuanLaoGuOrganBehavior extends AbstractYuanLaoGuBehavior {

    public static final YuanLaoGuOrganBehavior INSTANCE = new YuanLaoGuOrganBehavior();

    private static final String LOG_PREFIX = "[compat/guzhenren][yu_dao][yuan_lao_gu]";

    private YuanLaoGuOrganBehavior() {
    }

    @Override
    protected String logPrefix() {
        return LOG_PREFIX;
    }

    @Override
    protected boolean matchesOrgan(ItemStack stack) {
        return YuanLaoGuHelper.isYuanLaoGu(stack);
    }

    @Override
    protected double zhenyuanPerStone() {
        return 100.0;
    }
}
