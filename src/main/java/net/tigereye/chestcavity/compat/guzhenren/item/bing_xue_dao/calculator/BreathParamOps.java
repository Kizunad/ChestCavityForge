package net.tigereye.chestcavity.compat.guzhenren.item.bing_xue_dao.calculator;

import net.tigereye.chestcavity.compat.guzhenren.item.bing_xue_dao.calculator.CooldownOps;
import net.tigereye.chestcavity.config.CCConfig;

/** 霜息参数计算（纯逻辑）。 */
public final class BreathParamOps {
  private BreathParamOps() {}

  public record BreathParams(
      double range,
      double coneDotThreshold,
      double frostbiteChance,
      double frostbitePercent,
      int frostbiteDurationSeconds,
      long cooldownTicks,
      int particleSteps,
      double particleSpacing) {}

  public static BreathParams compute(
      CCConfig.GuzhenrenBingXueDaoConfig.ShuangXiGuConfig cfg, double daohen, int liupaiExp) {
    double multiplier = 1.0 + Math.max(0.0, daohen);
    double range = Math.max(0.0D, cfg.abilityRange);
    double cone = cfg.coneDotThreshold;
    double chance = Math.max(0.0D, Math.min(1.0D, cfg.frostbiteChance));
    double percent = Math.max(0.0D, cfg.frostbiteDamagePercent) * multiplier;
    int seconds = Math.max(0, cfg.frostbiteDurationSeconds);
    long cooldown = CooldownOps.withBingXueExp(cfg.baseCooldownTicks, liupaiExp);
    int steps = Math.max(0, cfg.breathParticleSteps);
    double spacing = cfg.breathParticleSpacing;
    return new BreathParams(range, cone, chance, percent, seconds, cooldown, steps, spacing);
  }
}

