package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.client;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.behavior.active.GuiQiGuOrganBehavior;
import net.tigereye.chestcavity.registration.CCKeybindings;
import org.slf4j.Logger;

/**
 * Client-side registries for Hun Dao abilities and features.
 *
 * <p>Registers Hun Dao abilities with ChestCavity's keybinding system and client-side FX hooks.
 * Called during FMLClientSetupEvent.
 *
 * <p>Phase 5: Refactored from HunDaoClientAbilities to handle only registration logic.
 */
public final class HunDaoClientRegistries {

  private static final Logger LOGGER = LogUtils.getLogger();
  private static boolean initialized = false;

  private HunDaoClientRegistries() {}

  /**
   * Registers all Hun Dao client-side features.
   *
   * <p>Safe to call multiple times - subsequent calls are ignored.
   */
  public static void init() {
    if (initialized) {
      LOGGER.debug("[hun_dao][client_registries] Already initialized, skipping");
      return;
    }

    LOGGER.info("[hun_dao][client_registries] Registering client-side abilities...");

    registerAbilities();
    registerFxHooks();

    initialized = true;
    LOGGER.info("[hun_dao][client_registries] Client-side registration complete");
  }

  /** Registers Hun Dao abilities with ChestCavity's keybinding system. */
  private static void registerAbilities() {
    // Register Gui Qi Gu ability
    if (!CCKeybindings.ATTACK_ABILITY_LIST.contains(GuiQiGuOrganBehavior.ABILITY_ID)) {
      CCKeybindings.ATTACK_ABILITY_LIST.add(GuiQiGuOrganBehavior.ABILITY_ID);
      LOGGER.debug(
          "[hun_dao][client_registries] Registered ability: {}", GuiQiGuOrganBehavior.ABILITY_ID);
    }

    // Register Hun Shou Hua synergy ability
    ResourceLocation hunShouHuaAbility =
        ResourceLocation.fromNamespaceAndPath("guzhenren", "synergy/hun_shou_hua");
    if (!CCKeybindings.ATTACK_ABILITY_LIST.contains(hunShouHuaAbility)) {
      CCKeybindings.ATTACK_ABILITY_LIST.add(hunShouHuaAbility);
      LOGGER.debug("[hun_dao][client_registries] Registered ability: {}", hunShouHuaAbility);
    }
  }

  /**
   * Registers FX hooks for client-side rendering.
   *
   * <p>Currently a placeholder - FX rendering is handled by FxEngine. Future phases may add custom
   * particle renderers here.
   */
  private static void registerFxHooks() {
    // Placeholder for future FX hook registration
    // Example: register custom particle renderers, render layers, etc.
    LOGGER.debug("[hun_dao][client_registries] FX hooks registration (placeholder)");
  }

  /**
   * Checks if client-side registries have been initialized.
   *
   * @return true if initialized, false otherwise
   */
  public static boolean isInitialized() {
    return initialized;
  }
}
