package net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.shou_pi.stoic_release.calculator;

import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.behavior.ShouPiGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.shou_pi.stoic_release.tuning.ShouPiStoicReleaseTuning;

/** 坚忍释放的纯逻辑计算器。 */
public final class ShouPiStoicReleaseCalculator {
  private ShouPiStoicReleaseCalculator() {}

  public static StoicParameters compute(ShouPiGuOrganBehavior.TierParameters tierParams) {
    boolean applySlow = tierParams.stage() == ShouPiGuOrganBehavior.Tier.STAGE5;
    return new StoicParameters(
        tierParams.stoicMitigation(),
        tierParams.stoicShield(),
        ShouPiStoicReleaseTuning.ACTIVE_DURATION_TICKS,
        tierParams.lockTicks(),
        applySlow,
        ShouPiStoicReleaseTuning.SOFT_REFLECT_BONUS);
  }

  /** 坚忍释放输出参数。 */
  public record StoicParameters(
      double mitigationFraction,
      double shieldAmount,
      int activeDurationTicks,
      long lockTicks,
      boolean applySlowAura,
      double softReflectBonus) {}
}

