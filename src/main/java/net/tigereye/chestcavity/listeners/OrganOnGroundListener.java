package net.tigereye.chestcavity.listeners;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;

/**
 * Implemented by organs that need to react each tick while the owner is standing on solid ground.
 */
public interface OrganOnGroundListener {

  /**
   * Called once per tick when the owning entity is on the ground.
   *
   * @param entity the entity currently on the ground
   * @param cc the chest cavity instance for that entity
   * @param organ the organ stack providing this listener
   */
  void onGroundTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ);
}
