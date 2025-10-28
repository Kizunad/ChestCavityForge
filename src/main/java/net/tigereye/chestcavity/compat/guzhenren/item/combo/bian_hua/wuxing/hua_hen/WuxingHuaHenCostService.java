package net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.wuxing.hua_hen;

import java.util.OptionalDouble;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge.ResourceHandle;

/**
 * 五行化痕 资源消耗工具。
 */
public final class WuxingHuaHenCostService {

  private static final String FIELD_NIANTOU = "niantou";
  private static final String FIELD_NIANTOU_MAX = "niantou_zuida";
  private static final String FIELD_HUNPO = "hunpo";
  private static final String FIELD_HUNPO_MAX = "zuida_hunpo";
  private static final String FIELD_JINGLI = "jingli";
  private static final String FIELD_JINGLI_MAX = "zuida_jingli";

  private WuxingHuaHenCostService() {}

  public static boolean tryConsumeBaseCost(ResourceHandle handle) {
    if (handle == null) {
      return false;
    }

    OptionalDouble zhenyuanResult =
        ResourceOps.tryConsumeTieredZhenyuan(
            handle,
            WuxingHuaHenTuning.DESIGN_ZHUANSHU,
            WuxingHuaHenTuning.DESIGN_JIEDUAN,
            WuxingHuaHenTuning.ZHENYUAN_TIER);
    if (zhenyuanResult.isEmpty()) {
      return false;
    }

    OptionalDouble niantouResult =
        ResourceOps.tryAdjustDouble(
            handle,
            FIELD_NIANTOU,
            -WuxingHuaHenTuning.COST_NIANTOU,
            true,
            FIELD_NIANTOU_MAX);
    if (niantouResult.isEmpty()) {
      ResourceOps.tryReplenishScaledZhenyuan(
          handle, WuxingHuaHenTuning.ZHENYUAN_BASE_COST, true);
      return false;
    }

    OptionalDouble hunpoResult =
        ResourceOps.tryAdjustDouble(
            handle, FIELD_HUNPO, -WuxingHuaHenTuning.COST_HUNPO, true, FIELD_HUNPO_MAX);
    if (hunpoResult.isEmpty()) {
      ResourceOps.tryAdjustDouble(
          handle, FIELD_NIANTOU, WuxingHuaHenTuning.COST_NIANTOU, true, FIELD_NIANTOU_MAX);
      ResourceOps.tryReplenishScaledZhenyuan(
          handle, WuxingHuaHenTuning.ZHENYUAN_BASE_COST, true);
      return false;
    }

    if (WuxingHuaHenTuning.COST_JINGLI > 0.0) {
      OptionalDouble jingliResult =
          ResourceOps.tryAdjustDouble(
              handle, FIELD_JINGLI, -WuxingHuaHenTuning.COST_JINGLI, true, FIELD_JINGLI_MAX);
      if (jingliResult.isEmpty()) {
        ResourceOps.tryAdjustDouble(
            handle, FIELD_HUNPO, WuxingHuaHenTuning.COST_HUNPO, true, FIELD_HUNPO_MAX);
        ResourceOps.tryAdjustDouble(
            handle, FIELD_NIANTOU, WuxingHuaHenTuning.COST_NIANTOU, true, FIELD_NIANTOU_MAX);
        ResourceOps.tryReplenishScaledZhenyuan(
            handle, WuxingHuaHenTuning.ZHENYUAN_BASE_COST, true);
        return false;
      }
    }

    return true;
  }

  public static void refundBaseCost(ResourceHandle handle) {
    if (handle == null) {
      return;
    }
    ResourceOps.tryReplenishScaledZhenyuan(
        handle, WuxingHuaHenTuning.ZHENYUAN_BASE_COST, true);
    ResourceOps.tryAdjustDouble(
        handle, FIELD_NIANTOU, WuxingHuaHenTuning.COST_NIANTOU, true, FIELD_NIANTOU_MAX);
    ResourceOps.tryAdjustDouble(
        handle, FIELD_HUNPO, WuxingHuaHenTuning.COST_HUNPO, true, FIELD_HUNPO_MAX);
    if (WuxingHuaHenTuning.COST_JINGLI > 0.0) {
      ResourceOps.tryAdjustDouble(
          handle, FIELD_JINGLI, WuxingHuaHenTuning.COST_JINGLI, true, FIELD_JINGLI_MAX);
    }
  }
}
