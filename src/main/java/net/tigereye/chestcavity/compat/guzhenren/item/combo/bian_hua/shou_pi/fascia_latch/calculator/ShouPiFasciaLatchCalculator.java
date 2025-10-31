package net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.shou_pi.fascia_latch.calculator;

import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.shou_pi_gu.tuning.ShouPiGuTuning;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.shou_pi.fascia_latch.tuning.ShouPiFasciaLatchTuning;

/** 筋膜锁扣的纯逻辑计算器。 */
public final class ShouPiFasciaLatchCalculator {
  private ShouPiFasciaLatchCalculator() {}

  public static FasciaParameters compute(int fasciaHits, boolean hasTigerGu, boolean hasTieGuGu) {
    if (fasciaHits < ShouPiGuTuning.FASCIA_TRIGGER) {
      throw new IllegalArgumentException("fascia latch requires trigger count");
    }
    double shield = ShouPiComboLogic.applyDaoHenBuff(
        ShouPiFasciaLatchTuning.BASE_SHIELD, snapshot.daoHen());
    if (hasTieGuGu) {
      shield += ShouPiComboLogic.applyDaoHenBuff(
          ShouPiFasciaLatchTuning.IRON_EXTRA_SHIELD, snapshot.daoHen());
    }
    return new FasciaParameters(
        ShouPiGuTuning.FASCIA_ACTIVE_REDUCTION,
        ShouPiFasciaLatchTuning.EFFECT_DURATION_TICKS,
        shield,
        hasTieGuGu,
        ShouPiComboLogic.applyDaoHenBuff(
            ShouPiFasciaLatchTuning.SHOCKWAVE_RADIUS, snapshot.daoHen()),
        ShouPiComboLogic.applyDaoHenBuff(
            ShouPiFasciaLatchTuning.SHOCKWAVE_STRENGTH, snapshot.daoHen()),
        hasTigerGu,
        ShouPiFasciaLatchTuning.TENACITY_DURATION_TICKS,
        ShouPiComboLogic.applyDaoHenBuff(
            ShouPiFasciaLatchTuning.TENACITY_KNOCKBACK_RESIST, snapshot.daoHen()),
        ShouPiComboLogic.computeCooldown(
            ShouPiFasciaLatchTuning.COOLDOWN_TICKS, snapshot.flowExperience()));
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

