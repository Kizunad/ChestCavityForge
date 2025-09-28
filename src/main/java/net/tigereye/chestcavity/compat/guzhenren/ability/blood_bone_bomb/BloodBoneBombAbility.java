package net.tigereye.chestcavity.compat.guzhenren.ability.blood_bone_bomb;

import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;

/**
 * @deprecated Use {@link net.tigereye.chestcavity.guscript.ability.guzhenren.blood_bone_bomb.BloodBoneBombAbility} instead.
 */
@Deprecated(forRemoval = false)
@EventBusSubscriber(modid = ChestCavity.MODID)
public final class BloodBoneBombAbility {

    private BloodBoneBombAbility() {
    }

    public static void tryActivate(LivingEntity entity, ChestCavityInstance cc) {
        net.tigereye.chestcavity.guscript.ability.guzhenren.blood_bone_bomb.BloodBoneBombAbility.tryActivate(entity, cc);
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        net.tigereye.chestcavity.guscript.ability.guzhenren.blood_bone_bomb.BloodBoneBombAbility.onEntityTick(event);
    }
}
