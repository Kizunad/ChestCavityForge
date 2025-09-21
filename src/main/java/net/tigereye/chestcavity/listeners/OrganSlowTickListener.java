package net.tigereye.chestcavity.listeners;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;

/**
 * Implemented by organs that should only tick at a slow interval (default once per second).
 */
public interface OrganSlowTickListener {

    /**
     * Called every slow tick (20 game ticks by default) while the organ is present.
     */
    void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ);
}

