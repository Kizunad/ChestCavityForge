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

    // Position: above hotbar, center-aligned
    int barX = screenWidth / 2 - 91;
    int barY = screenHeight - 32 - 10;
    int barWidth = 182;
    int barHeight = 5;
    double percentage = Math.max(0.0, Math.min(1.0, hunPoCurrent / hunPoMax));

    // Background (dark gray)
    guiGraphics.fill(barX, barY, barX + barWidth, barY + barHeight, 0xFF2A2A2A);

    // Foreground bar (gradient purple-blue for soul energy)
    int filledWidth = (int) (barWidth * percentage);
    if (filledWidth > 0) {
      // Use purple color for hun po (soul energy): 0xFFAA00FF
      guiGraphics.fill(barX, barY, barX + filledWidth, barY + barHeight, 0xFFAA00FF);
    }

    // Border (light gray)
    guiGraphics.fill(barX - 1, barY - 1, barX + barWidth + 1, barY, 0xFFAAAAAA); // Top
    guiGraphics.fill(barX - 1, barY + barHeight, barX + barWidth + 1, barY + barHeight + 1, 0xFFAAAAAA); // Bottom
    guiGraphics.fill(barX - 1, barY, barX, barY + barHeight, 0xFFAAAAAA); // Left
    guiGraphics.fill(barX + barWidth, barY, barX + barWidth + 1, barY + barHeight, 0xFFAAAAAA); // Right

    // Text: "Hun Po: 50 / 100" centered above bar
    String text = String.format("Hun Po: %.0f / %.0f", hunPoCurrent, hunPoMax);
    int textX = screenWidth / 2 - Minecraft.getInstance().font.width(text) / 2;
    int textY = barY - 10;
    guiGraphics.drawString(
        Minecraft.getInstance().font,
        text,
        textX,
        textY,
        0xFFFFFFFF, // White text
        true); // Drop shadow
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

    // Position: top-right corner with padding
    String text = String.format("Soul Beast: %ds", durationSeconds);
    int textX = screenWidth - Minecraft.getInstance().font.width(text) - 10;
    int textY = 10;

    // Background box (semi-transparent dark)
    int boxPadding = 3;
    int boxX1 = textX - boxPadding;
    int boxY1 = textY - boxPadding;
    int boxX2 = screenWidth - 10 + boxPadding;
    int boxY2 = textY + 8 + boxPadding;
    guiGraphics.fill(boxX1, boxY1, boxX2, boxY2, 0xAA000000);

    // Text with red color to indicate transformation
    guiGraphics.drawString(
        Minecraft.getInstance().font,
        text,
        textX,
        textY,
        0xFFFF5555, // Red color for soul beast
        true); // Drop shadow
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
    // Get the entity the player is looking at
    Minecraft mc = Minecraft.getInstance();
    if (mc.crosshairPickEntity == null) {
      return;
    }

    UUID targetId = mc.crosshairPickEntity.getUUID();
    int stacks = state.getSoulFlameStacks(targetId);

    if (stacks <= 0) {
      return;
    }

    // Position: slightly below crosshair center
    String text = String.format("Soul Flame: %d", stacks);
    int textX = screenWidth / 2 - Minecraft.getInstance().font.width(text) / 2;
    int textY = screenHeight / 2 + 15;

    // Background box (semi-transparent dark)
    int boxPadding = 2;
    int boxX1 = textX - boxPadding;
    int boxY1 = textY - boxPadding;
    int boxX2 = textX + mc.font.width(text) + boxPadding;
    int boxY2 = textY + 8 + boxPadding;
    guiGraphics.fill(boxX1, boxY1, boxX2, boxY2, 0xAA000000);

    // Text with orange/fire color for soul flame
    guiGraphics.drawString(
        Minecraft.getInstance().font,
        text,
        textX,
        textY,
        0xFFFF8800, // Orange color for flame
        true); // Drop shadow
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

    // Position: top-right corner, below soul beast timer if active
    int yOffset = 10;
    if (state.isSoulBeastActive(player.getUUID())) {
      yOffset = 30; // Stack below soul beast timer
    }

    String text = String.format("Gui Wu: %ds", durationSeconds);
    int textX = screenWidth - Minecraft.getInstance().font.width(text) - 10;
    int textY = yOffset;

    // Background box (semi-transparent dark)
    int boxPadding = 3;
    int boxX1 = textX - boxPadding;
    int boxY1 = textY - boxPadding;
    int boxX2 = screenWidth - 10 + boxPadding;
    int boxY2 = textY + 8 + boxPadding;
    guiGraphics.fill(boxX1, boxY1, boxX2, boxY2, 0xAA000000);

    // Text with dark green color for gui wu (ghost mist)
    guiGraphics.drawString(
        Minecraft.getInstance().font,
        text,
        textX,
        textY,
        0xFF55FF55, // Green color for ghost mist
        true); // Drop shadow
  }

}
