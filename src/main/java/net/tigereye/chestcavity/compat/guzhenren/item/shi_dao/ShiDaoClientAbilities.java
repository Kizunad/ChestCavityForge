package net.tigereye.chestcavity.compat.guzhenren.item.shi_dao;

import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.tigereye.chestcavity.compat.guzhenren.item.shi_dao.behavior.JiuChongOrganBehavior;
import net.tigereye.chestcavity.registration.CCKeybindings;

/**
 * Client entry point for Shi Dao abilities. Ensures the shared attack hotkey exposes the Jiu Chong
 * breath so players can trigger it manually.
 */
public final class ShiDaoClientAbilities {

  private ShiDaoClientAbilities() {}

  public static void onClientSetup(FMLClientSetupEvent event) {
    if (!CCKeybindings.ATTACK_ABILITY_LIST.contains(JiuChongOrganBehavior.ABILITY_ID)) {
      CCKeybindings.ATTACK_ABILITY_LIST.add(JiuChongOrganBehavior.ABILITY_ID);
    }
  }
}
