package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.tuning;

import net.tigereye.chestcavity.compat.guzhenren.item.common.cost.ResourceCost;

/** 剑域蛊 调参与常量。 */
public final class JianYuGuTuning {
  private JianYuGuTuning() {}

  // 调域窗口：6s
  public static final int TUNING_WINDOW_T = 6 * 20;

  // 持续与冷却（层数可扩展，当前使用0层）
  public static final double ACTIVE_BASE_S = 8.0; // 8s
  public static final double ACTIVE_PER_LAYER_S = 1.2; // +1.2s/层
  public static final double ACTIVE_MAX_S = 14.0; // ≤14s
  public static final double COOLDOWN_BASE_S = 25.0; // 25s
  public static final double COOLDOWN_PER_LAYER_S = -1.5; // -1.5s/层
  public static final double COOLDOWN_MIN_S = 16.0; // ≥16s

  // 开启成本：60 真元 + 6 念头
  public static final ResourceCost OPEN_COST = new ResourceCost(60.0, 0.0, 0.0, 6.0, 0, 0.0f);

  // 正面锥额外减伤（期间效果）
  public static final double FRONT_CONE_EXTRA_REDUCTION = 0.10; // 额外 -10% 实伤
  public static final double FRONT_CONE_HALF_ANGLE_COS = 0.5; // 60°锥：cos=0.5
}
