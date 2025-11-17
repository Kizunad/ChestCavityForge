package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.client.HunDaoClientState;

/**
 * HUD overlay for Hun Dao status display.
 *
 * <p>Renders hun po bar, soul beast timer, soul flame stacks, and gui wu indicator on the client
 * screen. Queries HunDaoClientState for current values.
 *
 * <p>Phase 5: Basic framework established. Full rendering implementation in Phase 6+.
 */
public final class HunDaoSoulHud {

  private HunDaoSoulHud() {}

  /**
   * Renders the Hun Dao HUD overlay.
   *
   * <p>Called from RenderGuiEvent.Post or similar client rendering event.
   *
   * @param guiGraphics the GUI graphics context
   * @param partialTicks partial tick time for smooth rendering
   */
  public static void render(GuiGraphics guiGraphics, float partialTicks) {
    Minecraft mc = Minecraft.getInstance();
    LocalPlayer player = mc.player;

    if (player == null || mc.options.hideGui) {
      return;
    }

    HunDaoClientState state = HunDaoClientState.instance();
    int screenWidth = mc.getWindow().getGuiScaledWidth();
    int screenHeight = mc.getWindow().getGuiScaledHeight();

    // Render hun po bar if player has hun po data
    renderHunPoBar(guiGraphics, state, player, screenWidth, screenHeight);

    // Render soul beast timer if active
    renderSoulBeastTimer(guiGraphics, state, player, screenWidth, screenHeight);

    // Render soul flame stacks if target has stacks
    renderSoulFlameStacks(guiGraphics, state, player, screenWidth, screenHeight);

    // Render gui wu indicator if active
    renderGuiWuIndicator(guiGraphics, state, player, screenWidth, screenHeight);
  }

  /**
   * Renders the hun po resource bar.
   *
   * @param guiGraphics the GUI graphics context
   * @param state client state
   * @param player local player
   * @param screenWidth screen width
   * @param screenHeight screen height
   */
  private static void renderHunPoBar(
      GuiGraphics guiGraphics,
      HunDaoClientState state,
      LocalPlayer player,
      int screenWidth,
      int screenHeight) {
    double hunPoCurrent = state.getHunPoCurrent(player.getUUID());
    double hunPoMax = state.getHunPoMax(player.getUUID());

    if (hunPoMax <= 0) {
      return; // Player doesn't have hun po system active
    }

    // TODO Phase 6+: Render hun po bar using GuiGraphics
    // Example position: above hotbar, center-aligned
    // int barX = screenWidth / 2 - 91;
    // int barY = screenHeight - 32 - 10;
    // int barWidth = 182;
    // int barHeight = 5;
    // double percentage = hunPoCurrent / hunPoMax;
    // guiGraphics.fill(...);
  }

  /**
   * Renders the soul beast timer indicator.
   *
   * @param guiGraphics the GUI graphics context
   * @param state client state
   * @param player local player
   * @param screenWidth screen width
   * @param screenHeight screen height
   */
  private static void renderSoulBeastTimer(
      GuiGraphics guiGraphics,
      HunDaoClientState state,
      LocalPlayer player,
      int screenWidth,
      int screenHeight) {
    if (!state.isSoulBeastActive(player.getUUID())) {
      return;
    }

    int durationTicks = state.getSoulBeastDuration(player.getUUID());
    int durationSeconds = durationTicks / 20;

    // TODO Phase 6+: Render soul beast timer using GuiGraphics
    // Example: Display "Soul Beast: 12s" in top-right corner
    // guiGraphics.drawString(...);
  }

  /**
   * Renders soul flame stack indicator.
   *
   * @param guiGraphics the GUI graphics context
   * @param state client state
   * @param player local player
   * @param screenWidth screen width
   * @param screenHeight screen height
   */
  private static void renderSoulFlameStacks(
      GuiGraphics guiGraphics,
      HunDaoClientState state,
      LocalPlayer player,
      int screenWidth,
      int screenHeight) {
    // TODO Phase 6+: Render soul flame stacks on crosshair target
    // Requires target entity tracking (not implemented in Phase 5)
    // Example: Display "Soul Flame: 3" near crosshair if targeting entity with stacks
  }

  /**
   * Renders gui wu active indicator.
   *
   * @param guiGraphics the GUI graphics context
   * @param state client state
   * @param player local player
   * @param screenWidth screen width
   * @param screenHeight screen height
   */
  private static void renderGuiWuIndicator(
      GuiGraphics guiGraphics,
      HunDaoClientState state,
      LocalPlayer player,
      int screenWidth,
      int screenHeight) {
    if (!state.isGuiWuActive(player.getUUID())) {
      return;
    }

    int durationTicks = state.getGuiWuDuration(player.getUUID());
    int durationSeconds = durationTicks / 20;

    // TODO Phase 6+: Render gui wu indicator using GuiGraphics
    // Example: Display "Gui Wu: 5s" below soul beast timer
    // guiGraphics.drawString(...);
  }

  /**
   * Registers the HUD overlay with NeoForge rendering system.
   *
   * <p>Called during client initialization.
   */
  public static void register() {
    // TODO Phase 6+: Register with RenderGuiEvent.Post or RegisterGuiOverlaysEvent
    // Example:
    // NeoForge.EVENT_BUS.addListener(HunDaoSoulHud::onRenderGui);
  }
}
