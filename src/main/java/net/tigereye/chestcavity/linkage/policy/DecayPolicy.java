package net.tigereye.chestcavity.linkage.policy;

import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.linkage.ActiveLinkageContext;
import net.tigereye.chestcavity.linkage.LinkageChannel;
import net.tigereye.chestcavity.linkage.LinkagePolicy;

/**
 * Relocated from the compat.guzhenren linkage package.
 *
 * <p>Applies a linear decay towards zero each slow tick. The channel can never overshoot past the
 * origin.
 */
public final class DecayPolicy implements LinkagePolicy {

  private final double amountPerTick;

  public DecayPolicy(double amountPerTick) {
    this.amountPerTick = Math.max(0.0, amountPerTick);
  }

  @Override
  public void tick(
      LinkageChannel channel,
      ActiveLinkageContext context,
      net.minecraft.world.entity.LivingEntity entity,
      ChestCavityInstance chestCavity) {
    if (amountPerTick <= 0.0) {
      return;
    }
    double value = channel.get();
    if (Math.abs(value) <= 1.0e-5) {
      if (value != 0.0) {
        if (ChestCavity.LOGGER.isTraceEnabled()) {
          ChestCavity.LOGGER.trace(
              "[Guzhenren] DecayPolicy snapped {} to zero on channel {}",
              String.format("%.6f", value),
              channel.id());
        }
        channel.set(0.0);
      }
      return;
    }
    double delta = Math.min(Math.abs(value), amountPerTick);
    double result = value > 0 ? value - delta : value + delta;
    if (ChestCavity.LOGGER.isTraceEnabled()) {
      ChestCavity.LOGGER.trace(
          "[Guzhenren] DecayPolicy adjusted channel {} by {} ({} -> {})",
          channel.id(),
          String.format("%.3f", delta * (value > 0 ? 1 : -1)),
          String.format("%.3f", value),
          String.format("%.3f", result));
    }
    channel.set(result);
  }
}
