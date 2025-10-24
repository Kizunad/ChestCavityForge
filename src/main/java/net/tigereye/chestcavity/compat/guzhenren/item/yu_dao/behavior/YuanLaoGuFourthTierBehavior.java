package net.tigereye.chestcavity.compat.guzhenren.item.yu_dao.behavior;

import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.guzhenren.resource.YuanLaoGuHelper;

/** 四转元老蛊：极大容量并开启魂魄/念头与元石的双向转化。 */
public final class YuanLaoGuFourthTierBehavior extends AbstractYuanLaoGuBehavior {

  public static final YuanLaoGuFourthTierBehavior INSTANCE = new YuanLaoGuFourthTierBehavior();

  private static final String LOG_PREFIX = "[compat/guzhenren][yu_dao][yuan_lao_gu_4]";

  private YuanLaoGuFourthTierBehavior() {}

  @Override
  protected String logPrefix() {
    return LOG_PREFIX;
  }

  @Override
  protected boolean matchesOrgan(ItemStack stack) {
    return YuanLaoGuHelper.isFourthTierYuanLaoGu(stack);
  }

  @Override
  protected double zhenyuanPerStone() {
    return 600.0;
  }

  @Override
  protected double stoneRegenPerSlowTick() {
    return 20.0;
  }

  @Override
  protected double configuredStoneCap() {
    return 10_000_000.0;
  }

  @Override
  protected boolean allowAuxiliaryConversion() {
    return true;
  }

  @Override
  protected boolean isAuxiliaryResourceEnabled(AuxiliaryResource resource) {
    return resource == AuxiliaryResource.HUNPO
        || resource == AuxiliaryResource.NIANTOU
        || resource == AuxiliaryResource.JINGLI;
  }
}
