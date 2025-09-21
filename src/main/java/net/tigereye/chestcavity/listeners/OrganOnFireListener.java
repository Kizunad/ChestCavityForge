package net.tigereye.chestcavity.listeners;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;

/**
 * Implemented by organs that need to react while the owner is burning.
 */
public interface OrganOnFireListener {

    /**
     * Called once per tick while the owning entity is on fire.
     *
     * @param entity the burning entity that owns the chest cavity
     * @param cc     the chest cavity instance for that entity
     * @param organ  the organ stack providing this listener
     */
    void onFireTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ);
}

