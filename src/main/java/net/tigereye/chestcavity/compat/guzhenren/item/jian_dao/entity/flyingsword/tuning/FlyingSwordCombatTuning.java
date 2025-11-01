package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.tuning;

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

  private static double config(String key, double def) {
    return BehaviorConfigAccess.getFloat(FlyingSwordCombatTuning.class, key, (float) def);
  }

  private static int configInt(String key, int def) {
    return BehaviorConfigAccess.getInt(FlyingSwordCombatTuning.class, key, def);
  }
}

