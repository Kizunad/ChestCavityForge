package net.tigereye.chestcavity.compat.guzhenren.item.combo.wuxing.hua_hen;

import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.guzhenren.util.ZhenyuanBaseCosts;
import net.tigereye.chestcavity.guzhenren.util.ZhenyuanBaseCosts.Tier;

/**
 * 五行化痕 调参常量。
 */
public final class WuxingHuaHenTuning {

  private WuxingHuaHenTuning() {}

  public static final ResourceLocation ICON =
      ResourceLocation.fromNamespaceAndPath("guzhenren", "textures/skill/wuxing_hua_hen.png");

  public static final long BASE_COOLDOWN_TICKS = 15L * 20L;
  public static final long LARGE_AMOUNT_COOLDOWN_BONUS = 5L * 20L;
  public static final long UNDO_WINDOW_TICKS = 10L * 60L * 20L;
  public static final long UNDO_WARNING_TICKS = 10L * 20L;

  public static final double BASE_TAX = 0.05;
  public static final double MODE_TAX_REDUCTION = 0.02;
  public static final double MAX_TAX = 0.20;
  public static final double SINGLE_CAST_CAP = 200.0;
  public static final double LARGE_AMOUNT_THRESHOLD = 100.0;
  public static final double UNDO_RETURN_RATIO = 0.80;

  public static final String BIANHUA_DAO_KEY = "daohen_bianhuadao";

  public static final int DESIGN_ZHUANSHU = 4;
  public static final int DESIGN_JIEDUAN = 1;
  public static final Tier ZHENYUAN_TIER = Tier.BURST;
  public static final double ZHENYUAN_BASE_COST =
      ZhenyuanBaseCosts.baseForTier(DESIGN_ZHUANSHU, DESIGN_JIEDUAN, ZHENYUAN_TIER);

  public static final double COST_NIANTOU = 4.0;
  public static final double COST_HUNPO = 2.0;
  public static final double COST_JINGLI = 0.0;
}

