package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.calculator;

import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.tuning.JianYingTuning;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.StandardScaling;

/**
 * 剑影蛊纯计算模块：不触碰 MC 实体/世界，仅根据入参返回结果。
 */
public final class JianYingCalculator {

  private JianYingCalculator() {}

  /**
   * 计算本次被动影袭的倍率。
   *
   * @param lastTriggerTick 上次触发的游戏刻
   * @param nowTick 当前刻
   * @param lastMultiplier 上次倍率（若超出窗口则忽略）
   */
  public static float passiveMultiplier(long lastTriggerTick, long nowTick, float lastMultiplier) {
    if (nowTick - lastTriggerTick > JianYingTuning.PASSIVE_RESET_WINDOW_TICKS) {
      return JianYingTuning.PASSIVE_INITIAL_MULTIPLIER;
    }
    double m = Math.max(JianYingTuning.PASSIVE_MIN_MULTIPLIER,
        lastMultiplier - JianYingTuning.PASSIVE_DECAY_STEP);
    return (float) StandardScaling.INSTANCE.clamp(m, 0.0, 1.0);
  }

  /** 计算分身造成的单次伤害（已含家族增伤 efficiency）。 */
  public static float cloneDamage(double efficiency) {
    return (float) (JianYingTuning.BASE_DAMAGE * JianYingTuning.CLONE_DAMAGE_RATIO * efficiency);
  }

  /** 计算残影的 AoE 伤害（已含家族增伤 efficiency）。 */
  public static float afterimageDamage(double efficiency) {
    return (float) (JianYingTuning.BASE_DAMAGE * JianYingTuning.AFTERIMAGE_DAMAGE_RATIO * efficiency);
  }
}

