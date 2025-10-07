package net.tigereye.chestcavity.compat.guzhenren.item.yan_dao;

import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.tigereye.chestcavity.compat.guzhenren.item.yan_dao.behavior.HuoYiGuOrganBehavior;
import net.tigereye.chestcavity.registration.CCKeybindings;

/**
 * Ensures Yan Dao attack abilities are exposed to the shared attack hotkey on the client.
 */
public final class YanDaoClientAbilities {

    private YanDaoClientAbilities() {
    }

    public static void onClientSetup(FMLClientSetupEvent event) {
        if (!CCKeybindings.ATTACK_ABILITY_LIST.contains(HuoYiGuOrganBehavior.ABILITY_ID)) {
            CCKeybindings.ATTACK_ABILITY_LIST.add(HuoYiGuOrganBehavior.ABILITY_ID);
        }
    }
}
