package net.tigereye.chestcavity.listeners;

import java.util.Optional;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.interfaces.ChestCavityEntity;

public class OrganAddStatusEffectCallback {

  public static MobEffectInstance organAddMobEffect(
      LivingEntity entity, MobEffectInstance mobEffectInstance) {
    Optional<ChestCavityEntity> optional = ChestCavityEntity.of(entity);
    if (optional.isPresent()) {
      ChestCavityEntity chestCavityEntity = optional.get();
      ChestCavityInstance chestCavityInstance = chestCavityEntity.getChestCavityInstance();
      return OrganAddStatusEffectListeners.callMethods(
          entity, chestCavityInstance, mobEffectInstance);
    }
    return mobEffectInstance;
  }
}
