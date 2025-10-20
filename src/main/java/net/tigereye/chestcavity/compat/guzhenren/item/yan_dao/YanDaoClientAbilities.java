package net.tigereye.chestcavity.compat.guzhenren.item.yan_dao;

import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.tigereye.chestcavity.registration.CCKeybindings;
import net.tigereye.chestcavity.ChestCavity;
import net.minecraft.resources.ResourceLocation;

/**
 * Ensures Yan Dao attack abilities are exposed to the shared attack hotkey on the client.
 */
public final class YanDaoClientAbilities {

    private YanDaoClientAbilities() {
    }

    public static void onClientSetup(FMLClientSetupEvent event) {
        // Avoid class-loading behaviour classes on client setup; use literal id
        ResourceLocation huoYiGuId = ResourceLocation.fromNamespaceAndPath("guzhenren", "huo_gu");
        if (!CCKeybindings.ATTACK_ABILITY_LIST.contains(huoYiGuId)) {
            CCKeybindings.ATTACK_ABILITY_LIST.add(huoYiGuId);
        }
        ResourceLocation huoLongBreath = ResourceLocation.fromNamespaceAndPath("guzhenren", "huo_long_gu_breath");
        if (!CCKeybindings.ATTACK_ABILITY_LIST.contains(huoLongBreath)) {
            CCKeybindings.ATTACK_ABILITY_LIST.add(huoLongBreath);
        }
        ResourceLocation huoLongHover = ResourceLocation.fromNamespaceAndPath("guzhenren", "huo_long_gu_hover");
        if (!CCKeybindings.ATTACK_ABILITY_LIST.contains(huoLongHover)) {
            CCKeybindings.ATTACK_ABILITY_LIST.add(huoLongHover);
        }
        ResourceLocation huoLongDive = ResourceLocation.fromNamespaceAndPath("guzhenren", "huo_long_gu_dive");
        if (!CCKeybindings.ATTACK_ABILITY_LIST.contains(huoLongDive)) {
            CCKeybindings.ATTACK_ABILITY_LIST.add(huoLongDive);
        }
    }
}
