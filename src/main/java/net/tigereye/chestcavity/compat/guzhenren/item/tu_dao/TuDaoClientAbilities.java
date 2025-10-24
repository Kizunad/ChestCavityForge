package net.tigereye.chestcavity.compat.guzhenren.item.tu_dao;

import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.tigereye.chestcavity.compat.guzhenren.item.tu_dao.behavior.TuQiangGuOrganBehavior;
import net.tigereye.chestcavity.registration.CCKeybindings;

/** Ensures Tu Dao's active ability participates in the shared attack hotkey. */
public final class TuDaoClientAbilities {

  private TuDaoClientAbilities() {}

  public static void onClientSetup(FMLClientSetupEvent event) {
    if (!CCKeybindings.ATTACK_ABILITY_LIST.contains(TuQiangGuOrganBehavior.ABILITY_ID)) {
      CCKeybindings.ATTACK_ABILITY_LIST.add(TuQiangGuOrganBehavior.ABILITY_ID);
    }
  }
}
