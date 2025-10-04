package net.tigereye.chestcavity.compat.guzhenren.util;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.mob_effect.SoulBeastIntimidatedEffect;
import net.tigereye.chestcavity.registration.CCStatusEffects;

/**
 * Event hooks ensuring the soul beast intimidation flee goal lifecycle tracks the effect.
 */
@EventBusSubscriber(modid = ChestCavity.MODID)
public final class SoulBeastIntimidationEvents {

    private SoulBeastIntimidationEvents() {}

    @SubscribeEvent
    public static void onEffectAdded(MobEffectEvent.Added event) {
        LivingEntity entity = event.getEntity();
        MobEffectInstance instance = event.getEffectInstance();
        if (entity == null || instance == null) {
            return;
        }
        if (instance.getEffect() != CCStatusEffects.SOUL_BEAST_INTIMIDATED.value()) {
            return;
        }
        SoulBeastIntimidatedEffect.handleEffectAdded(entity);
    }

    @SubscribeEvent
    public static void onEffectRemoved(MobEffectEvent.Remove event) {
        LivingEntity entity = event.getEntity();
        MobEffectInstance instance = event.getEffectInstance();
        if (entity == null || instance == null) {
            return;
        }
        if (instance.getEffect() != CCStatusEffects.SOUL_BEAST_INTIMIDATED.value()) {
            return;
        }
        SoulBeastIntimidatedEffect.handleEffectRemoved(entity);
    }
}
