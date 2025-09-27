package net.tigereye.chestcavity.linkage;

import net.minecraft.world.entity.LivingEntity;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;

/**
 * Relocated from the compat.guzhenren linkage package.
 *
 * Callback invoked whenever a channel's value changes. Subscribers may treat this passively (READ)
 * by only observing values or actively (FOLLOW) by triggering additional side effects.
 */
@FunctionalInterface
public interface LinkageSubscriber {

    void onChannelUpdated(LinkageChannel channel, double previousValue, ActiveLinkageContext context,
                          LivingEntity entity, ChestCavityInstance chestCavity);
}
