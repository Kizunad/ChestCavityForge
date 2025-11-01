package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao;

import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.tigereye.chestcavity.registration.CCKeybindings;

/** Ensures the sword shadow active ability is hooked into the shared attack hotkey. */
public final class JiandaoClientAbilities {

  private JiandaoClientAbilities() {}

  public static void onClientSetup(FMLClientSetupEvent event) {
    // 避免行为类过早 classloading，仅以字面 ID 注册
    var abilityId = net.minecraft.resources.ResourceLocation.parse("guzhenren:jian_ying_fenshen");
    if (!CCKeybindings.ATTACK_ABILITY_LIST.contains(abilityId)) {
      CCKeybindings.ATTACK_ABILITY_LIST.add(abilityId);
    }

    // 心定冥想（剑心蛊）
    var jianXin = net.minecraft.resources.ResourceLocation.parse("guzhenren:jian_xin_mingxiang");
    if (!CCKeybindings.ATTACK_ABILITY_LIST.contains(jianXin)) {
      CCKeybindings.ATTACK_ABILITY_LIST.add(jianXin);
    }

    // 剑域蛊（调域 TUI）
    var jianYu = net.minecraft.resources.ResourceLocation.parse("guzhenren:jian_yu_gu_adjust");
    if (!CCKeybindings.ATTACK_ABILITY_LIST.contains(jianYu)) {
      CCKeybindings.ATTACK_ABILITY_LIST.add(jianYu);
    }
  }
}
