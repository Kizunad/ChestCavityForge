package net.tigereye.chestcavity.compat.guzhenren.item.bing_xue_dao.tuning;

import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.config.CCConfig;

/** 参数与ID访问器：冰肌蛊 */
public final class BingJiTuning {

  private BingJiTuning() {}

  private static final CCConfig.GuzhenrenBingXueDaoConfig.BingJiGuConfig DEFAULTS =
      new CCConfig.GuzhenrenBingXueDaoConfig.BingJiGuConfig();

  public static CCConfig.GuzhenrenBingXueDaoConfig.BingJiGuConfig cfg() {
    CCConfig root = ChestCavity.config;
    if (root != null) {
      CCConfig.GuzhenrenBingXueDaoConfig group = root.GUZHENREN_BING_XUE_DAO;
      if (group != null && group.BING_JI_GU != null) {
        return group.BING_JI_GU;
      }
    }
    return DEFAULTS;
  }

  // 常用ID（如需外部统一引用时使用）
  public static final ResourceLocation ABSORPTION_MODIFIER_ID =
      ResourceLocation.fromNamespaceAndPath("guzhenren", "modifiers/bing_ji_gu_absorption");
  public static final ResourceLocation ICE_BURST_FLOW_ID =
      ResourceLocation.fromNamespaceAndPath(ChestCavity.MODID, "bing_xue_burst");
}

