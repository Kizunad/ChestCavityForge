package net.tigereye.chestcavity.compat.guzhenren.item.yun_dao_cloud;

import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.tigereye.chestcavity.compat.guzhenren.item.yun_dao_cloud.behavior.BaiYunGuOrganBehavior;
import net.tigereye.chestcavity.registration.CCKeybindings;

/**
 * Client-side ability wiring for 云道·白云蛊，确保主动技能加入统一按键。
 */
public final class YunDaoCloudClientAbilities {

    private YunDaoCloudClientAbilities() {
    }

    public static void onClientSetup(FMLClientSetupEvent event) {
        if (!CCKeybindings.ATTACK_ABILITY_LIST.contains(BaiYunGuOrganBehavior.ABILITY_ID)) {
            CCKeybindings.ATTACK_ABILITY_LIST.add(BaiYunGuOrganBehavior.ABILITY_ID);
        }
    }
}
