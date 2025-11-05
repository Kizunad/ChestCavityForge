package net.tigereye.chestcavity.compat.guzhenren.flyingsword.tuning;

import net.tigereye.chestcavity.compat.guzhenren.util.behavior.BehaviorConfigAccess;

/**
 * 战斗参数（攻击范围与冷却）
 */
public final class FlyingSwordCombatTuning {
  private FlyingSwordCombatTuning() {}

  /** 碰撞攻击范围（方块） */
  public static final double ATTACK_RANGE = config("ATTACK_RANGE", 3);

  /** 攻击冷却（ticks） */
  public static final int ATTACK_COOLDOWN_TICKS = configInt("ATTACK_COOLDOWN_TICKS", 10);

  /** 调试日志：碰撞检测心跳日志（默认关闭） */
  public static final boolean COMBAT_DEBUG_LOGS = configBool("COMBAT_DEBUG_LOGS", false);

  private static double config(String key, double def) {
    return BehaviorConfigAccess.getFloat(FlyingSwordCombatTuning.class, key, (float) def);
  }

  private static int configInt(String key, int def) {
    return BehaviorConfigAccess.getInt(FlyingSwordCombatTuning.class, key, def);
  }

  private static boolean configBool(String key, boolean def) {
    return BehaviorConfigAccess.getBoolean(FlyingSwordCombatTuning.class, key, def);
  }
}
