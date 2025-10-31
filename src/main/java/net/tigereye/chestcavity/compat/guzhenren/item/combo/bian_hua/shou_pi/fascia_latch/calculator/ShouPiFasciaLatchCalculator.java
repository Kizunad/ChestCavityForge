package net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.shou_pi.fascia_latch.calculator;

import net.tigereye.chestcavity.compat.common.tuning.ShouPiGuTuning;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.shou_pi.fascia_latch.tuning.ShouPiFasciaLatchTuning;

/** 筋膜锁扣的纯逻辑计算器（安全默认实现，去除未实现依赖）。 */
public final class ShouPiFasciaLatchCalculator {
  private ShouPiFasciaLatchCalculator() {}

  public static FasciaParameters compute(int fasciaHits, boolean hasTigerGu, boolean hasTieGuGu) {
    if (fasciaHits < ShouPiGuTuning.FASCIA_TRIGGER) {
      throw new IllegalArgumentException("fascia latch requires trigger count");
    }
    double shield = ShouPiFasciaLatchTuning.BASE_SHIELD + (hasTieGuGu ? ShouPiFasciaLatchTuning.IRON_EXTRA_SHIELD : 0.0);
    double shockwaveRadius = hasTieGuGu ? ShouPiFasciaLatchTuning.SHOCKWAVE_RADIUS : 0.0;
    double shockwaveStrength = hasTieGuGu ? ShouPiFasciaLatchTuning.SHOCKWAVE_STRENGTH : 0.0;
    double tenacityKnockbackResist = hasTigerGu ? ShouPiFasciaLatchTuning.TENACITY_KNOCKBACK_RESIST : 0.0;
    long cooldown = ShouPiFasciaLatchTuning.COOLDOWN_TICKS;
    return new FasciaParameters(
        ShouPiGuTuning.FASCIA_ACTIVE_REDUCTION,
        ShouPiFasciaLatchTuning.EFFECT_DURATION_TICKS,
        shield,
        hasTieGuGu,
        shockwaveRadius,
        shockwaveStrength,
        hasTigerGu,
        ShouPiFasciaLatchTuning.TENACITY_DURATION_TICKS,
        tenacityKnockbackResist,
        cooldown);
  }

  /** 筋膜锁扣计算结果。 */
  public record FasciaParameters(
      double damageReduction,
      int durationTicks,
      double shieldAmount,
      boolean applyShockwave,
      double shockwaveRadius,
      double shockwaveStrength,
      boolean grantTenacity,
      int tenacityDurationTicks,
      double tenacityKnockbackResist,
      long cooldown) {}
}
