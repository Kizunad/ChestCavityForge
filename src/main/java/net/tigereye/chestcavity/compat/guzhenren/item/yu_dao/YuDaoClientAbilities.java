package net.tigereye.chestcavity.compat.guzhenren.item.yu_dao;

import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.tigereye.chestcavity.compat.guzhenren.item.yu_dao.behavior.YuanLaoGuFifthTierBehavior;
import net.tigereye.chestcavity.registration.CCKeybindings;

/**
 * Registers宇道相关的主动技能到按键列表。
 */
public final class YuDaoClientAbilities {

    private YuDaoClientAbilities() {}

    public static void onClientSetup(FMLClientSetupEvent event) {
        if (!CCKeybindings.ATTACK_ABILITY_LIST.contains(YuanLaoGuFifthTierBehavior.ABILITY_ID)) {
            CCKeybindings.ATTACK_ABILITY_LIST.add(YuanLaoGuFifthTierBehavior.ABILITY_ID);
        }
    }
}
