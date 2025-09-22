package net.tigereye.chestcavity.compat.guzhenren.item.gu_dao;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.compat.guzhenren.item.gu_dao.behavior.LuoXuanGuQiangguOrganBehavior;
import net.tigereye.chestcavity.registration.CCKeybindings;

/**
 * Ensures Guzhenren attack abilities are hooked into the shared attack hotkey on the client.
 */
@EventBusSubscriber(modid = ChestCavity.MODID, value = Dist.CLIENT)
public final class GuDaoClientAbilities {
    private GuDaoClientAbilities() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        if (!CCKeybindings.ATTACK_ABILITY_LIST.contains(LuoXuanGuQiangguOrganBehavior.ABILITY_ID)) {
            CCKeybindings.ATTACK_ABILITY_LIST.add(LuoXuanGuQiangguOrganBehavior.ABILITY_ID);
        }
    }
}
