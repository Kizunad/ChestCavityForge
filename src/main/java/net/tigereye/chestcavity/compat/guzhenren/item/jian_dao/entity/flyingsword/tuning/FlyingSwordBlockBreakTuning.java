package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.tuning;

import net.tigereye.chestcavity.compat.guzhenren.util.behavior.BehaviorConfigAccess;

/**
 * 飞剑破块参数。
 */
public final class FlyingSwordBlockBreakTuning {
  private FlyingSwordBlockBreakTuning() {}

  /** 启动破块的最低速度百分比（0-1） */
  public static final double BREAK_MIN_SPEED_PERCENT =
      config("BREAK_MIN_SPEED_PERCENT", 0.3);

  /** 启动破块的最低绝对速度（方块/ tick）。若 >0，则优先生效，替代百分比阈值。 */
  public static final double BREAK_MIN_SPEED_ABS =
      config("BREAK_MIN_SPEED_ABS", 0.12);

  /** 调试日志开关（INFO），默认关闭 */
  public static final boolean BREAK_DEBUG_LOGS =
      configBool("BREAK_DEBUG_LOGS", false);

  /** 探测射线基础长度（方块） */
  public static final double BREAK_RAY_BASE = config("BREAK_RAY_BASE", 1.2);

  /** 探测射线随速度的增量系数（方块/速度） */
  public static final double BREAK_RAY_SPEED_MULT = config("BREAK_RAY_SPEED_MULT", 1.5);

  /** 满速时额外提升的镐等级（与 Attributes.toolTier 相加） */
  public static final int BREAK_EXTRA_TIER_AT_MAX = configInt("BREAK_EXTRA_TIER_AT_MAX", 2);

  /** 每 tick 破坏的最大方块数（性能保护） */
  public static final int BREAK_MAX_BLOCKS_PER_TICK = configInt("BREAK_MAX_BLOCKS_PER_TICK", 2);

  /** 每破坏一个方块施加的减速（0-1），例如 0.2 表示剩余 80% 速度 */
  public static final double BREAK_DECEL_PER_BLOCK = config("BREAK_DECEL_PER_BLOCK", 0.2);

  /** 命中盒周围扫描半径（方块），用于邻域破坏 */
  public static final double BREAK_SCAN_RADIUS = config("BREAK_SCAN_RADIUS", 2.0);

  private static double config(String key, double def) {
    return BehaviorConfigAccess.getFloat(FlyingSwordBlockBreakTuning.class, key, (float) def);
  }

  private static int configInt(String key, int def) {
    return BehaviorConfigAccess.getInt(FlyingSwordBlockBreakTuning.class, key, def);
  }

  private static boolean configBool(String key, boolean def) {
    return BehaviorConfigAccess.getBoolean(FlyingSwordBlockBreakTuning.class, key, def);
  }
}
