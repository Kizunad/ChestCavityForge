package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.tuning;

/**
 * 剑锋蛊（四转/五转·剑道主动+被动器官）平衡调参。
 *
 * <p>主动技能·锋芒化形：消耗真元/精力，生成飞剑随侍；持续期间近战伤害提升。
 * <p>高额一击协同：当造成高伤害时，飞剑立即协同突击。
 * <p>被动技能·意随形动：完成3次协同后触发"剑意共振"，为所有飞剑短时加速。
 *
 * <p>平衡范围：四转/五转。资源消耗、阈值、效果随转数缩放。
 */
public final class JianFengGuTuning {

  private JianFengGuTuning() {}

  // ========== 模组与器官标识 ==========

  /** 模组ID */
  public static final String MOD_ID = "guzhenren";

  /** 四转器官ID：guzhenren:jianfenggu */
  public static final String ORGAN_ID_FOUR = "jianfenggu";

  /** 五转器官ID：guzhenren:jian_feng_gu_5 */
  public static final String ORGAN_ID_FIVE = "jian_feng_gu_5";

  /** 主动技能ID：guzhenren:jian_feng/huaxing */
  public static final String ABILITY_ID = "jian_feng/huaxing";

  // ========== 主动技能·锋芒化形 ==========

  /** 主动技能持续时间（ticks）= 10秒 */
  public static final int ACTIVE_BASE_DURATION_TICKS = 200;

  /** 主动期间近战伤害加成倍率（10%） */
  public static final double ACTIVE_BONUS_DAMAGE_MULT = 0.10;

  /** 真元消耗（四转：BURST Tier 4-1，五转：BURST Tier 5-1） */
  public static final double COST_ZHENYUAN_FOUR = 60.0;
  public static final double COST_ZHENYUAN_FIVE = 100.0;

  /** 精力消耗 */
  public static final double COST_JINGLI = 20.0;

  /** 冷却时间（ticks）= 20秒 */
  public static final int COOLDOWN_TICKS = 400;

  /** 四转生成飞剑数量 */
  public static final int SPAWN_COUNT_FOUR = 1;

  /** 五转生成飞剑数量 */
  public static final int SPAWN_COUNT_FIVE = 2;

  /** 飞剑生成偏移量（相对宿主身后，X, Y, Z） */
  public static final double SPAWN_OFFSET_X = 0.0;
  public static final double SPAWN_OFFSET_Y = 1.6;
  public static final double SPAWN_OFFSET_Z = -1.2;

  // ========== 高额一击协同 ==========

  /** 四转高额一击阈值 */
  public static final float HIGH_HIT_THRESHOLD_FOUR = 100.0f;

  /** 五转高额一击阈值 */
  public static final float HIGH_HIT_THRESHOLD_FIVE = 500.0f;

  /** 两次协同触发最小间隔（ticks，去抖） */
  public static final int COOP_MIN_INTERVAL_TICKS = 10;

  /** 协同触发冷却（ticks），用于 OnHit 触发节流 */
  public static final int COOP_COOLDOWN_TICKS = 20;

  /** 协同伤害基础倍率（相对原伤害） */
  public static final double COOP_DAMAGE_BASE_MULT = 0.5;

  /** 协同伤害道痕加成（每300道痕额外+50%伤害，上限+50%） */
  public static final double COOP_DAMAGE_DAOHEN_DIV = 300.0;
  public static final double COOP_DAMAGE_DAOHEN_MAX = 0.5;

  // ========== 剑意共振 ==========

  /** 触发共振需要的协同次数 */
  public static final int COOP_COUNT_FOR_RESONANCE = 3;

  /** 共振基础持续时间（ticks） */
  public static final int RESONANCE_BASE_DURATION_TICKS = 60;

  /** 共振基础速度加成（+15%） */
  public static final double RESONANCE_BASE_SPEED_BONUS = 0.15;

  /** 道痕对共振速度加成的增幅（每200道痕+5%速度） */
  public static final double RESONANCE_SPEED_PER_200_DAOHEN = 0.05;

  /** 道痕对共振持续时间的增幅（每300流派经验+20 ticks，这里简化为道痕） */
  public static final double RESONANCE_DURATION_PER_300_DAOHEN = 20.0;

  /** 五转共振额外速度加成（+25%） */
  public static final double RESONANCE_FIVE_BONUS = 0.25;

  // ========== 非玩家自动化 ==========

  /** 脱战延迟（ticks），超过后回收生成飞剑 */
  public static final int DISENGAGE_DELAY_TICKS = 100;

  // ========== 器官属性 ==========

  /** 四转剑道道痕 */
  public static final double DAOHEN_FOUR = 80.0;

  /** 五转剑道道痕 */
  public static final double DAOHEN_FIVE = 140.0;

  /** 四转移动速度 */
  public static final double SPEED_FOUR = 0.02;

  /** 五转移动速度 */
  public static final double SPEED_FIVE = 0.04;

  /** 四转防御 */
  public static final double ARMOR_FOUR = -2.0;

  /** 五转防御 */
  public static final double ARMOR_FIVE = -4.0;

  /** 四转力量 */
  public static final double STRENGTH_FOUR = 2.0;

  /** 五转力量 */
  public static final double STRENGTH_FIVE = 4.0;
}
