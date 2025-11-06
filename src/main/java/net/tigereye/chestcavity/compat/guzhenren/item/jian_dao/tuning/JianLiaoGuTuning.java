package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.tuning;

/**
 * 剑疗蛊调参与默认常量。
 *
 * <p>后续若需数据驱动，可由配置或数据包覆盖，此处以常量实现基线行为。
 */
public final class JianLiaoGuTuning {

  private JianLiaoGuTuning() {}

  // ----------------- Passive: 心跳治疗 -----------------
  /** 心跳间隔（tick）。 */
  public static final int HEARTBEAT_PERIOD_T = 40;

  /** 单次基础治疗量。 */
  public static final float HEARTBEAT_HEAL_BASE = 0.2f;

  /** 每100点剑道道痕追加治疗量。 */
  public static final float HEARTBEAT_HEAL_PER_100_SCAR = 0.08f;

  /** 心跳治疗占最大生命百分比的上限（0.0-1.0）。 */
  public static final double HEARTBEAT_HEAL_CAP_RATIO = 0.01;

  // ----------------- Passive: 飞剑互补修复 -----------------
  /** 飞剑修复检查周期（tick）。 */
  public static final int SWORD_REPAIR_PERIOD_T = 100;

  /** 判定低耐久的阈值，低于该比例视为需要互补。 */
  public static final double LOW_DURABILITY_THRESHOLD = 0.10;

  /** 健康飞剑单次贡献的最大耐久比例。 */
  public static final double DONOR_COST_FRACTION = 0.05;

  /** 贡献池抽取的税率。 */
  public static final double DONOR_TAX_FRACTION = 0.05;

  /** 单次对目标飞剑的修复上限（占最大耐久的比例）。 */
  public static final double TARGET_ONCE_CAP_RATIO = 0.80;

  // ----------------- Active: 剑血互济 -----------------
  /** 激活时消耗的最大生命比例。 */
  public static final float ACTIVE_HP_SPEND_RATIO = 0.15f;

  /** 主动技能基础冷却（tick）。 */
  public static final int ACTIVE_BASE_COOLDOWN_T = 1200;

  /** 主动技能最小冷却（tick）。 */
  public static final int ACTIVE_MIN_COOLDOWN_T = 400;

  /** 每100点剑道道痕提升的修复效率（倍率）。 */
  public static final double EFFICIENCY_PER_100_SCAR = 0.25;

  /** 冷却缩放：每100点剑道道痕缩短的冷却比例（0.0-1.0）。 */
  public static final double COOLDOWN_REDUCTION_PER_100_SCAR = 0.05;

  /** 冷却缩放上限（避免过度缩短）。 */
  public static final double COOLDOWN_REDUCTION_CAP = 0.80;
}
