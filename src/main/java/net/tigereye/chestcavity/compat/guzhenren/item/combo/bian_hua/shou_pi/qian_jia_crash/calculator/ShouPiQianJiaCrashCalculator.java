package net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.shou_pi.qian_jia_crash.calculator;

import net.tigereye.chestcavity.compat.common.tuning.ShouPiGuTuning;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.shou_pi.qian_jia_crash.tuning.ShouPiQianJiaCrashTuning;

/** 嵌甲冲撞的纯逻辑计算器（安全默认实现，移除未实现依赖）。 */
public final class ShouPiQianJiaCrashCalculator {
  private ShouPiQianJiaCrashCalculator() {}

  public static CrashParameters compute(
      double softPool, double attackDamage, int armorSynergyCount) {
    if (armorSynergyCount <= 0) {
      throw new IllegalArgumentException("crash combo requires at least one synergy organ");
    }
    int cappedSynergy = Math.min(armorSynergyCount, 2);
    double ratio = ShouPiQianJiaCrashTuning.BASE_REFLECT_RATIO
        + (cappedSynergy >= 2 ? ShouPiQianJiaCrashTuning.DUAL_REFLECT_BONUS : 0.0);
    double damage = Math.max(0.0D, softPool * ratio);
    double cap = ShouPiQianJiaCrashTuning.BASE_DAMAGE_CAP
        + attackDamage * ShouPiQianJiaCrashTuning.ATTACK_SCALE
        + (cappedSynergy >= 2 ? ShouPiQianJiaCrashTuning.DUAL_DAMAGE_CAP_BONUS : 0.0);
    damage = Math.min(damage, cap);
    double radius = ShouPiGuTuning.CRASH_SPLASH_RADIUS
        + (cappedSynergy >= 2 ? ShouPiQianJiaCrashTuning.DUAL_RADIUS_BONUS : 0.0);
    long cooldown = ShouPiQianJiaCrashTuning.COOLDOWN_TICKS;
    return new CrashParameters(damage, radius, cooldown);
  }

  /** 嵌甲冲撞输出参数。 */
  public record CrashParameters(double damage, double radius, long cooldown) {}
}
