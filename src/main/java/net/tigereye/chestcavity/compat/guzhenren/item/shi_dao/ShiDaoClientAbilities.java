package net.tigereye.chestcavity.compat.guzhenren.item.shi_dao;

import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.tigereye.chestcavity.compat.guzhenren.item.shi_dao.behavior.JiuChongOrganBehavior;
import net.tigereye.chestcavity.registration.CCKeybindings;

/**
 * Ensures Shi Dao attack abilities are bound to the shared attack keybinding on the client.
 */
public final class ShiDaoClientAbilities {

    private ShiDaoClientAbilities() {
    }

    public static void onClientSetup(FMLClientSetupEvent event) {
        if (!CCKeybindings.ATTACK_ABILITY_LIST.contains(JiuChongOrganBehavior.ABILITY_ID)) {
            CCKeybindings.ATTACK_ABILITY_LIST.add(JiuChongOrganBehavior.ABILITY_ID);
        }
    }
}
