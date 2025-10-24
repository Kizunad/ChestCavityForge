package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao;

import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.behavior.JianYingGuOrganBehavior;
import net.tigereye.chestcavity.registration.CCKeybindings;

/** Ensures the sword shadow active ability is hooked into the shared attack hotkey. */
public final class JiandaoClientAbilities {

  private JiandaoClientAbilities() {}

  public static void onClientSetup(FMLClientSetupEvent event) {
    if (!CCKeybindings.ATTACK_ABILITY_LIST.contains(JianYingGuOrganBehavior.ABILITY_ID)) {
      CCKeybindings.ATTACK_ABILITY_LIST.add(JianYingGuOrganBehavior.ABILITY_ID);
    }
  }
}
