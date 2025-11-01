package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.tuning;

import net.tigereye.chestcavity.compat.guzhenren.util.behavior.BehaviorConfigAccess;

/**
 * 飞剑 AI 行为参数（距离与速度系数）
 */
public final class FlyingSwordAITuning {
  private FlyingSwordAITuning() {}

  // 距离/范围
  public static final double ORBIT_TARGET_DISTANCE = config("ORBIT_TARGET_DISTANCE", 3.0);
  public static final double ORBIT_DISTANCE_TOLERANCE = config("ORBIT_DISTANCE_TOLERANCE", 0.5);
  public static final double GUARD_SEARCH_RANGE = config("GUARD_SEARCH_RANGE", 12.0);
  public static final double GUARD_FOLLOW_DISTANCE = config("GUARD_FOLLOW_DISTANCE", 2.0);
  public static final double HUNT_SEARCH_RANGE = config("HUNT_SEARCH_RANGE", 24.0);
  public static final double HUNT_TARGET_VALID_RANGE = config("HUNT_TARGET_VALID_RANGE", 32.0);
  public static final double HUNT_RETURN_DISTANCE = config("HUNT_RETURN_DISTANCE", 4.0);
  public static final double HOVER_FOLLOW_DISTANCE = config("HOVER_FOLLOW_DISTANCE", 1.2);

  // 速度系数
  public static final double ORBIT_TANGENT_SPEED_FACTOR = config("ORBIT_TANGENT_SPEED_FACTOR", 0.8);
  public static final double ORBIT_RADIAL_PULL_IN = config("ORBIT_RADIAL_PULL_IN", 0.1);
  public static final double ORBIT_APPROACH_SPEED_FACTOR = config("ORBIT_APPROACH_SPEED_FACTOR", 1.0);
  public static final double ORBIT_RETREAT_SPEED_FACTOR = config("ORBIT_RETREAT_SPEED_FACTOR", 1.0);

  public static final double GUARD_CHASE_MAX_FACTOR = config("GUARD_CHASE_MAX_FACTOR", 0.9);
  public static final double GUARD_FOLLOW_APPROACH_FACTOR = config("GUARD_FOLLOW_APPROACH_FACTOR", 1.2);
  public static final double GUARD_IDLE_TANGENT_FACTOR = config("GUARD_IDLE_TANGENT_FACTOR", 0.4);

  public static final double HUNT_CHASE_MAX_FACTOR = config("HUNT_CHASE_MAX_FACTOR", 1.0);
  public static final double HUNT_RETURN_APPROACH_FACTOR = config("HUNT_RETURN_APPROACH_FACTOR", 1.0);
  public static final double HUNT_IDLE_TANGENT_FACTOR = config("HUNT_IDLE_TANGENT_FACTOR", 0.5);
  public static final double HOVER_APPROACH_FACTOR = config("HOVER_APPROACH_FACTOR", 0.8);

  private static double config(String key, double def) {
    return BehaviorConfigAccess.getFloat(FlyingSwordAITuning.class, key, (float) def);
  }
}
