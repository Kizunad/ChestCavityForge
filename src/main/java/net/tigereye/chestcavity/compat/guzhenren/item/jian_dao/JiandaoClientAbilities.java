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

    // 剑引蛊（占位主动技能）
    var jianYin = net.minecraft.resources.ResourceLocation.parse("guzhenren:jian_yin_guidance");
    if (!CCKeybindings.ATTACK_ABILITY_LIST.contains(jianYin)) {
      CCKeybindings.ATTACK_ABILITY_LIST.add(jianYin);
    }

    var jianYinUi = net.minecraft.resources.ResourceLocation.parse("guzhenren:jian_yin_command_ui");
    if (!CCKeybindings.ATTACK_ABILITY_LIST.contains(jianYinUi)) {
      CCKeybindings.ATTACK_ABILITY_LIST.add(jianYinUi);
    }

    var shouJianLing = net.minecraft.resources.ResourceLocation.parse("guzhenren:shou_jian_ling");
    if (!CCKeybindings.ATTACK_ABILITY_LIST.contains(shouJianLing)) {
      CCKeybindings.ATTACK_ABILITY_LIST.add(shouJianLing);
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

    // 裂剑蛊：裂刃空隙（仅以字面ID注册，避免类提前加载）
    var lieJian = net.minecraft.resources.ResourceLocation.parse("guzhenren:lie_jian_gu_activate");
    if (!CCKeybindings.ATTACK_ABILITY_LIST.contains(lieJian)) {
      CCKeybindings.ATTACK_ABILITY_LIST.add(lieJian);
    }

    // 剑疗蛊：剑血互济
    var jianLiao = net.minecraft.resources.ResourceLocation.parse("guzhenren:jian_xue_hu_ji");
    if (!CCKeybindings.ATTACK_ABILITY_LIST.contains(jianLiao)) {
      CCKeybindings.ATTACK_ABILITY_LIST.add(jianLiao);
    }

    // 剑脉蛊：剑脉涌流
    var jianmai = net.minecraft.resources.ResourceLocation.parse("guzhenren:jianmai_overdrive");
    if (!CCKeybindings.ATTACK_ABILITY_LIST.contains(jianmai)) {
      CCKeybindings.ATTACK_ABILITY_LIST.add(jianmai);
    }

    // 碎刃蛊：碎刃祭痕
    var suiRen = net.minecraft.resources.ResourceLocation.parse("guzhenren:sui_ren_gu");
    if (!CCKeybindings.ATTACK_ABILITY_LIST.contains(suiRen)) {
      CCKeybindings.ATTACK_ABILITY_LIST.add(suiRen);
    }
  }
}
