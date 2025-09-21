package net.tigereye.chestcavity.compat.guzhenren.linkage;

import net.minecraft.world.entity.LivingEntity;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;

/**
 * Policies can transform or clamp channel values and update them during the slow-tick phase
 * (e.g. decay, saturation). They are optional and only applied when explicitly attached to a channel.
 */
public interface LinkagePolicy {

    /**
     * Allows a policy to mutate the proposed value before it is committed. The return value becomes the
     * channel's new value. Policies are consulted in registration order.
     */
    default double apply(LinkageChannel channel, double previousValue, double proposedValue,
                         ActiveLinkageContext context, LivingEntity entity, ChestCavityInstance chestCavity) {
        return proposedValue;
    }

    /** Called once per slow tick before triggers to allow the policy to adjust the channel. */
    default void tick(LinkageChannel channel, ActiveLinkageContext context,
                      LivingEntity entity, ChestCavityInstance chestCavity) {
    }
}
