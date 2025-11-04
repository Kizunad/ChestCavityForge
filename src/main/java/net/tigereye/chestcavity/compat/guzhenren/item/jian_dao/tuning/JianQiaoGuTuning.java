package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.tuning;

import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.guzhenren.util.ZhenyuanBaseCosts;
import net.tigereye.chestcavity.guzhenren.util.ZhenyuanBaseCosts.Tier;

/** 调参常量：剑鞘蛊（五转·剑道辅助核心）。 */
public final class JianQiaoGuTuning {

  private JianQiaoGuTuning() {}

  public static final String MOD_ID = "guzhenren";

  /** 物品 ID。 */
  public static final ResourceLocation ORGAN_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "jian_qiao_gu");

  /** 主动技能 ID。 */
  public static final ResourceLocation ABILITY_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "shou_jian_ling");

  /** OrganState 根键。 */
  public static final String STATE_ROOT = "JianQiaoGu";

  /** 主动技能冷却键（tick）。 */
  public static final String KEY_READY_TICK = "ShouJianLingReadyAt";

  /** 主动技能冷却：20分钟。 */
  public static final int ACTIVE_COOLDOWN_T = 20 * 60 * 20;

  /** 收剑令判定范围（格）。 */
  public static final double SEIZE_RANGE = 12.0;

  /** 飞剑召回被动：最大修复比例。 */
  public static final double MAX_REPAIR_PERCENT = 0.25;

  /** 飞剑召回被动：精力消耗（每 1% 修复）。 */
  public static final double REPAIR_JINGLI_PER_PERCENT = 0.25;

  /** 道痕换算：每 100 点剑道道痕额外 +1 存储位。 */
  public static final double DAO_HEN_PER_EXTRA_SLOT = 100.0;

  /** 装备剑鞘蛊时的基础额外容量（+1）。 */
  public static final int ORGAN_CAPACITY_BONUS = 1;

  /** 默认容量（无额外加成）。 */
  public static final int BASE_CAPACITY = 10;

  /** 修复成本：五转·四阶，Tier LOW。 */
  public static final int REPAIR_COST_ZHUANSHU = 5;
  public static final int REPAIR_COST_JIEDUAN = 4;

  /** 收剑令成本：五转·四阶，Tier BURST。 */
  public static final int SEIZE_COST_ZHUANSHU = 5;
  public static final int SEIZE_COST_JIEDUAN = 4;

  public static double repairZhenyuanCost(double percent) {
    if (!(percent > 0.0)) {
      return 0.0;
    }
    double base =
        ZhenyuanBaseCosts.baseForTier(REPAIR_COST_ZHUANSHU, REPAIR_COST_JIEDUAN, Tier.SMALL);
    return base * Math.min(percent, 1.0);
  }

  public static double repairJingliCost(double percent) {
    if (!(percent > 0.0)) {
      return 0.0;
    }
    return REPAIR_JINGLI_PER_PERCENT * Math.min(percent, 1.0) * 100.0;
  }

  public static double seizeZhenyuanCost() {
    return ZhenyuanBaseCosts.baseForTier(SEIZE_COST_ZHUANSHU, SEIZE_COST_JIEDUAN, Tier.BURST);
  }

  public static int computeCapacity(boolean equipped, double daoHen) {
    int total = BASE_CAPACITY;
    if (equipped) {
      total += ORGAN_CAPACITY_BONUS;
    }
    if (daoHen > 0.0 && Double.isFinite(daoHen)) {
      total += (int) Math.floor(Math.max(0.0, daoHen) / DAO_HEN_PER_EXTRA_SLOT);
    }
    return Math.max(BASE_CAPACITY, total);
  }
}
