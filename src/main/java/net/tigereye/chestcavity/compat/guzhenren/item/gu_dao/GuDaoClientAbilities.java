package net.tigereye.chestcavity.compat.guzhenren.item.gu_dao;

import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.tigereye.chestcavity.compat.guzhenren.item.gu_dao.behavior.LeGuDunGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.gu_dao.behavior.LuoXuanGuQiangguOrganBehavior;
import net.tigereye.chestcavity.registration.CCKeybindings;

/**
 * Ensures Guzhenren attack abilities are hooked into the shared attack hotkey on the client.
 */
public final class GuDaoClientAbilities {
    private GuDaoClientAbilities() {
    }

    public static void onClientSetup(FMLClientSetupEvent event) {
        if (!CCKeybindings.ATTACK_ABILITY_LIST.contains(LuoXuanGuQiangguOrganBehavior.ABILITY_ID)) {
            CCKeybindings.ATTACK_ABILITY_LIST.add(LuoXuanGuQiangguOrganBehavior.ABILITY_ID);
        }
        if (!CCKeybindings.ATTACK_ABILITY_LIST.contains(LeGuDunGuOrganBehavior.ABILITY_ID)) {
            CCKeybindings.ATTACK_ABILITY_LIST.add(LeGuDunGuOrganBehavior.ABILITY_ID);
        }
    }
}
