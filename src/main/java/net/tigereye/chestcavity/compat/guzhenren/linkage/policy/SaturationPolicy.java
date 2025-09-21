package net.tigereye.chestcavity.compat.guzhenren.linkage.policy;

import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.linkage.ActiveLinkageContext;
import net.tigereye.chestcavity.compat.guzhenren.linkage.LinkageChannel;
import net.tigereye.chestcavity.compat.guzhenren.linkage.LinkagePolicy;

/**
 * Soft caps the channel: values beyond {@code softCap} are compressed by {@code falloffMultiplier}.
 */
public final class SaturationPolicy implements LinkagePolicy {

    private final double softCap;
    private final double falloffMultiplier;

    public SaturationPolicy(double softCap, double falloffMultiplier) {
        this.softCap = softCap;
        this.falloffMultiplier = Math.max(0.0, Math.min(1.0, falloffMultiplier));
    }

    @Override
    public double apply(LinkageChannel channel, double previousValue, double proposedValue,
                        ActiveLinkageContext context, net.minecraft.world.entity.LivingEntity entity,
                        ChestCavityInstance chestCavity) {
        if (falloffMultiplier >= 1.0) {
            return proposedValue;
        }
        if (proposedValue <= softCap) {
            return proposedValue;
        }
        double overshoot = proposedValue - softCap;
        return softCap + overshoot * falloffMultiplier;
    }
}
