package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.tuning;

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
}
