package net.tigereye.chestcavity.compat.guzhenren.item.yu_dao.behavior;

import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.guzhenren.resource.YuanLaoGuHelper;

/** 二转元老蛊：1 元石 = 250 基础真元，并且每次 slowTick 被动恢复 1 元石。 */
public final class YuanLaoGuSecondTierBehavior extends AbstractYuanLaoGuBehavior {

  public static final YuanLaoGuSecondTierBehavior INSTANCE = new YuanLaoGuSecondTierBehavior();

  private static final String LOG_PREFIX = "[compat/guzhenren][yu_dao][yuan_lao_gu_2]";

  private YuanLaoGuSecondTierBehavior() {}

  @Override
  protected String logPrefix() {
    return LOG_PREFIX;
  }

  @Override
  protected boolean matchesOrgan(ItemStack stack) {
    return YuanLaoGuHelper.isSecondTierYuanLaoGu(stack);
  }

  @Override
  protected double zhenyuanPerStone() {
    return 200.0;
  }

  @Override
  protected double stoneRegenPerSlowTick() {
    return 0.5;
  }

  @Override
  protected double configuredStoneCap() {
    return 100_000.0;
  }
}
