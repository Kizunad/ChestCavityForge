package net.tigereye.chestcavity.compat.guzhenren.item.shi_dao;

import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Client entry point for Shi Dao abilities. No manual keybinding wiring is required now that the
 * breath activates automatically server-side.
 */
public final class ShiDaoClientAbilities {

    private ShiDaoClientAbilities() {
    }

    public static void onClientSetup(FMLClientSetupEvent event) {
        // No client-side keybinding registration is required because the Shi Dao breath now
        // triggers automatically when its server-side conditions are met.
    }
}
