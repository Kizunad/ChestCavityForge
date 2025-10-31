package net.tigereye.chestcavity.compat.guzhenren.item.bing_xue_dao.tuning;

import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.config.CCConfig;

/** 参数与ID访问器：霜息蛊 */
public final class ShuangXiTuning {
  private ShuangXiTuning() {}

  private static final CCConfig.GuzhenrenBingXueDaoConfig.ShuangXiGuConfig DEFAULTS =
      new CCConfig.GuzhenrenBingXueDaoConfig.ShuangXiGuConfig();

  public static CCConfig.GuzhenrenBingXueDaoConfig.ShuangXiGuConfig cfg() {
    CCConfig root = ChestCavity.config;
    if (root != null) {
      CCConfig.GuzhenrenBingXueDaoConfig group = root.GUZHENREN_BING_XUE_DAO;
      if (group != null && group.SHUANG_XI_GU != null) {
        return group.SHUANG_XI_GU;
      }
    }
    return DEFAULTS;
  }
}

