package net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.shou_pi.qian_jia_crash.calculator;

import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.shou_pi_gu.tuning.ShouPiGuTuning;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.shou_pi.qian_jia_crash.tuning.ShouPiQianJiaCrashTuning;

/** 嵌甲冲撞的纯逻辑计算器。 */
public final class ShouPiQianJiaCrashCalculator {
  private ShouPiQianJiaCrashCalculator() {}

  public static CrashParameters compute(
      double softPool, double attackDamage, int armorSynergyCount) {
    if (armorSynergyCount <= 0) {
      throw new IllegalArgumentException("crash combo requires at least one synergy organ");
    }
    int cappedSynergy = Math.min(armorSynergyCount, 2);
    double ratio = ShouPiQianJiaCrashTuning.BASE_REFLECT_RATIO;
    if (cappedSynergy >= 2) {
      ratio += ShouPiQianJiaCrashTuning.DUAL_REFLECT_BONUS;
    }
    double damage = Math.max(0.0D, softPool * ratio);
    double cap =
        ShouPiQianJiaCrashTuning.BASE_DAMAGE_CAP
            + attackDamage * ShouPiQianJiaCrashTuning.ATTACK_SCALE;
    if (cappedSynergy >= 2) {
      cap += ShouPiQianJiaCrashTuning.DUAL_DAMAGE_CAP_BONUS;
    }
    damage = Math.min(damage, cap);
    double radius = ShouPiGuTuning.CRASH_SPLASH_RADIUS;
    if (cappedSynergy >= 2) {
      radius += ShouPiQianJiaCrashTuning.DUAL_RADIUS_BONUS;
    }
    return new CrashParameters(damage, radius);
  }

  /** 嵌甲冲撞输出参数。 */
  public record CrashParameters(double damage, double radius) {}
}

