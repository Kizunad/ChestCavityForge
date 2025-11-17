package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao;

import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.client.HunDaoClientRegistries;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.fx.HunDaoFxInit;

/**
 * Client-side ability registration entry point for Hun Dao organs.
 *
 * <p>Phase 5: Refactored to delegate to HunDaoClientRegistries for cleaner separation of concerns.
 * This class serves as the top-level entry point called from mod initialization, while actual
 * registration logic lives in the client/ package.
 */
public final class HunDaoClientAbilities {

  private HunDaoClientAbilities() {}

  /**
   * Called during FMLClientSetupEvent.
   *
   * <p>Initializes client-side registries and FX system.
   *
   * @param event the client setup event
   */
  public static void onClientSetup(FMLClientSetupEvent event) {
    // Initialize FX templates (client needs them for rendering)
    HunDaoFxInit.init();

    // Register client-side abilities and hooks
    HunDaoClientRegistries.init();
  }
}
