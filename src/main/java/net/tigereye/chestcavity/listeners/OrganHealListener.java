package net.tigereye.chestcavity.listeners;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;

/**
 * Implemented by organs that provide generic healing.
 * Implementations should be side-agnostic; healing is applied server-side.
 */
public interface OrganHealListener {

    /**
     * Return the healing amount to apply this tick.
     *  - Return 0 for no healing.
     *  - Positive values heal the owner (clamped by max health).
     *
     * @param entity the entity to heal
     * @param cc     the chest cavity instance for that entity
     * @param organ  the organ stack providing this listener
     * @return amount of health to heal this tick
     */
    float getHealingPerTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ);
}

