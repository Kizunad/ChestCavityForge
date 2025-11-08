package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.tuning;

/**
 * 剑梭蛊（3-5转·剑道突进+躲避器官）平衡调参。
 *
 * <p>本蛊主动技能：向前突进，路径伤害，基于剑道道痕放大距离与伤害。
 * <p>被动技能：受击时有几率反向后退 + 减伤 + 无敌帧，全局冷却。
 * <p>飞剑增强：定时自动沿朝向短突进。
 *
 * <p>平衡范围：3–5 转。资源消耗、冷却随转数/阶段缩放。
 */
public final class JianSuoGuTuning {

  private JianSuoGuTuning() {}

  // ========== 主动突进 ==========

  /** 基础突进距离（格）。*/
  public static final double BASE_DASH_DISTANCE = 5.0;

  /** 基础伤害系数（与道痕、速度缩放）。*/
  public static final double BASE_ATK = 1.0;

  /** 突进路径胶囊体半宽（用于命中判定）。*/
  public static final double RAY_WIDTH = 0.8;

  /** 逐帧推进上限（避免无限循环）。*/
  public static final int MAX_DASH_STEPS = 12;

  /** 命中去重窗口（tick）。*/
  public static final int HIT_ONCE_DEDUP_TICKS = 10;

  /** 突进距离上限（格）。*/
  public static final double MAX_DASH_DISTANCE = 17.0;

  /** 道痕对距离的增幅系数（每 100 道痕）。*/
  public static final double DASH_DIST_PER_100_DAOHEN = 0.25;

  /** 道痕对伤害的增幅系数（每 100 道痕）。*/
  public static final double DAMAGE_PER_100_DAOHEN = 0.35;

  /** 速度对伤害的额外增幅上限。*/
  public static final double VELOCITY_SCALE_MAX = 0.25;

  // ========== NPC/飞剑适配 ==========

  /** NPC 目标锁定最大距离（超出则取朝向）。*/
  public static final double NPC_GOAL_LOCK_MAXDIST = 24.0;

  /** 飞剑自动突进间隔（秒）。*/
  public static final double SWORD_DASH_INTERVAL_S = 3.0;

  /** 飞剑突进距离缩放系数（相对玩家主动）。*/
  public static final double SWORD_DASH_DISTANCE_SCALE = 0.6;

  /** 飞剑突进伤害缩放系数（相对玩家主动）。*/
  public static final double SWORD_DASH_DAMAGE_SCALE = 0.6;

  // ========== 被动躲避 ==========

  /** 躲避冷却（秒）。*/
  public static final double EVADE_COOLDOWN_S = 6.0;

  /** 后退距离（格）。*/
  public static final double EVADE_BACKSTEP_DISTANCE = 2.4;

  /** 无敌帧持续（tick）。*/
  public static final int EVADE_INVULN_FRAMES = 6;

  /** 基础躲避几率。*/
  public static final double EVADE_CHANCE_BASE = 0.10;

  /** 每 100 道痕增加的躲避几率。*/
  public static final double EVADE_CHANCE_PER_100 = 0.06;

  /** 躲避几率上限。*/
  public static final double EVADE_CHANCE_MAX = 0.60;

  /** 基础减伤比例（最小）。*/
  public static final double EVADE_REDUCE_MIN = 0.10;

  /** 每 100 道痕增加的减伤比例。*/
  public static final double EVADE_REDUCE_PER_100 = 0.08;

  /** 减伤比例上限。*/
  public static final double EVADE_REDUCE_MAX = 0.90;

  // ========== 资源消耗 ==========

  /** 真元基础消耗。*/
  public static final double BASE_COST_ZHENYUAN = 6.0;

  /** 精力基础消耗。*/
  public static final double BASE_COST_JINGLI = 2.0;

  /** 念头基础消耗（玩家可选）。*/
  public static final double BASE_COST_NIANTOU = 1.0;

  // ========== 冷却 ==========

  /** 主动技能冷却下限（ticks）= 3秒。*/
  public static final int ACTIVE_COOLDOWN_MIN_TICKS = 60;

  /** 主动技能冷却上限（ticks）= 6秒。*/
  public static final int ACTIVE_COOLDOWN_MAX_TICKS = 120;
}
