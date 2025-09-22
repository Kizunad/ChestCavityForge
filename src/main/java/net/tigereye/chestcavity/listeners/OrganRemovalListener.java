package net.tigereye.chestcavity.listeners;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;

/**
 * Implemented by organs that need to react when they are removed from the chest cavity.
 */
public interface OrganRemovalListener {

    /**
     * Called when the organ stack providing this listener is removed from the chest cavity inventory.
     *
     * @param entity the entity whose chest cavity contained the organ
     * @param cc     the chest cavity instance for that entity
     * @param organ  the organ stack providing this listener
     */
    void onRemoved(LivingEntity entity, ChestCavityInstance cc, ItemStack organ);
}
