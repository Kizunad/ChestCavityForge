package net.tigereye.chestcavity.compat.guzhenren.item.bing_xue_dao;

import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.tigereye.chestcavity.compat.guzhenren.item.bing_xue_dao.behavior.BingJiGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.bing_xue_dao.behavior.ShuangXiGuOrganBehavior;
import net.tigereye.chestcavity.registration.CCKeybindings;

/** Client-side ability registration for 冰雪道 organs. */
public final class BingXueDaoClientAbilities {

  private BingXueDaoClientAbilities() {}

  public static void onClientSetup(FMLClientSetupEvent event) {
    if (!CCKeybindings.ATTACK_ABILITY_LIST.contains(BingJiGuOrganBehavior.ABILITY_ID)) {
      CCKeybindings.ATTACK_ABILITY_LIST.add(BingJiGuOrganBehavior.ABILITY_ID);
    }
    if (!CCKeybindings.ATTACK_ABILITY_LIST.contains(ShuangXiGuOrganBehavior.ABILITY_ID)) {
      CCKeybindings.ATTACK_ABILITY_LIST.add(ShuangXiGuOrganBehavior.ABILITY_ID);
    }
  }
}
