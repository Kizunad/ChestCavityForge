package net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.wuxing.gui_bian;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.guzhenren.util.ZhenyuanBaseCosts;
import net.tigereye.chestcavity.guzhenren.util.ZhenyuanBaseCosts.Tier;

/**
 * 五行归变·逆转 调参常量。
 *
 * <p>集中保存所有调节参数，便于后续统一调优与测试覆写。
 */
public final class WuxingGuiBianTuning {

  private WuxingGuiBianTuning() {}

  public static final long BASE_COOLDOWN_TICKS = 30L * 20L;
  public static final long LARGE_AMOUNT_COOLDOWN_BONUS = 10L * 20L;
  public static final long TEMPORARY_FREEZE_DURATION = 20L * 20L;
  public static final long FREEZE_WARNING_TICKS = 5L * 20L;

  public static final double BASE_TAX = 0.08;
  public static final double TAX_REDUCTION_PER_ANCHOR = 0.02;
  public static final double YINYANG_TAX_REDUCTION = 0.02;
  public static final double SINGLE_CAST_CAP = 120.0;
  public static final double LARGE_AMOUNT_THRESHOLD = 100.0;

  public static final String BIANHUA_DAO_KEY = "daohen_bianhuadao";

  public static final int DESIGN_ZHUANSHU = 4;
  public static final int DESIGN_JIEDUAN = 4;
  public static final Tier ZHENYUAN_TIER = Tier.BURST;
  public static final double ZHENYUAN_BASE_COST =
      ZhenyuanBaseCosts.baseForTier(DESIGN_ZHUANSHU, DESIGN_JIEDUAN, ZHENYUAN_TIER);

  public static final double COST_NIANTOU = 6.0;
  public static final double COST_HUNPO = 3.0;
  public static final double COST_JINGLI = 6.0;

  public static final List<ResourceLocation> ELEMENT_ANCHORS =
      List.of(
          ResourceLocation.fromNamespaceAndPath("guzhenren", "jinfeigu"),
          ResourceLocation.fromNamespaceAndPath("guzhenren", "mugangu"),
          ResourceLocation.fromNamespaceAndPath("guzhenren", "shuishengu"),
          ResourceLocation.fromNamespaceAndPath("guzhenren", "huoxingu"),
          ResourceLocation.fromNamespaceAndPath("guzhenren", "tupigu"));
}
