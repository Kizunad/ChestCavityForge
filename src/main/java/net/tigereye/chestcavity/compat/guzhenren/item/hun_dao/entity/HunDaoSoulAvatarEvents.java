package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.entity;

import net.minecraft.world.entity.EntityDimensions;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityEvent;
import net.tigereye.chestcavity.ChestCavity;

/** Forge events for Hun Dao soul avatars (size overrides, etc.). */
@EventBusSubscriber(modid = ChestCavity.MODID)
public final class HunDaoSoulAvatarEvents {

  private HunDaoSoulAvatarEvents() {}

  @SubscribeEvent
  public static void onEntitySize(EntityEvent.Size event) {
    if (!(event.getEntity() instanceof HunDaoSoulAvatarEntity avatar)) {
      return;
    }
    double multiplier = avatar.getHunpoScaleMultiplier();
    if (multiplier <= 1.0D + 1.0E-4D) {
      return;
    }
    EntityDimensions scaled = event.getNewSize().scale((float) multiplier);
    event.setNewSize(scaled);
  }
}
