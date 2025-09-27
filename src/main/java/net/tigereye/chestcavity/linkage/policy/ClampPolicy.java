package net.tigereye.chestcavity.linkage.policy;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.linkage.ActiveLinkageContext;
import net.tigereye.chestcavity.linkage.LinkageChannel;
import net.tigereye.chestcavity.linkage.LinkagePolicy;

/**
 * Relocated from the compat.guzhenren linkage package.
 *
 * Clamps the channel's value within an inclusive range every time it is written to.
 */
public final class ClampPolicy implements LinkagePolicy {

    private final double min;
    private final double max;

    public ClampPolicy(double min, double max) {
        this.min = min;
        this.max = max;
    }

    @Override
    public double apply(LinkageChannel channel, double previousValue, double proposedValue,
                        ActiveLinkageContext context, LivingEntity entity, ChestCavityInstance chestCavity) {
        double clamped = Mth.clamp(proposedValue, min, max);
        if (ChestCavity.LOGGER.isDebugEnabled() && Double.compare(clamped, proposedValue) != 0) {
            ChestCavity.LOGGER.debug(
                    "[Guzhenren] ClampPolicy {} -> {} on channel {} (range {}-{})",
                    String.format("%.3f", proposedValue),
                    String.format("%.3f", clamped),
                    channel.id(),
                    String.format("%.3f", min),
                    String.format("%.3f", max)
            );
        }
        return clamped;
    }
}
