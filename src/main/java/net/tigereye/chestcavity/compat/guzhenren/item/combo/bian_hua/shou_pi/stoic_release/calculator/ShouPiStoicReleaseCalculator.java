package net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.shou_pi.stoic_release.calculator;

import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.tuning.ShouPiGuTuning;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.shou_pi.stoic_release.tuning.ShouPiStoicReleaseTuning;

/** 坚忍释放的纯逻辑计算器（安全默认实现，移除未实现依赖）。 */
public final class ShouPiStoicReleaseCalculator {
  private ShouPiStoicReleaseCalculator() {}

  public static StoicParameters compute(ShouPiGuTuning.TierParameters tierParams) {
    boolean applySlow = tierParams.stage() == ShouPiGuTuning.Tier.STAGE5;
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
