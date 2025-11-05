package net.tigereye.chestcavity.compat.guzhenren.flyingsword.tuning;

import net.tigereye.chestcavity.compat.guzhenren.util.behavior.BehaviorConfigAccess;

/**
 * 飞剑经验/等级成长参数
 */
public final class FlyingSwordExperienceTuning {
  private FlyingSwordExperienceTuning() {}

  public static final double EXP_PER_DAMAGE = config("EXP_PER_DAMAGE", 2.0);
  public static final int EXP_KILL_MULT = configInt("EXP_KILL_MULT", 5);
  public static final int EXP_ELITE_MULT = configInt("EXP_ELITE_MULT", 2);
  public static final double EXP_BASE = config("EXP_BASE", 40.0);
  public static final double EXP_ALPHA = config("EXP_ALPHA", 1.5);
  public static final int MAX_LEVEL = configInt("MAX_LEVEL", 30);
  public static final double DAMAGE_PER_LEVEL = config("DAMAGE_PER_LEVEL", 0.6);

  private static double config(String key, double def) {
    return BehaviorConfigAccess.getFloat(FlyingSwordExperienceTuning.class, key, (float) def);
  }

  private static int configInt(String key, int def) {
    return BehaviorConfigAccess.getInt(FlyingSwordExperienceTuning.class, key, def);
  }
}

