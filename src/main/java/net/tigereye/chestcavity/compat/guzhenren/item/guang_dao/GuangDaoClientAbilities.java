package net.tigereye.chestcavity.compat.guzhenren.item.guang_dao;

import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.registration.CCKeybindings;

/**
 * Ensures 光道·闪光蛊 主动技能接入通用攻击类快捷键（ATTACK_ABILITIES）。
 */
public final class GuangDaoClientAbilities {

    private GuangDaoClientAbilities() {}

    public static void onClientSetup(FMLClientSetupEvent event) {
        ResourceLocation shanGuangId = ResourceLocation.fromNamespaceAndPath("guzhenren", "shan_guang_gu_flash");
        if (!CCKeybindings.ATTACK_ABILITY_LIST.contains(shanGuangId)) {
            CCKeybindings.ATTACK_ABILITY_LIST.add(shanGuangId);
        }
        ResourceLocation xiaoGuangId = ResourceLocation.fromNamespaceAndPath("guzhenren", "xiao_guang_illusion");
        if (!CCKeybindings.ATTACK_ABILITY_LIST.contains(xiaoGuangId)) {
            CCKeybindings.ATTACK_ABILITY_LIST.add(xiaoGuangId);
        }
    }
}
