package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.tuning;

/**
 * 剑气蛊（四转·剑道主动+被动器官）平衡调参。
 *
 * <p>主动技能·一斩开天：强力直线剑气斩击，可命中多个实体与受控方块。
 * <p>被动技能·气断山河：每次有效命中增加断势层数，每满3层提供威能加成与衰减豁免。
 * <p>非玩家生物 OnHit 触发：按概率触发主动技，带1分钟冷却。
 *
 * <p>平衡范围：四转4阶。资源消耗、阈值、效果按四转标准调校。
 */
public final class JianQiGuTuning {

  private JianQiGuTuning() {}

  // ========== 模组与器官标识 ==========

  /** 模组ID */
  public static final String MOD_ID = "guzhenren";

  /** 器官ID：guzhenren:jianqigu */
  public static final String ORGAN_ID = "jianqigu";

  /** 主动技能ID：guzhenren:jian_qi_yi_zhan_kai_tian */
  public static final String ABILITY_ID = "jian_qi_yi_zhan_kai_tian";

  // ========== 主动技能·一斩开天 ==========

  /** 基础伤害（真实伤害，尽量绕过护甲减免） */
  public static final double BASE_DAMAGE = 80.0;

  /** 最大射程（blocks） */
  public static final double MAX_RANGE = 20.0;

  /** 剑光速度（blocks/tick） */
  public static final double SLASH_SPEED = 2.5;

  /** 每次命中后的威能衰减率（15%） */
  public static final double DECAY_RATE = 0.15;

  /** 威能低于初始值的比例阈值时提前终止（20%） */
  public static final double MIN_DAMAGE_RATIO = 0.20;

  /** 剑光宽度（用于碰撞检测，blocks） */
  public static final double SLASH_WIDTH = 1.5;

  /** 真元消耗（BURST Tier 4-1） */
  public static final double COST_ZHENYUAN_BURST = 80.0;

  /** 精力消耗 */
  public static final double COST_JINGLI = 25.0;

  /** 念头消耗 */
  public static final double COST_NIANTOU = 15.0;

  /** 冷却时间（ticks）= 15秒 */
  public static final int COOLDOWN_TICKS = 300;

  /** 最小境界要求（4转） */
  public static final int MIN_REALM_REQUIREMENT = 4;

  /** 最小阶位要求（4阶） */
  public static final int MIN_TIER_REQUIREMENT = 4;

  // ========== 方块破坏 ==========

  /** 方块破坏硬度上限（仅破坏硬度低于此值的方块） */
  public static final float BLOCK_BREAK_HARDNESS_MAX = 3.0f;

  /** 每次破坏方块后的额外威能衰减（5%，与命中实体叠加） */
  public static final double BLOCK_BREAK_DECAY = 0.05;

  /** 每tick最多破坏的方块数量（防止性能问题） */
  public static final int BLOCK_BREAK_CAP_PER_TICK = 3;

  // ========== 被动技能·气断山河 ==========

  /** 断势层数累积阈值（每3层触发一次加成） */
  public static final int DUANSHI_STACK_THRESHOLD = 3;

  /** 每次断势触发提供的威能加成（25%，乘区） */
  public static final double DUANSHI_POWER_BONUS = 0.25;

  /** 每次断势触发减少的衰减次数（豁免1次衰减） */
  public static final int DUANSHI_DECAY_GRACE = 1;

  /** 断势层数过期时间（ticks）= 10秒 */
  public static final int STACK_EXPIRE_TICKS = 200;

  // ========== 非玩家生物 OnHit 触发 ==========

  /** 非玩家生物 OnHit 触发主动技的概率（15%） */
  public static final float NPC_ACTIVE_CHANCE = 0.15f;

  /** 非玩家生物触发主动技后的冷却时间（ticks）= 1分钟 */
  public static final int NPC_ONHIT_COOLDOWN_TICKS = 1200;

  // ========== 道痕与流派经验加成 ==========

  /** 道痕系数除数（每100道痕提供额外10%伤害） */
  public static final double DAOHEN_DAMAGE_DIV = 100.0;

  /** 道痕加成上限（最多+50%） */
  public static final double DAOHEN_DAMAGE_MAX = 0.50;

  /** 流派经验系数除数（每200流派经验提供额外5%伤害） */
  public static final double LIUPAI_DAMAGE_DIV = 200.0;

  /** 流派经验加成上限（最多+30%） */
  public static final double LIUPAI_DAMAGE_MAX = 0.30;

  // ========== 器官属性（用于物品属性定义，这里仅作参考） ==========

  /** 剑道道痕 */
  public static final double DAOHEN_JIANDAO = 100.0;

  /** 移动速度加成 */
  public static final double SPEED = 0.03;

  /** 防御减免（攻击型器官，减少防御） */
  public static final double ARMOR = -3.0;

  /** 力量加成 */
  public static final double STRENGTH = 3.0;
}
