package net.tigereye.chestcavity.linkage.policy;

import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.linkage.ActiveLinkageContext;
import net.tigereye.chestcavity.linkage.LinkageChannel;
import net.tigereye.chestcavity.linkage.LinkagePolicy;

/**
 * Relocated from the compat.guzhenren linkage package.
 *
 * <p>Soft caps the channel: values beyond {@code softCap} are compressed by {@code
 * falloffMultiplier}.
 */
public final class SaturationPolicy implements LinkagePolicy {

  private final double softCap;
  private final double falloffMultiplier;

  public SaturationPolicy(double softCap, double falloffMultiplier) {
    this.softCap = softCap;
    this.falloffMultiplier = Math.max(0.0, Math.min(1.0, falloffMultiplier));
  }

  @Override
  public double apply(
      LinkageChannel channel,
      double previousValue,
      double proposedValue,
      ActiveLinkageContext context,
      net.minecraft.world.entity.LivingEntity entity,
      ChestCavityInstance chestCavity) {
    if (falloffMultiplier >= 1.0) {
      return proposedValue;
    }
    if (proposedValue <= softCap) {
      return proposedValue;
    }
    double overshoot = proposedValue - softCap;
    double result = softCap + overshoot * falloffMultiplier;
    if (ChestCavity.LOGGER.isDebugEnabled()) {
      ChestCavity.LOGGER.debug(
          "[Guzhenren] SaturationPolicy softened {} -> {} on channel {} (softCap {}, falloff {})",
          String.format("%.3f", proposedValue),
          String.format("%.3f", result),
          channel.id(),
          String.format("%.3f", softCap),
          String.format("%.3f", falloffMultiplier));
    }
    return result;
  }
}
