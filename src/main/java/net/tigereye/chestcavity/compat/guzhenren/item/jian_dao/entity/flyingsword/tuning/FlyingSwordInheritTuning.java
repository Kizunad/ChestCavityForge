package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.tuning;

import net.tigereye.chestcavity.compat.guzhenren.util.behavior.BehaviorConfigAccess;

/**
 * 飞剑释放继承参数（由物品转化到实体）
 */
public final class FlyingSwordInheritTuning {
  private FlyingSwordInheritTuning() {}

  public static final double INHERIT_DMG_MIN = config("INHERIT_DMG_MIN", -2.0);
  public static final double INHERIT_DMG_MAX = config("INHERIT_DMG_MAX", 10.0);
  public static final double INHERIT_SPEED_MIN = config("INHERIT_SPEED_MIN", -0.1);
  public static final double INHERIT_SPEED_MAX = config("INHERIT_SPEED_MAX", 0.3);
  public static final double INHERIT_SHARPNESS_DMG = config("INHERIT_SHARPNESS_DMG", 0.5);
  public static final double INHERIT_SHARPNESS_VEL = config("INHERIT_SHARPNESS_VEL", 0.03);
  public static final double INHERIT_ATTACK_DAMAGE_COEF = config("INHERIT_ATTACK_DAMAGE_COEF", 0.5);
  public static final double INHERIT_ATTACK_SPEED_COEF = config("INHERIT_ATTACK_SPEED_COEF", 0.05);

  private static double config(String key, double def) {
    return BehaviorConfigAccess.getFloat(FlyingSwordInheritTuning.class, key, (float) def);
  }
}

