package net.tigereye.chestcavity.compat.guzhenren.flyingsword.tuning;

/**
 * 玩家骑乘飞剑时的手感与增幅调参。
 */
public final class FlyingSwordControlTuning {
  private FlyingSwordControlTuning() {}

  /**
   * 速度平滑系数（0..1）：越大越跟手，越小越平滑。
   */
  public static final double RIDER_VELOCITY_SMOOTHING = 0.25; // 25% 向目标插值

  /** 输入死区：低于该幅度视为无输入，进入悬停/慢停。 */
  public static final double RIDER_INPUT_DEADZONE = 0.05;

  /** 基础速度倍率（独立于实体属性，便于单独调速度手感）。 */
  public static final double RIDER_BASE_SPEED_MULT = 0.1;

  /** 道痕对速度的增幅系数（按 sqrt(道痕) 计算）。 */
  public static final double RIDER_DAOHEN_COEF = 0.02;

  /** 道痕加成的最大额外倍率（1.0 表示无上限前的基线）。 */
  public static final double RIDER_DAOHEN_MAX_EXTRA = 0.5; // 最多 +50%
}

