package net.tigereye.chestcavity.compat.guzhenren.linkage.policy;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.linkage.ActiveLinkageContext;
import net.tigereye.chestcavity.compat.guzhenren.linkage.LinkageChannel;
import net.tigereye.chestcavity.compat.guzhenren.linkage.LinkagePolicy;

/**
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
        return Mth.clamp(proposedValue, min, max);
    }
}
