package net.tigereye.chestcavity.compat.guzhenren.item.mu_dao;

import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.tigereye.chestcavity.compat.guzhenren.item.mu_dao.behavior.CaoQunGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.mu_dao.behavior.LiandaoGuOrganBehavior;
import net.tigereye.chestcavity.registration.CCKeybindings;

/** Ensures Mu Dao attack abilities are bound to the shared attack keybinding on the client. */
public final class MuDaoClientAbilities {

  private MuDaoClientAbilities() {}

  public static void onClientSetup(FMLClientSetupEvent event) {
    if (!CCKeybindings.ATTACK_ABILITY_LIST.contains(LiandaoGuOrganBehavior.ABILITY_ID)) {
      CCKeybindings.ATTACK_ABILITY_LIST.add(LiandaoGuOrganBehavior.ABILITY_ID);
    }
    if (!CCKeybindings.ATTACK_ABILITY_LIST.contains(CaoQunGuOrganBehavior.ABILITY_A1_ID)) {
      CCKeybindings.ATTACK_ABILITY_LIST.add(CaoQunGuOrganBehavior.ABILITY_A1_ID);
    }
    if (!CCKeybindings.ATTACK_ABILITY_LIST.contains(CaoQunGuOrganBehavior.ABILITY_A2_ID)) {
      CCKeybindings.ATTACK_ABILITY_LIST.add(CaoQunGuOrganBehavior.ABILITY_A2_ID);
    }
    if (!CCKeybindings.ATTACK_ABILITY_LIST.contains(CaoQunGuOrganBehavior.ABILITY_A3_ID)) {
      CCKeybindings.ATTACK_ABILITY_LIST.add(CaoQunGuOrganBehavior.ABILITY_A3_ID);
    }
    if (!CCKeybindings.ATTACK_ABILITY_LIST.contains(CaoQunGuOrganBehavior.ABILITY_A4_ID)) {
      CCKeybindings.ATTACK_ABILITY_LIST.add(CaoQunGuOrganBehavior.ABILITY_A4_ID);
    }
  }
}
