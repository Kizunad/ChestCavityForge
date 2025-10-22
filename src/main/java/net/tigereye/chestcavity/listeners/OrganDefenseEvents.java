package net.tigereye.chestcavity.listeners;

import java.util.Optional;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.interfaces.ChestCavityEntity;
import net.tigereye.chestcavity.util.ChestCavityUtil;

/** Subscribes to incoming damage so organs can adjust the final amount. */
@EventBusSubscriber(modid = ChestCavity.MODID)
public final class OrganDefenseEvents {

  private OrganDefenseEvents() {}

  @SubscribeEvent
  public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
    LivingEntity victim = event.getEntity();
    float incoming = event.getAmount();
    if (incoming <= 0f) {
      return;
    }

    float modified = incoming;

    if (event.getSource().getEntity() instanceof LivingEntity attacker) {
      Optional<ChestCavityEntity> attackerCavity = ChestCavityEntity.of(attacker);
      if (attackerCavity.isPresent()) {
        float adjusted =
            ChestCavityUtil.onHit(
                attackerCavity.get().getChestCavityInstance(), event.getSource(), victim, modified);
        if (adjusted != modified) {
          modified = Math.max(0f, adjusted);
        }
      }
    }

    Optional<ChestCavityEntity> defender = ChestCavityEntity.of(victim);
    if (defender.isPresent() && modified > 0f) {
      float defended =
          ChestCavityUtil.onIncomingDamage(
              defender.get().getChestCavityInstance(), event.getSource(), modified);
      if (defended != modified) {
        modified = Math.max(0f, defended);
      }
    }

    if (modified != incoming) {
      event.setAmount(modified);
    }
  }
}
