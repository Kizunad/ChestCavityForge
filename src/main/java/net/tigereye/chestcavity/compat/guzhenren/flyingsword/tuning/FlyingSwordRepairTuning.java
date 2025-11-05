package net.tigereye.chestcavity.compat.guzhenren.flyingsword.tuning;

import net.tigereye.chestcavity.compat.guzhenren.util.behavior.BehaviorConfigAccess;

/**
 * 飞剑修复/赋能相关参数。
 */
public final class FlyingSwordRepairTuning {
  private FlyingSwordRepairTuning() {}

  /** 每次使用修复材料恢复的最大耐久比例（默认 10%）。 */
  public static final double REPAIR_PERCENT_PER_USE = config("REPAIR_PERCENT_PER_USE", 0.10);

  private static double config(String key, double def) {
    return BehaviorConfigAccess.getFloat(FlyingSwordRepairTuning.class, key, (float) def);
  }
}

