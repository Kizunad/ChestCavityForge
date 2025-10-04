package net.tigereye.chestcavity.compat.guzhenren.item.san_zhuan.li_dao;

import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.tigereye.chestcavity.registration.CCKeybindings;

/**
 * Ensures third-turn Li Dao abilities are mapped to the shared attack ability key.
 */
public final class SanZhuanLiDaoClientAbilities {

    private SanZhuanLiDaoClientAbilities() {
    }

    public static void onClientSetup(FMLClientSetupEvent event) {
        if (!CCKeybindings.ATTACK_ABILITY_LIST.contains(ZiLiGengShengGuOrganBehavior.ABILITY_ID)) {
            CCKeybindings.ATTACK_ABILITY_LIST.add(ZiLiGengShengGuOrganBehavior.ABILITY_ID);
        }
    }
}
