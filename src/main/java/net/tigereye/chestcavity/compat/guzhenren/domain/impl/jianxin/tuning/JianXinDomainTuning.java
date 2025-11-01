package net.tigereye.chestcavity.compat.guzhenren.domain.impl.jianxin.tuning;

/**
 * 剑心域调参配置
 *
 * <p>集中管理剑心域的所有数值参数，便于平衡性调整。
 */
public final class JianXinDomainTuning {

  private JianXinDomainTuning() {}

  // ========== 基础属性 ==========

  /** 基础半径（方块） */
  public static final double BASE_RADIUS = 5.0;

  /** 最小等级 */
  public static final int MIN_LEVEL = 5;

  /** 最大等级 */
  public static final int MAX_LEVEL = 6;

  /** 等级判定阈值（总实力超过此值则6级，否则5级） */
  public static final double LEVEL_THRESHOLD = 100.0;

  // ========== 友方效果 ==========

  /** 友方资源恢复速率（每tick） */
  public static final double FRIENDLY_REGEN_PER_TICK = 0.05;

  /** 剑势恢复速率（每秒） */
  public static final int FRIENDLY_SWORD_MOMENTUM_PER_SEC = 1;

  /** 资源恢复强化倍数（定心返本触发时） */
  public static final double ENHANCED_REGEN_MULT = 2.0;

  // ========== 敌方效果 ==========

  /** 敌方移动速度减慢比例 */
  public static final double ENEMY_SLOW_FACTOR = 0.15; // 15%减速

  /** 敌方攻击速度减慢比例 */
  public static final double ENEMY_ATTACK_SLOW_FACTOR = 0.1; // 10%减速

  /** 强化状态下减益倍数 */
  public static final double ENHANCED_DEBUFF_MULT = 2.0;

  /** 效果持续时间（tick，每tick刷新） */
  public static final int EFFECT_DURATION = 20; // 1秒

  // ========== 友方飞剑加速 ==========

  /** 友方飞剑基础加速比例（最终缩放为 1.0 + clamp(...)）。*/
  public static final double FRIENDLY_SWORD_SPEED_BOOST_BASE = 0.15; // +15%

  /** 友方飞剑最大加速上限。*/
  public static final double FRIENDLY_SWORD_SPEED_BOOST_MAX = 0.35; // +35%

  /** 强化状态对加速的额外倍乘。*/
  public static final double FRIENDLY_SWORD_BOOST_ENHANCED_MULT = 1.15; // +15% on top

  // ========== 实力判定 ==========

  /** 剑道道痕权重（用于实力计算） */
  public static final double DAOHEN_WEIGHT = 0.5;

  /** 流派经验权重 */
  public static final double SCHOOL_EXP_WEIGHT = 0.5;

  /** 剑道道痕强度系数（用于效果缩放） */
  public static final double DAOHEN_INTENSITY_COEF = 0.001;

  /** 流派经验强度系数 */
  public static final double SCHOOL_EXP_INTENSITY_COEF = 0.002;

  // ========== 剑气反噬 ==========

  /** 剑气反噬基础伤害 */
  public static final float SWORD_COUNTER_BASE_DAMAGE = 2.0f;

  /** 剑气反噬硬直时间（tick） */
  public static final int SWORD_COUNTER_STUN_TICKS = 20; // 1秒

  /** 实力差距伤害系数（每点差距额外伤害） */
  public static final float POWER_DIFF_DAMAGE_COEF = 0.1f;

  // ========== 强化状态（定心返本） ==========

  /** 强化持续时间（tick） */
  public static final int ENHANCED_DURATION = 40; // 2秒

  /** 无敌焦点持续时间（tick） */
  public static final int UNBREAKABLE_FOCUS_DURATION = 40; // 2秒

  // ========== 粒子特效 ==========

  /** 领域边界粒子生成频率（tick） */
  public static final int BORDER_PARTICLE_INTERVAL = 10;

  /** 每次生成的边界粒子数量 */
  public static final int BORDER_PARTICLE_COUNT = 16;

  /** 中心粒子生成频率（tick） */
  public static final int CENTER_PARTICLE_INTERVAL = 5;

  /** 强化状态粒子密度倍数 */
  public static final double ENHANCED_PARTICLE_MULT = 2.0;
}
