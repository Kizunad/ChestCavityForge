package net.tigereye.chestcavity.compat.guzhenren.flyingsword.tuning;

import net.tigereye.chestcavity.compat.guzhenren.util.behavior.BehaviorConfigAccess;

/**
 * 飞剑核心参数（移动/伤害/耐久/维持/破块开关与效率）
 */
public final class FlyingSwordCoreTuning {
  private FlyingSwordCoreTuning() {}

  // 速度/机动
  public static final double SPEED_BASE = config("SPEED_BASE", 0.18);
  public static final double SPEED_MAX = config("SPEED_MAX", 0.42);
  public static final double ACCEL = config("ACCEL", 0.015);
  public static final double TURN_RATE = config("TURN_RATE", 0.14);

  // 伤害
  public static final double DAMAGE_BASE = config("DAMAGE_BASE", 4.0);
  public static final double VEL_DMG_COEF = config("VEL_DMG_COEF", 1.0);
  public static final double V_REF = config("V_REF", 0.35);

  // 耐久
  public static final double MAX_DURABILITY = config("MAX_DURABILITY", 1000.0);
  public static final double DURA_LOSS_RATIO = config("DURA_LOSS_RATIO", 0.1);
  public static final double DURA_BREAK_MULT = config("DURA_BREAK_MULT", 2.0);

  // 维持
  public static final double UPKEEP_BASE_RATE = config("UPKEEP_BASE_RATE", 1.0);
  public static final double UPKEEP_ORBIT_MULT = config("UPKEEP_ORBIT_MULT", 0.6);
  public static final double UPKEEP_GUARD_MULT = config("UPKEEP_GUARD_MULT", 1.0);
  public static final double UPKEEP_HUNT_MULT = config("UPKEEP_HUNT_MULT", 1.4);
  public static final double UPKEEP_SPRINT_MULT = config("UPKEEP_SPRINT_MULT", 1.5);
  public static final double UPKEEP_BREAK_MULT = config("UPKEEP_BREAK_MULT", 2.0);
  public static final double UPKEEP_SPEED_SCALE = config("UPKEEP_SPEED_SCALE", 0.5);
  public static final int UPKEEP_CHECK_INTERVAL = configInt("UPKEEP_CHECK_INTERVAL", 20);

  // 破块
  public static final boolean ENABLE_BLOCK_BREAK = configBool("ENABLE_BLOCK_BREAK", true);
  public static final double BLOCK_BREAK_EFF_BASE = config("BLOCK_BREAK_EFF_BASE", 0.75);

  private static double config(String key, double def) {
    return BehaviorConfigAccess.getFloat(FlyingSwordCoreTuning.class, key, (float) def);
  }

  private static int configInt(String key, int def) {
    return BehaviorConfigAccess.getInt(FlyingSwordCoreTuning.class, key, def);
  }

  private static boolean configBool(String key, boolean def) {
    return BehaviorConfigAccess.getBoolean(FlyingSwordCoreTuning.class, key, def);
  }
}

