package net.tigereye.chestcavity.compat.guzhenren.item.combo.wuxing.gui_bian;

import java.util.OptionalDouble;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge.ResourceHandle;

/**
 * 五行归变·逆转 资源消耗工具。
 */
public final class WuxingGuiBianCostService {

  private static final String FIELD_NIANTOU = "niantou";
  private static final String FIELD_NIANTOU_MAX = "niantou_zuida";
  private static final String FIELD_HUNPO = "hunpo";
  private static final String FIELD_HUNPO_MAX = "zuida_hunpo";
  private static final String FIELD_JINGLI = "jingli";
  private static final String FIELD_JINGLI_MAX = "zuida_jingli";

  private WuxingGuiBianCostService() {}

  public static boolean tryConsumeBaseCost(ResourceHandle handle) {
    if (handle == null) {
      return false;
    }

    OptionalDouble zhenyuanResult =
        ResourceOps.tryConsumeTieredZhenyuan(
            handle,
            WuxingGuiBianTuning.DESIGN_ZHUANSHU,
            WuxingGuiBianTuning.DESIGN_JIEDUAN,
            WuxingGuiBianTuning.ZHENYUAN_TIER);
    if (zhenyuanResult.isEmpty()) {
      return false;
    }

    OptionalDouble niantouResult =
        ResourceOps.tryAdjustDouble(
            handle,
            FIELD_NIANTOU,
            -WuxingGuiBianTuning.COST_NIANTOU,
            true,
            FIELD_NIANTOU_MAX);
    if (niantouResult.isEmpty()) {
      ResourceOps.tryReplenishScaledZhenyuan(
          handle, WuxingGuiBianTuning.ZHENYUAN_BASE_COST, true);
      return false;
    }

    OptionalDouble hunpoResult =
        ResourceOps.tryAdjustDouble(
            handle, FIELD_HUNPO, -WuxingGuiBianTuning.COST_HUNPO, true, FIELD_HUNPO_MAX);
    if (hunpoResult.isEmpty()) {
      ResourceOps.tryAdjustDouble(
          handle, FIELD_NIANTOU, WuxingGuiBianTuning.COST_NIANTOU, true, FIELD_NIANTOU_MAX);
      ResourceOps.tryReplenishScaledZhenyuan(
          handle, WuxingGuiBianTuning.ZHENYUAN_BASE_COST, true);
      return false;
    }

    OptionalDouble jingliResult =
        ResourceOps.tryAdjustDouble(
            handle, FIELD_JINGLI, -WuxingGuiBianTuning.COST_JINGLI, true, FIELD_JINGLI_MAX);
    if (jingliResult.isEmpty()) {
      ResourceOps.tryAdjustDouble(
          handle, FIELD_HUNPO, WuxingGuiBianTuning.COST_HUNPO, true, FIELD_HUNPO_MAX);
      ResourceOps.tryAdjustDouble(
          handle, FIELD_NIANTOU, WuxingGuiBianTuning.COST_NIANTOU, true, FIELD_NIANTOU_MAX);
      ResourceOps.tryReplenishScaledZhenyuan(
          handle, WuxingGuiBianTuning.ZHENYUAN_BASE_COST, true);
      return false;
    }

    return true;
  }

  public static void refundBaseCost(ResourceHandle handle) {
    if (handle == null) {
      return;
    }
    ResourceOps.tryReplenishScaledZhenyuan(
        handle, WuxingGuiBianTuning.ZHENYUAN_BASE_COST, true);
    ResourceOps.tryAdjustDouble(
        handle, FIELD_NIANTOU, WuxingGuiBianTuning.COST_NIANTOU, true, FIELD_NIANTOU_MAX);
    ResourceOps.tryAdjustDouble(
        handle, FIELD_HUNPO, WuxingGuiBianTuning.COST_HUNPO, true, FIELD_HUNPO_MAX);
    ResourceOps.tryAdjustDouble(
        handle, FIELD_JINGLI, WuxingGuiBianTuning.COST_JINGLI, true, FIELD_JINGLI_MAX);
  }
}
