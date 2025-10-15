package net.tigereye.chestcavity.compat.guzhenren.item.ren_dao;

import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.tigereye.chestcavity.compat.guzhenren.item.ren_dao.behavior.QingTongSheLiGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.ren_dao.behavior.ChiTieSheLiGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.ren_dao.behavior.BaiYinSheLiGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.ren_dao.behavior.HuangJinSheLiGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.ren_dao.behavior.ZaijinSheLiGuOrganBehavior;
import net.tigereye.chestcavity.registration.CCKeybindings;

/**
 * 注册人道相关的主动技能到按键列表。
 */
public final class RenDaoClientAbilities {

    private RenDaoClientAbilities() {}

    public static void onClientSetup(FMLClientSetupEvent event) {
        if (!CCKeybindings.ATTACK_ABILITY_LIST.contains(QingTongSheLiGuOrganBehavior.ABILITY_ID)) {
            CCKeybindings.ATTACK_ABILITY_LIST.add(QingTongSheLiGuOrganBehavior.ABILITY_ID);
        }
        if (!CCKeybindings.ATTACK_ABILITY_LIST.contains(ChiTieSheLiGuOrganBehavior.ABILITY_ID)) {
            CCKeybindings.ATTACK_ABILITY_LIST.add(ChiTieSheLiGuOrganBehavior.ABILITY_ID);
        }
        if (!CCKeybindings.ATTACK_ABILITY_LIST.contains(BaiYinSheLiGuOrganBehavior.ABILITY_ID)) {
            CCKeybindings.ATTACK_ABILITY_LIST.add(BaiYinSheLiGuOrganBehavior.ABILITY_ID);
        }
        if (!CCKeybindings.ATTACK_ABILITY_LIST.contains(HuangJinSheLiGuOrganBehavior.ABILITY_ID)) {
            CCKeybindings.ATTACK_ABILITY_LIST.add(HuangJinSheLiGuOrganBehavior.ABILITY_ID);
        }
        if (!CCKeybindings.ATTACK_ABILITY_LIST.contains(ZaijinSheLiGuOrganBehavior.ABILITY_ID)) {
            CCKeybindings.ATTACK_ABILITY_LIST.add(ZaijinSheLiGuOrganBehavior.ABILITY_ID);
        }
    }
}
