package net.tigereye.chestcavity.listeners;

import java.util.Optional;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.interfaces.ChestCavityEntity;
import net.tigereye.chestcavity.util.ChestCavityUtil;

@EventBusSubscriber(modid = ChestCavity.MODID)
public final class OrganRuntimeEvents {

  private OrganRuntimeEvents() {}

  @SubscribeEvent
  public static void onLivingTick(EntityTickEvent.Post event) {
    if (!(event.getEntity() instanceof LivingEntity living)) {
      return;
    }

    Optional<ChestCavityEntity> chestCavity = ChestCavityEntity.of(living);
    if (chestCavity.isEmpty()) {
      return;
    }

    ChestCavityUtil.onTick(chestCavity.get().getChestCavityInstance());
  }
}
