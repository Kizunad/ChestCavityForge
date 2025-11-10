package net.tigereye.chestcavity.compat.guzhenren.flyingsword.tuning;

/** 飞剑核心参数（移动/伤害/耐久/维持/破块开关与效率） */
public final class FlyingSwordCoreTuning {
  private FlyingSwordCoreTuning() {}

  // 速度/机动
  public static final double SPEED_BASE = 0.8;
  public static final double SPEED_MAX = 10;
  public static final double ACCEL = 0.1;
  public static final double TURN_RATE = 0.28;

  /**
   * 转向随速度提升的倍率系数。
   *
   * <p>实际转向上限 = 基准转向 × (1 + TURN_RATE_SPEED_SCALE × 当前速度 / 最大速度)。
   */
  public static final double TURN_RATE_SPEED_SCALE = 0.4;

  // 伤害
  public static final double DAMAGE_BASE = 40.0;
  public static final double VEL_DMG_COEF = 100.0;
  public static final double V_REF = 0.35;

  // 耐久
  public static final double MAX_DURABILITY = 100000.0;
  public static final double DURA_LOSS_RATIO = 0.1;
  public static final double DURA_BREAK_MULT = 2.0;

  // 维持
  public static final double UPKEEP_BASE_RATE = 1.0;
  public static final double UPKEEP_ORBIT_MULT = 0.6;
  public static final double UPKEEP_GUARD_MULT = 1.0;
  public static final double UPKEEP_HUNT_MULT = 1.4;
  public static final double UPKEEP_SPRINT_MULT = 1.5;
  public static final double UPKEEP_BREAK_MULT = 2.0;
  public static final double UPKEEP_SPEED_SCALE = 0.5;
  public static final int UPKEEP_CHECK_INTERVAL = 20;

  // 破块
  public static final boolean ENABLE_BLOCK_BREAK = true;
  public static final double BLOCK_BREAK_EFF_BASE = 0.75;
}
