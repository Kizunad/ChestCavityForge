package net.tigereye.chestcavity.compat.guzhenren.shockfield.api;

/** 干涉相位类型：用于计算波的叠加效果。 */
public enum PhaseKind {
  /** 同相增强：相位差 ≤ 60° → 伤害倍率 1.25 */
  CONSTRUCTIVE,

  /** 正常：相位差在 (60°, 120°) → 伤害倍率 1.0 */
  NORMAL,

  /** 反相减弱：相位差 ≥ 120° → 伤害倍率 0.75 */
  DESTRUCTIVE;

  /**
   * 根据相位差（弧度）判定干涉类型。
   *
   * @param phaseDiffRad 相位差，单位：弧度
   * @return 干涉类型
   */
  public static PhaseKind fromPhaseDiff(double phaseDiffRad) {
    double degrees = Math.toDegrees(Math.abs(phaseDiffRad) % (2.0 * Math.PI));
    if (degrees <= 60.0) {
      return CONSTRUCTIVE;
    } else if (degrees >= 120.0) {
      return DESTRUCTIVE;
    } else {
      return NORMAL;
    }
  }

  /**
   * 获取伤害倍率。
   *
   * @return 伤害倍率
   */
  public double getDamageMultiplier() {
    return switch (this) {
      case CONSTRUCTIVE -> 1.25;
      case NORMAL -> 1.0;
      case DESTRUCTIVE -> 0.75;
    };
  }
}
