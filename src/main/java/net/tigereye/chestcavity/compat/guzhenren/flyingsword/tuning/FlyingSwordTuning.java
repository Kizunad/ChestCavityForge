package net.tigereye.chestcavity.compat.guzhenren.flyingsword.tuning;

/**
 * 飞剑参数聚合（兼容旧引用）。
 *
 * <p>新项目请直接引用细分类：
 * - FlyingSwordCoreTuning
 * - FlyingSwordExperienceTuning
 * - FlyingSwordInheritTuning
 * - FlyingSwordAITuning
 */
public final class FlyingSwordTuning {
  private FlyingSwordTuning() {}

  // 基础/核心
  public static final double SPEED_BASE = FlyingSwordCoreTuning.SPEED_BASE;
  public static final double SPEED_MAX = FlyingSwordCoreTuning.SPEED_MAX;
  public static final double ACCEL = FlyingSwordCoreTuning.ACCEL;
  public static final double TURN_RATE = FlyingSwordCoreTuning.TURN_RATE;
  public static final double DAMAGE_BASE = FlyingSwordCoreTuning.DAMAGE_BASE;
  public static final double VEL_DMG_COEF = FlyingSwordCoreTuning.VEL_DMG_COEF;
  public static final double V_REF = FlyingSwordCoreTuning.V_REF;
  public static final double MAX_DURABILITY = FlyingSwordCoreTuning.MAX_DURABILITY;
  public static final double DURA_LOSS_RATIO = FlyingSwordCoreTuning.DURA_LOSS_RATIO;
  public static final double DURA_BREAK_MULT = FlyingSwordCoreTuning.DURA_BREAK_MULT;
  public static final double UPKEEP_BASE_RATE = FlyingSwordCoreTuning.UPKEEP_BASE_RATE;
  public static final double UPKEEP_ORBIT_MULT = FlyingSwordCoreTuning.UPKEEP_ORBIT_MULT;
  public static final double UPKEEP_GUARD_MULT = FlyingSwordCoreTuning.UPKEEP_GUARD_MULT;
  public static final double UPKEEP_HUNT_MULT = FlyingSwordCoreTuning.UPKEEP_HUNT_MULT;
  public static final double UPKEEP_SPRINT_MULT = FlyingSwordCoreTuning.UPKEEP_SPRINT_MULT;
  public static final double UPKEEP_BREAK_MULT = FlyingSwordCoreTuning.UPKEEP_BREAK_MULT;
  public static final double UPKEEP_SPEED_SCALE = FlyingSwordCoreTuning.UPKEEP_SPEED_SCALE;
  public static final int UPKEEP_CHECK_INTERVAL = FlyingSwordCoreTuning.UPKEEP_CHECK_INTERVAL;
  public static final boolean ENABLE_BLOCK_BREAK = FlyingSwordCoreTuning.ENABLE_BLOCK_BREAK;
  public static final double BLOCK_BREAK_EFF_BASE = FlyingSwordCoreTuning.BLOCK_BREAK_EFF_BASE;

  // 经验/成长
  public static final double EXP_PER_DAMAGE = FlyingSwordExperienceTuning.EXP_PER_DAMAGE;
  public static final int EXP_KILL_MULT = FlyingSwordExperienceTuning.EXP_KILL_MULT;
  public static final int EXP_ELITE_MULT = FlyingSwordExperienceTuning.EXP_ELITE_MULT;
  public static final double EXP_BASE = FlyingSwordExperienceTuning.EXP_BASE;
  public static final double EXP_ALPHA = FlyingSwordExperienceTuning.EXP_ALPHA;
  public static final int MAX_LEVEL = FlyingSwordExperienceTuning.MAX_LEVEL;
  public static final double DAMAGE_PER_LEVEL = FlyingSwordExperienceTuning.DAMAGE_PER_LEVEL;

  // 释放继承
  public static final double INHERIT_DMG_MIN = FlyingSwordInheritTuning.INHERIT_DMG_MIN;
  public static final double INHERIT_DMG_MAX = FlyingSwordInheritTuning.INHERIT_DMG_MAX;
  public static final double INHERIT_SPEED_MIN = FlyingSwordInheritTuning.INHERIT_SPEED_MIN;
  public static final double INHERIT_SPEED_MAX = FlyingSwordInheritTuning.INHERIT_SPEED_MAX;
  public static final double INHERIT_SHARPNESS_DMG =
      FlyingSwordInheritTuning.INHERIT_SHARPNESS_DMG;
  public static final double INHERIT_SHARPNESS_VEL =
      FlyingSwordInheritTuning.INHERIT_SHARPNESS_VEL;
  public static final double INHERIT_ATTACK_DAMAGE_COEF =
      FlyingSwordInheritTuning.INHERIT_ATTACK_DAMAGE_COEF;
  public static final double INHERIT_ATTACK_SPEED_COEF =
      FlyingSwordInheritTuning.INHERIT_ATTACK_SPEED_COEF;
  public static final double INHERIT_MAX_DAMAGE_TO_MAX_DURABILITY_COEF =
      FlyingSwordInheritTuning.INHERIT_MAX_DAMAGE_TO_MAX_DURABILITY_COEF;
  public static final double INHERIT_ARMOR_TO_MAX_DURABILITY_COEF =
      FlyingSwordInheritTuning.INHERIT_ARMOR_TO_MAX_DURABILITY_COEF;
  public static final double INHERIT_ARMOR_DURA_LOSS_MULT_PER_POINT =
      FlyingSwordInheritTuning.INHERIT_ARMOR_DURA_LOSS_MULT_PER_POINT;

  // AI 行为
  public static final double ORBIT_TARGET_DISTANCE = FlyingSwordAITuning.ORBIT_TARGET_DISTANCE;
  public static final double ORBIT_DISTANCE_TOLERANCE =
      FlyingSwordAITuning.ORBIT_DISTANCE_TOLERANCE;
  public static final double GUARD_SEARCH_RANGE = FlyingSwordAITuning.GUARD_SEARCH_RANGE;
  public static final double GUARD_FOLLOW_DISTANCE = FlyingSwordAITuning.GUARD_FOLLOW_DISTANCE;
  public static final double HUNT_SEARCH_RANGE = FlyingSwordAITuning.HUNT_SEARCH_RANGE;
  public static final double HUNT_TARGET_VALID_RANGE =
      FlyingSwordAITuning.HUNT_TARGET_VALID_RANGE;
  public static final double HUNT_RETURN_DISTANCE = FlyingSwordAITuning.HUNT_RETURN_DISTANCE;

  public static final double ORBIT_TANGENT_SPEED_FACTOR =
      FlyingSwordAITuning.ORBIT_TANGENT_SPEED_FACTOR;
  public static final double ORBIT_RADIAL_PULL_IN = FlyingSwordAITuning.ORBIT_RADIAL_PULL_IN;
  public static final double ORBIT_APPROACH_SPEED_FACTOR =
    FlyingSwordAITuning.ORBIT_APPROACH_SPEED_FACTOR;
  public static final double ORBIT_RETREAT_SPEED_FACTOR =
    FlyingSwordAITuning.ORBIT_RETREAT_SPEED_FACTOR;
  public static final double GUARD_CHASE_MAX_FACTOR =
      FlyingSwordAITuning.GUARD_CHASE_MAX_FACTOR;
  public static final double GUARD_FOLLOW_APPROACH_FACTOR =
      FlyingSwordAITuning.GUARD_FOLLOW_APPROACH_FACTOR;
  public static final double GUARD_IDLE_TANGENT_FACTOR =
      FlyingSwordAITuning.GUARD_IDLE_TANGENT_FACTOR;
  public static final double HUNT_CHASE_MAX_FACTOR = FlyingSwordAITuning.HUNT_CHASE_MAX_FACTOR;
  public static final double HUNT_RETURN_APPROACH_FACTOR =
      FlyingSwordAITuning.HUNT_RETURN_APPROACH_FACTOR;
  public static final double HUNT_IDLE_TANGENT_FACTOR =
      FlyingSwordAITuning.HUNT_IDLE_TANGENT_FACTOR;

  // 非玩家配置
  /** 非玩家维持消耗模式 */
  public enum NonPlayerUpkeepMode {
    /** 不消耗任何资源 */
    NONE,
    /** 消耗血量代替真元 */
    HEALTH
  }

  /** 非玩家维持消耗模式（默认：不消耗） */
  public static final NonPlayerUpkeepMode NON_PLAYER_UPKEEP_MODE = NonPlayerUpkeepMode.NONE;

  /** 非玩家默认剑道流派经验（用于耐久减免计算） */
  public static final double NON_PLAYER_DEFAULT_SWORD_PATH_EXP = 10001.0;

  // ==================== Phase 4: 维持失败策略 ====================

  /**
   * 维持消耗失败策略
   *
   * <p>Phase 4: 当维持消耗失败（真元不足）时，系统的处理策略：
   * <ul>
   *   <li>RECALL: 召回飞剑到物品栏（默认，兼容旧版行为）</li>
   *   <li>STALL: 停滞不动，保持姿态但冻结移动</li>
   *   <li>SLOW: 减速移动，速度降低为原来的 SLOW_FACTOR 倍</li>
   * </ul>
   *
   * @see net.tigereye.chestcavity.compat.guzhenren.flyingsword.systems.UpkeepSystem
   */
  public enum UpkeepFailureStrategy {
    /** 召回到物品栏（默认） */
    RECALL,
    /** 停滞不动 */
    STALL,
    /** 减速移动 */
    SLOW
  }

  /** 维持失败策略（默认：召回） */
  public static final UpkeepFailureStrategy UPKEEP_FAILURE_STRATEGY =
      UpkeepFailureStrategy.RECALL;

  /** SLOW 策略的速度倍率（0.0 到 1.0） */
  public static final double UPKEEP_FAILURE_SLOW_FACTOR = 0.3;

  /** STALL 策略下播放音效的间隔（tick） */
  public static final int UPKEEP_FAILURE_SOUND_INTERVAL = 40;

  // ==================== 功能开关 ====================
  // Phase 0: 添加功能开关，为后续裁剪做准备

  /** 启用高级轨迹（保留：Orbit, PredictiveLine, CurvedIntercept） */
  public static final boolean ENABLE_ADVANCED_TRAJECTORIES = true;

  /** 启用额外意图（每个模式最多保留 2 条基础意图） */
  public static final boolean ENABLE_EXTRA_INTENTS = false;

  /** 启用青莲剑群系统（QingLianSwordSwarm） */
  public static final boolean ENABLE_SWARM = false;

  /** 启用剑引蛊 TUI（SwordCommandTUI） */
  public static final boolean ENABLE_TUI = true;

  /** 启用 Gecko 模型覆盖与视觉档案 */
  public static final boolean ENABLE_GEO_OVERRIDE_PROFILE = false;
}
