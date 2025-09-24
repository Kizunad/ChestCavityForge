package net.tigereye.chestcavity.compat.guzhenren.item.xue_dao;

import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

import net.tigereye.chestcavity.compat.guzhenren.item.xue_dao.behavior.XueFeiguOrganBehavior;
import net.tigereye.chestcavity.registration.CCKeybindings;

/**
  Client-side ability registrations for 血道 organs.
  Ensures Xue Dao active abilities share the common attack keybinding on the client.
 */
public final class XueDaoClientAbilities {

    private XueDaoClientAbilities() {
    }

    public static void onClientSetup(FMLClientSetupEvent event) {

        if (!CCKeybindings.ATTACK_ABILITY_LIST.contains(XueFeiguOrganBehavior.ABILITY_ID)) {
            CCKeybindings.ATTACK_ABILITY_LIST.add(XueFeiguOrganBehavior.ABILITY_ID);
        }
        if (!CCKeybindings.ATTACK_ABILITY_LIST.contains(XiediguOrganBehavior.ABILITY_ID)) {
            CCKeybindings.ATTACK_ABILITY_LIST.add(XiediguOrganBehavior.ABILITY_ID);

        }
    }  
}
