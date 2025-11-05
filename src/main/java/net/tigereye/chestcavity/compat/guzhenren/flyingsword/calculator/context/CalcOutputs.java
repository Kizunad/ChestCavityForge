package net.tigereye.chestcavity.compat.guzhenren.flyingsword.calculator.context;

/**
 * 计算输出（可被钩子累积修改的系数/结果）。
 */
public class CalcOutputs {
  // 速度系数
  public double speedBaseMult = 1.0;
  public double speedMaxMult = 1.0;
  public double accelMult = 1.0;

  // 伤害系数
  public double damageMult = 1.0;

  // 维持、耐久、冷却
  public double upkeepMult = 1.0;
  public double durabilityLossMult = 1.0;
  public int attackCooldownTicks = -1; // <0 表示未指定，使用基准

  public static CalcOutputs create() {
    return new CalcOutputs();
  }
}
