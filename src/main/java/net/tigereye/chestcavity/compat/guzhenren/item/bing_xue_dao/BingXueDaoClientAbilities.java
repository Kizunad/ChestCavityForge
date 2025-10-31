package net.tigereye.chestcavity.compat.guzhenren.item.bing_xue_dao;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.tigereye.chestcavity.registration.CCKeybindings;

/** Client-side ability registration for 冰雪道 organs. */
public final class BingXueDaoClientAbilities {

  private BingXueDaoClientAbilities() {}

  public static void onClientSetup(FMLClientSetupEvent event) {
    ResourceLocation bingJiGuAbility = ResourceLocation.fromNamespaceAndPath("guzhenren", "bing_ji_gu_iceburst");
    if (!CCKeybindings.ATTACK_ABILITY_LIST.contains(bingJiGuAbility)) {
      CCKeybindings.ATTACK_ABILITY_LIST.add(bingJiGuAbility);
    }

    ResourceLocation shuangXiGuAbility = ResourceLocation.fromNamespaceAndPath("guzhenren", "shuang_xi_gu_frost_breath");
    if (!CCKeybindings.ATTACK_ABILITY_LIST.contains(shuangXiGuAbility)) {
      CCKeybindings.ATTACK_ABILITY_LIST.add(shuangXiGuAbility);
    }
  }
}
