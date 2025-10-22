package net.tigereye.chestcavity.listeners;

import net.minecraft.world.entity.LivingEntity;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;

public class OrganTickCallback { // todo: convert back to events for compat?
  public static void organTick(LivingEntity entity, ChestCavityInstance cc) {
    OrganTickListeners.callMethods(entity, cc);
  }
}
