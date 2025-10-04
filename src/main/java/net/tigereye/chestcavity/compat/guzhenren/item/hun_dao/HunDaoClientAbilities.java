package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao;

import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.behavior.GuiQiGuOrganBehavior;
import net.tigereye.chestcavity.registration.CCKeybindings;

/**
 * Client-side ability registration for Hun Dao organs.
 */
public final class HunDaoClientAbilities {

    private HunDaoClientAbilities() {}

    public static void onClientSetup(FMLClientSetupEvent event) {
        if (!CCKeybindings.ATTACK_ABILITY_LIST.contains(GuiQiGuOrganBehavior.ABILITY_ID)) {
            CCKeybindings.ATTACK_ABILITY_LIST.add(GuiQiGuOrganBehavior.ABILITY_ID);
        }
    }
}
