package net.tigereye.chestcavity.listeners;

import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.interfaces.ChestCavityEntity;
import net.tigereye.chestcavity.util.ChestCavityUtil;

import java.util.Optional;

/**
 * Subscribes to incoming damage so organs can adjust the final amount.
 */
@EventBusSubscriber(modid = ChestCavity.MODID)
public final class OrganDefenseEvents {

    private OrganDefenseEvents() {
    }

    @SubscribeEvent
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        Optional<ChestCavityEntity> chestCavity = ChestCavityEntity.of(victim);
        if (chestCavity.isEmpty()) {
            return;
        }

        float original = event.getAmount();
        if (original <= 0f) {
            return;
        }

        float modified = ChestCavityUtil.onIncomingDamage(chestCavity.get().getChestCavityInstance(), event.getSource(), original);
        if (modified != original) {
            event.setAmount(Math.max(0f, modified));
        }
    }
}
