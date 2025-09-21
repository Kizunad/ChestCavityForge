package net.tigereye.chestcavity.compat.guzhenren.linkage.policy;

import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.linkage.ActiveLinkageContext;
import net.tigereye.chestcavity.compat.guzhenren.linkage.LinkageChannel;
import net.tigereye.chestcavity.compat.guzhenren.linkage.LinkagePolicy;

/**
 * Applies a linear decay towards zero each slow tick. The channel can never overshoot past the origin.
 */
public final class DecayPolicy implements LinkagePolicy {

    private final double amountPerTick;

    public DecayPolicy(double amountPerTick) {
        this.amountPerTick = Math.max(0.0, amountPerTick);
    }

    @Override
    public void tick(LinkageChannel channel, ActiveLinkageContext context,
                     net.minecraft.world.entity.LivingEntity entity, ChestCavityInstance chestCavity) {
        if (amountPerTick <= 0.0) {
            return;
        }
        double value = channel.get();
        if (Math.abs(value) <= 1.0e-5) {
            if (value != 0.0) {
                channel.set(0.0);
            }
            return;
        }
        double delta = Math.min(Math.abs(value), amountPerTick);
        double result = value > 0 ? value - delta : value + delta;
        channel.set(result);
    }
}
