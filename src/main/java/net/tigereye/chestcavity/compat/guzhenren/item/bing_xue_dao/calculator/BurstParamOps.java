package net.tigereye.chestcavity.compat.guzhenren.item.bing_xue_dao.calculator;

import net.tigereye.chestcavity.config.CCConfig;

/** 冰爆参数计算（纯数学部分）。 */
public final class BurstParamOps {
  private BurstParamOps() {}

  public record BurstParams(double baseDamage, double radius, int slowDurationTicks, int slowAmplifier) {}

  public static BurstParams compute(
      CCConfig.GuzhenrenBingXueDaoConfig.BingJiGuConfig cfg,
      int stacks,
      double daohen,
      boolean hasBingBao) {
    double multiplier = 1.0 + Math.max(0.0, daohen);
    double baseDamage = cfg.iceBurstBaseDamage * Math.pow(cfg.iceBurstStackDamageScale, Math.max(0, stacks - 1));
    if (hasBingBao) {
      baseDamage *= (1.0 + cfg.iceBurstBingBaoMultiplier);
    }
    baseDamage *= multiplier;
    double radius = cfg.iceBurstRadius + Math.max(0, stacks - 1) * cfg.iceBurstRadiusPerStack;
    radius *= multiplier;
    int slowDuration = Math.max(0, cfg.iceBurstSlowDurationTicks);
    int slowAmplifier = Math.max(0, (int) Math.round(cfg.iceBurstSlowAmplifier));
    return new BurstParams(baseDamage, radius, slowDuration, slowAmplifier);
  }
}

