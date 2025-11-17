package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.client;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.ui.HunDaoNotificationRenderer;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.ui.HunDaoSoulHud;
import org.slf4j.Logger;

/**
 * Client-side event handlers for Hun Dao.
 *
 * <p>Listens to NeoForge client events (ClientTickEvent, RenderGuiEvent, etc.) and updates client
 * state, triggers FX, or renders HUD elements.
 *
 * <p>Phase 5: Client event handling decoupled from abilities registration.
 */
public final class HunDaoClientEvents {

  private static final Logger LOGGER = LogUtils.getLogger();

  /**
   * Called every client tick.
   *
   * <p>Updates HunDaoClientState timers and triggers periodic FX if needed.
   *
   * @param event the client tick event
   */
  @SubscribeEvent
  public static void onClientTick(ClientTickEvent.Post event) {
    Minecraft mc = Minecraft.getInstance();
    if (mc.level == null || mc.isPaused()) {
      return;
    }

    // Tick client state (decay timers)
    HunDaoClientState.instance().tick();

    // Tick notifications (remove expired)
    HunDaoNotificationRenderer.tick();

    // Optional: Trigger periodic client-side FX based on state
    // Example: Play ambient soul beast particles if soul beast is active
    LocalPlayer player = mc.player;
    if (player != null && HunDaoClientState.instance().isSoulBeastActive(player.getUUID())) {
      // Ambient FX handled by FxEngine, no client-side logic needed here
      // This is a placeholder for future client-only FX logic
    }
  }

  /**
   * Called when the client unloads a level.
   *
   * <p>Clears all cached client state to prevent stale data.
   *
   * @param event the level unload event
   */
  @SubscribeEvent
  public static void onLevelUnload(LevelEvent.Unload event) {
    if (event.getLevel().isClientSide()) {
      LOGGER.debug("[hun_dao][client_events] Clearing client state on level unload");
      HunDaoClientState.instance().clearAll();
      HunDaoNotificationRenderer.clear();
    }
  }

  /**
   * Called after GUI rendering.
   *
   * <p>Renders Hun Dao HUD overlays and notifications.
   *
   * @param event the render GUI event
   */
  @SubscribeEvent
  public static void onRenderGui(RenderGuiEvent.Post event) {
    Minecraft mc = Minecraft.getInstance();
    if (mc.level == null || mc.player == null) {
      return;
    }

    // Extract partial tick from DeltaTracker
    float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(true);

    // Render HUD (hun po bar, soul beast timer, etc.) if enabled
    if (HunDaoClientConfig.isHudEnabled()) {
      HunDaoSoulHud.render(event.getGuiGraphics(), partialTick);
    }

    // Render notifications (toast messages) if enabled
    if (HunDaoClientConfig.areNotificationsEnabled()) {
      HunDaoNotificationRenderer.render(event.getGuiGraphics(), partialTick);
    }
  }
}
