package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.calculator;

import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.AIMode;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.tuning.FlyingSwordTuning;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.calculator.context.CalcContext;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.calculator.context.CalcOutputs;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.calculator.hooks.FlyingSwordCalcRegistry;

/**
 * 飞剑核心计算器（纯函数，无副作用）
 * 所有计算都是纯函数，便于测试和推导
 */
public final class FlyingSwordCalculator {
  private FlyingSwordCalculator() {}

  /**
   * 速度²伤害公式
   * damage = (basedam * levelScale) × [1 + (v/vRef)² × velCoef]
   *
   * @param baseDamage 基础伤害
   * @param velocity 当前速度（方块/tick）
   * @param vRef 参考速度
   * @param velDmgCoef 速度²系数
   * @param levelScale 等级缩放（1 + level * factor）
   * @return 最终伤害
   */
  public static double calculateDamage(
      double baseDamage,
      double velocity,
      double vRef,
      double velDmgCoef,
      double levelScale) {
    if (baseDamage <= 0 || velocity < 0 || vRef <= 0 || levelScale <= 0) {
      return 0;
    }

    double speedFactor = Math.pow(velocity / vRef, 2.0) * velDmgCoef;
    return baseDamage * levelScale * (1.0 + speedFactor);
  }

  /**
   * 计算等级缩放系数
   *
   * @param level 当前等级
   * @param damagePerLevel 每级伤害增长
   * @return 缩放系数
   */
  public static double calculateLevelScale(int level, double damagePerLevel) {
    return 1.0 + (Math.max(0, level - 1)) * damagePerLevel / FlyingSwordTuning.DAMAGE_BASE;
  }

  /**
   * 维持消耗计算
   * cost = baseRate × modeMultiplier × stateMultiplier × speedMultiplier
   *
   * @param baseRate 基础消耗率
   * @param mode AI模式
   * @param sprinting 主人是否在冲刺
   * @param breaking 是否在破块
   * @param speedPercent 速度百分比（0.0-1.0）
   * @return 每秒消耗量
   */
  public static double calculateUpkeep(
      double baseRate, AIMode mode, boolean sprinting, boolean breaking, double speedPercent) {
    // 模式倍率
    double modeFactor =
        switch (mode) {
          case ORBIT -> FlyingSwordTuning.UPKEEP_ORBIT_MULT;
          case GUARD -> FlyingSwordTuning.UPKEEP_GUARD_MULT;
          case HUNT -> FlyingSwordTuning.UPKEEP_HUNT_MULT;
        };

    // 状态倍率
    double stateFactor = 1.0;
    if (sprinting) stateFactor *= FlyingSwordTuning.UPKEEP_SPRINT_MULT;
    if (breaking) stateFactor *= FlyingSwordTuning.UPKEEP_BREAK_MULT;

    // 速度倍率（速度越快消耗越高）
    double speedFactor = 1.0 + (speedPercent - 0.5) * FlyingSwordTuning.UPKEEP_SPEED_SCALE;
    speedFactor = Math.max(0.5, speedFactor); // 下限0.5

    return baseRate * modeFactor * stateFactor * speedFactor;
  }

  /**
   * 维持消耗（带上下文钩子）
   */
  public static double calculateUpkeepWithContext(
      double baseRate,
      AIMode mode,
      boolean sprinting,
      boolean breaking,
      double speedPercent,
      CalcContext ctx) {
    double base = calculateUpkeep(baseRate, mode, sprinting, breaking, speedPercent);
    CalcOutputs out = FlyingSwordCalcRegistry.applyAll(ctx);
    return Math.max(0.0, base * out.upkeepMult);
  }

  /**
   * 经验计算
   *
   * @param damageDealt 造成的伤害
   * @param isKill 是否击杀
   * @param isElite 是否为精英
   * @param expMultiplier 额外经验倍率
   * @return 获得的经验
   */
  public static int calculateExpGain(
      double damageDealt, boolean isKill, boolean isElite, double expMultiplier) {
    int base = (int) (damageDealt * FlyingSwordTuning.EXP_PER_DAMAGE);
    if (isKill) base *= FlyingSwordTuning.EXP_KILL_MULT;
    if (isElite) base *= FlyingSwordTuning.EXP_ELITE_MULT;
    return Math.max(0, (int) (base * expMultiplier));
  }

  /**
   * 计算升级所需经验
   * expToNext = base × (1 + level) ^ alpha
   *
   * @param level 当前等级
   * @return 升到下一级所需经验
   */
  public static int calculateExpToNext(int level) {
    if (level >= FlyingSwordTuning.MAX_LEVEL) {
      return Integer.MAX_VALUE;
    }
    double exp =
        FlyingSwordTuning.EXP_BASE * Math.pow(1 + level, FlyingSwordTuning.EXP_ALPHA);
    return (int) Math.round(exp);
  }

  /**
   * 耐久损耗计算
   *
   * @param damage 造成的伤害
   * @param lossRatio 损耗比例
   * @param breaking 是否在破块
   * @return 耐久损耗量
   */
  public static float calculateDurabilityLoss(float damage, double lossRatio, boolean breaking) {
    float loss = damage * (float) lossRatio;
    if (breaking) loss *= (float) FlyingSwordTuning.DURA_BREAK_MULT;
    return Math.max(0, loss);
  }

  /**
   * 耐久损耗（带上下文钩子）
   */
  public static float calculateDurabilityLossWithContext(
      float damage, double lossRatio, boolean breaking, CalcContext ctx) {
    float base = calculateDurabilityLoss(damage, lossRatio, breaking);
    CalcOutputs out = FlyingSwordCalcRegistry.applyAll(ctx);
    double v = Math.max(0.0, base * out.durabilityLossMult);
    return (float) v;
  }

  /**
   * 伤害（带上下文钩子）
   */
  public static double calculateDamageWithContext(
      double baseDamage,
      double velocity,
      double vRef,
      double velDmgCoef,
      double levelScale,
      CalcContext ctx) {
    double base = calculateDamage(baseDamage, velocity, vRef, velDmgCoef, levelScale);
    CalcOutputs out = FlyingSwordCalcRegistry.applyAll(ctx);
    return Math.max(0.0, base * out.damageMult);
  }

  /**
   * 计算攻击冷却（ticks）。钩子可直接覆盖或按倍数修改。
   */
  public static int calculateAttackCooldownTicks(CalcContext ctx, int baseTicks) {
    CalcOutputs out = FlyingSwordCalcRegistry.applyAll(ctx);
    int result = out.attackCooldownTicks >= 0 ? out.attackCooldownTicks : baseTicks;
    // 防止过小/过大
    return clamp(result, 1, 200);
  }

  /**
   * 有效最大/基础速度（应用钩子系数）。
   */
  public static double effectiveSpeedMax(double baseMax, CalcContext ctx) {
    CalcOutputs out = FlyingSwordCalcRegistry.applyAll(ctx);
    return Math.max(0.0, baseMax * out.speedMaxMult);
  }

  public static double effectiveSpeedBase(double baseBase, CalcContext ctx) {
    CalcOutputs out = FlyingSwordCalcRegistry.applyAll(ctx);
    return Math.max(0.0, baseBase * out.speedBaseMult);
  }

  /**
   * 有效加速度（应用钩子系数）。
   */
  public static double effectiveAccel(double baseAccel, CalcContext ctx) {
    CalcOutputs out = FlyingSwordCalcRegistry.applyAll(ctx);
    return Math.max(0.0, baseAccel * out.accelMult);
  }

  /**
   * 耐久损耗（按硬度）
   *
   * @param hardness 方块硬度
   * @param toolTier 工具等级
   * @return 耐久损耗量
   */
  public static float calculateDurabilityLossFromBlock(float hardness, int toolTier) {
    // 硬度越高，损耗越大；工具等级越高，损耗越小
    float tierFactor = 1.0f / (1.0f + toolTier * 0.25f);
    return hardness * tierFactor;
  }

  /**
   * 限制数值在范围内
   */
  public static double clamp(double value, double min, double max) {
    return Math.max(min, Math.min(max, value));
  }

  /**
   * 限制整数在范围内
   */
  public static int clamp(int value, int min, int max) {
    return Math.max(min, Math.min(max, value));
  }
}
