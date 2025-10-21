package net.tigereye.chestcavity.compat.guzhenren.item.feng_dao;

import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.tigereye.chestcavity.compat.guzhenren.item.feng_dao.behavior.QingFengLunOrganBehavior;
import net.tigereye.chestcavity.registration.CCKeybindings;

/**
 * Client-side ability registration for清风轮蛊。
 */
public final class FengDaoClientAbilities {

    private FengDaoClientAbilities() {
    }

    public static void onClientSetup(FMLClientSetupEvent event) {
        if (!CCKeybindings.ATTACK_ABILITY_LIST.contains(QingFengLunOrganBehavior.DASH_ABILITY_ID)) {
            CCKeybindings.ATTACK_ABILITY_LIST.add(QingFengLunOrganBehavior.DASH_ABILITY_ID);
        }
        if (!CCKeybindings.ATTACK_ABILITY_LIST.contains(QingFengLunOrganBehavior.WIND_SLASH_ABILITY_ID)) {
            CCKeybindings.ATTACK_ABILITY_LIST.add(QingFengLunOrganBehavior.WIND_SLASH_ABILITY_ID);
        }
        if (!CCKeybindings.ATTACK_ABILITY_LIST.contains(QingFengLunOrganBehavior.WIND_DOMAIN_ABILITY_ID)) {
            CCKeybindings.ATTACK_ABILITY_LIST.add(QingFengLunOrganBehavior.WIND_DOMAIN_ABILITY_ID);
        }
    }
}
