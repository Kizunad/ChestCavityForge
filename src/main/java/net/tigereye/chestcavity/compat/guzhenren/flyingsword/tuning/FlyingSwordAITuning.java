package net.tigereye.chestcavity.compat.guzhenren.flyingsword.tuning;

/** 飞剑 AI 行为参数（距离与速度系数） */
public final class FlyingSwordAITuning {
  private FlyingSwordAITuning() {}

  // 距离/范围
  public static final double ORBIT_TARGET_DISTANCE = 5.0;
  public static final double ORBIT_DISTANCE_TOLERANCE = 0.5;
  public static final double GUARD_SEARCH_RANGE = 12.0;
  public static final double GUARD_FOLLOW_DISTANCE = 2.0;
  public static final double HUNT_SEARCH_RANGE = 24.0;
  public static final double HUNT_TARGET_VALID_RANGE = 32.0;
  public static final double HUNT_RETURN_DISTANCE = 4.0;
  public static final double HOVER_FOLLOW_DISTANCE = 1.2;

  // 速度系数
  public static final double ORBIT_TANGENT_SPEED_FACTOR = 0.8;
  public static final double ORBIT_RADIAL_PULL_IN = 0.1;
  public static final double ORBIT_APPROACH_SPEED_FACTOR = 1.0;
  public static final double ORBIT_RETREAT_SPEED_FACTOR = 1.0;
  // 绝对速度上限（用于避免高属性情况下环绕过快）
  public static final double ORBIT_ABS_MAX_SPEED = 0.10;

  public static final double GUARD_CHASE_MAX_FACTOR = 0.9;
  public static final double GUARD_FOLLOW_APPROACH_FACTOR = 1.2;
  public static final double GUARD_IDLE_TANGENT_FACTOR = 0.4;

  public static final double HUNT_CHASE_MAX_FACTOR = 1.0;
  public static final double HUNT_RETURN_APPROACH_FACTOR = 1.0;
  public static final double HUNT_IDLE_TANGENT_FACTOR = 0.5;
  public static final double HOVER_APPROACH_FACTOR = 0.8;
}
