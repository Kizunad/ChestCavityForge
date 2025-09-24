package net.tigereye.chestcavity.compat.guzhenren.item.xue_dao;

import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.tigereye.chestcavity.compat.guzhenren.item.xue_dao.behavior.XueFeiguOrganBehavior;
import net.tigereye.chestcavity.registration.CCKeybindings;

/**
 * Client-side ability registrations for 血道 organs.
 */
public final class XueDaoClientAbilities {

    private XueDaoClientAbilities() {
    }

    public static void onClientSetup(FMLClientSetupEvent event) {
        if (!CCKeybindings.ATTACK_ABILITY_LIST.contains(XueFeiguOrganBehavior.ABILITY_ID)) {
            CCKeybindings.ATTACK_ABILITY_LIST.add(XueFeiguOrganBehavior.ABILITY_ID);
        }
    }
}
