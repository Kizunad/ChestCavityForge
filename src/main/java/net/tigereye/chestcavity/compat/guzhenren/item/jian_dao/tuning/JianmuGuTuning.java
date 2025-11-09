package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.tuning;

import net.minecraft.resources.ResourceLocation;

/**
 * 剑幕蛊调参项。
 *
 * <p>定义护幕飞剑的各项参数，包括数量、性能、耐久、反击等。
 */
public final class JianmuGuTuning {

  private JianmuGuTuning() {}

  public static final String MOD_ID = "guzhenren";

  /** 物品 ID。 */
  public static final ResourceLocation ORGAN_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "jianmugu");

  /** 主动技能 ID（开关护幕）。 */
  public static final ResourceLocation ABILITY_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "jianmu_ward_toggle");

  /** OrganState 根键。 */
  public static final String STATE_ROOT = "JianmuGu";

  /** 护幕激活状态键。 */
  public static final String KEY_WARD_ACTIVE = "WardActive";

  /** 上次受伤时间戳（用于脱战判定）。 */
  public static final String KEY_LAST_HURT_TICK = "LastHurtTick";

  /** 主动技能冷却时间（tick）。 */
  public static final int ACTIVE_COOLDOWN_T = 20 * 5; // 5秒

  /** 玩家激活消耗真元/tick。 */
  public static final double UPKEEP_ZHENYUAN_PER_TICK = 2.0;

  /** 玩家激活消耗精力/tick。 */
  public static final double UPKEEP_JINGLI_PER_TICK = 0.5;

  // ===== 护幕参数 =====

  /** 护幕最大数量（受道痕与经验影响，公式计算）。 */
  public static final int MAX_WARD_SWORDS = 4;

  /** 护幕最小数量。 */
  public static final int MIN_WARD_SWORDS = 1;

  /** 环绕半径基础值（米）。 */
  public static final double ORBIT_RADIUS_BASE = 2.6;

  /** 每个额外护幕增加的半径（米）。 */
  public static final double ORBIT_RADIUS_PER_SWORD = 0.4;

  /** 最大速度基础值（m/s）。 */
  public static final double SPEED_BASE = 6.0;

  /** 每点道痕增加的速度（m/s）。 */
  public static final double SPEED_PER_TRAIL = 0.02;

  /** 每点经验增加的速度（m/s）。 */
  public static final double SPEED_PER_EXP = 0.001;

  /** 最大加速度（m/s²）。 */
  public static final double ACCEL_MAX = 40.0;

  /** 反应延迟基础值（秒）。 */
  public static final double REACTION_BASE = 0.06;

  /** 每点经验降低的反应延迟（秒）。 */
  public static final double REACTION_PER_EXP = 0.00005;

  /** 反应延迟最小值（秒）。 */
  public static final double REACTION_MIN = 0.02;

  /** 反应延迟最大值（秒）。 */
  public static final double REACTION_MAX = 0.06;

  // ===== 时间窗口 =====

  /** 最小可达时间窗（秒）。 */
  public static final double WINDOW_MIN = 0.1;

  /** 最大可达时间窗（秒）。 */
  public static final double WINDOW_MAX = 1.0;

  /** 反击最大距离（米）。 */
  public static final double COUNTER_RANGE = 5.0;

  // ===== 耐久系统 =====

  /** 初始耐久基础值。 */
  public static final double DURABILITY_BASE = 60.0;

  /** 每点道痕增加的耐久。 */
  public static final double DURABILITY_PER_TRAIL = 0.3;

  /** 每点经验增加的耐久。 */
  public static final double DURABILITY_PER_EXP = 0.1;

  /** 经验衰减系数分母（用于计算 R）。 */
  public static final double EXP_DECAY_DENOMINATOR = 2000.0;

  /** 经验衰减系数上限。 */
  public static final double EXP_DECAY_MAX = 0.6;

  /** 拦截成功基础耐久消耗。 */
  public static final int COST_BLOCK_BASE = 8;

  /** 反击成功基础耐久消耗。 */
  public static final int COST_COUNTER_BASE = 10;

  /** 拦截失败基础耐久消耗。 */
  public static final int COST_FAIL_BASE = 2;

  // ===== 反击伤害 =====

  /** 反击伤害基础值。 */
  public static final double COUNTER_DAMAGE_BASE = 5.0;

  /** 每点道痕增加的反击伤害。 */
  public static final double COUNTER_DAMAGE_PER_TRAIL = 0.05;

  /** 每点经验增加的反击伤害。 */
  public static final double COUNTER_DAMAGE_PER_EXP = 0.01;

  // ===== 非玩家AI =====

  /** 脱战判定：无攻击目标的时间（tick）。 */
  public static final int AI_DISENGAGE_NO_TARGET_TIME = 0; // 立即

  /** 脱战判定：未受伤的时间（tick）。 */
  public static final int AI_DISENGAGE_NO_DAMAGE_TIME = 20 * 10; // 10秒

  /** 被动盔甲值加成。 */
  public static final double PASSIVE_ARMOR_BONUS = 10.0;
}
