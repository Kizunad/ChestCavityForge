package net.tigereye.chestcavity.compat.guzhenren.item.mu_dao;

import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.tigereye.chestcavity.compat.guzhenren.item.mu_dao.behavior.LiandaoGuOrganBehavior;
import net.tigereye.chestcavity.registration.CCKeybindings;

/**
 * Ensures Mu Dao attack abilities are bound to the shared attack keybinding on the client.
 */
public final class MuDaoClientAbilities {

    private MuDaoClientAbilities() {
    }

    public static void onClientSetup(FMLClientSetupEvent event) {
        if (!CCKeybindings.ATTACK_ABILITY_LIST.contains(LiandaoGuOrganBehavior.ABILITY_ID)) {
            CCKeybindings.ATTACK_ABILITY_LIST.add(LiandaoGuOrganBehavior.ABILITY_ID);
        }
    }
}
