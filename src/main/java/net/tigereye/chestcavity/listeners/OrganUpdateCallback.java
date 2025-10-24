package net.tigereye.chestcavity.listeners;

import net.minecraft.world.entity.LivingEntity;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;

public class OrganUpdateCallback {
  public static void organUpdate(LivingEntity entity, ChestCavityInstance cc) {
    OrganUpdateListeners.callMethods(entity, cc);
  }
}
