package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.client.modernui.tabs;

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.graphics.Paint;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.client.HunDaoClientState;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.client.SoulRarity;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.client.SoulState;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.client.modernui.IHunDaoPanelTab;

/**
 * Soul Overview Tab implementation.
 *
 * <p>Phase 7: Displays soul state, level, rarity, max hun po, and attributes.
 */
@OnlyIn(Dist.CLIENT)
public class SoulOverviewTab implements IHunDaoPanelTab {

  private static final int COLOR_WHITE = 0xFFFFFFFF;
  private static final int COLOR_GRAY = 0xFF9FA7B3;
  private static final int COLOR_YELLOW = 0xFFE6B422;
  private static final int COLOR_RED = 0xFFFF5555;

  @NonNull
  @Override
  public String getId() {
    return "soul_overview";
  }

  @NonNull
  @Override
  public String getTitle() {
    return "Soul Overview";
  }

  @Override
  public void renderContent(@NonNull Canvas canvas, int mouseX, int mouseY, float partialTick) {
    Minecraft minecraft = Minecraft.getInstance();
    Player player = minecraft.player;

    if (player == null) {
      renderText(canvas, "No player data available", 10, 10, COLOR_RED);
      return;
    }

    HunDaoClientState state = HunDaoClientState.instance();
    var playerId = player.getUUID();
    boolean isActive = state.isSoulSystemActive(playerId);

    float y = 10;
    float lineHeight = 20;

    // Title
    y = renderText(canvas, "Soul Overview", 10, y, COLOR_WHITE);
    y += lineHeight;

    // Check if soul system is active - show warning if not
    if (!isActive) {
      y = renderText(canvas, "━━━━━━━━━━━━━━━━━━━━━━━━━━", 10, y, COLOR_YELLOW);
      y = renderText(canvas, "⚠ Soul System is Inactive", 10, y, COLOR_YELLOW);
      y = renderText(canvas, "━━━━━━━━━━━━━━━━━━━━━━━━━━", 10, y, COLOR_YELLOW);
      y += lineHeight / 2;
    }

    // Soul State
    String stateText =
        "Soul State: " + formatSoulState(state.getSoulState(playerId).orElse(null));
    y = renderText(canvas, stateText, 10, y, COLOR_GRAY);

    // Soul Level
    String levelText = "Soul Level: " + formatSoulLevel(state.getSoulLevel(playerId));
    y = renderText(canvas, levelText, 10, y, COLOR_GRAY);

    // Soul Rarity
    String rarityText =
        "Soul Rarity: " + formatSoulRarity(state.getSoulRarity(playerId).orElse(null));
    y = renderText(canvas, rarityText, 10, y, COLOR_GRAY);

    // Soul Max (Hun Po Max)
    String maxText = "Soul Max: " + formatSoulMax((int) state.getHunPoMax(playerId));
    y = renderText(canvas, maxText, 10, y, COLOR_GRAY);

    y += lineHeight / 2;

    // Attributes section
    y = renderText(canvas, "Attributes:", 10, y, COLOR_WHITE);
    Map<String, Object> attributes = state.getSoulAttributes(playerId);

    if (attributes.isEmpty()) {
      y = renderText(canvas, "  - Attribute 1: --", 10, y, COLOR_GRAY);
      y = renderText(canvas, "  - Attribute 2: --", 10, y, COLOR_GRAY);
      y = renderText(canvas, "  - Attribute 3: --", 10, y, COLOR_GRAY);
    } else {
      int count = 0;
      for (Map.Entry<String, Object> entry : attributes.entrySet()) {
        count++;
        String attrText = "  - " + entry.getKey() + ": " + entry.getValue();
        y = renderText(canvas, attrText, 10, y, COLOR_GRAY);
      }
      // Fill in placeholders if less than 3 attributes
      while (count < 3) {
        count++;
        y = renderText(canvas, "  - Attribute " + count + ": --", 10, y, COLOR_GRAY);
      }
    }
  }

  /**
   * Helper method to render text on canvas and return the next Y position.
   *
   * @param canvas the canvas to draw on
   * @param text the text to render
   * @param x the x position
   * @param y the y position
   * @param color the text color
   * @return the next Y position (y + line height)
   */
  private float renderText(Canvas canvas, String text, float x, float y, int color) {
    Paint paint = Paint.get();
    paint.setColor(color);
    canvas.drawText(text, x, y, paint);
    return y + 20; // Return next line position
  }

  /**
   * Format soul data as text for display in a TextView.
   *
   * <p>This is a helper method used by the fragment to populate content.
   *
   * @return formatted text for soul overview
   */
  public String formatSoulData() {
    Minecraft minecraft = Minecraft.getInstance();
    Player player = minecraft.player;

    if (player == null) {
      return "No player data available";
    }

    HunDaoClientState state = HunDaoClientState.instance();
    var playerId = player.getUUID();

    StringBuilder sb = new StringBuilder();
    sb.append("Soul Overview\n\n");

    // Check if soul system is active
    boolean isActive = state.isSoulSystemActive(playerId);
    if (!isActive) {
      sb.append("Soul System is Inactive\n");
      sb.append("→ Display fallback placeholder\n");
      sb.append("→ No crash, no missing data\n\n");
    }

    // Soul State
    sb.append("Soul State: ");
    sb.append(formatSoulState(state.getSoulState(playerId).orElse(null)));
    sb.append("\n");

    // Soul Level
    sb.append("Soul Level: ");
    sb.append(formatSoulLevel(state.getSoulLevel(playerId)));
    sb.append("\n");

    // Soul Rarity
    sb.append("Soul Rarity: ");
    sb.append(formatSoulRarity(state.getSoulRarity(playerId).orElse(null)));
    sb.append("\n");

    // Soul Max (Hun Po Max)
    sb.append("Soul Max: ");
    sb.append(formatSoulMax((int) state.getHunPoMax(playerId)));
    sb.append("\n\n");

    // Attributes
    sb.append("Attributes:\n");
    Map<String, Object> attributes = state.getSoulAttributes(playerId);
    if (attributes.isEmpty()) {
      sb.append("  - Attribute 1: --\n");
      sb.append("  - Attribute 2: --\n");
      sb.append("  - Attribute 3: --\n");
    } else {
      int count = 0;
      for (Map.Entry<String, Object> entry : attributes.entrySet()) {
        count++;
        sb.append("  - ");
        sb.append(entry.getKey());
        sb.append(": ");
        sb.append(entry.getValue());
        sb.append("\n");
      }
      // Fill in placeholders if less than 3 attributes
      while (count < 3) {
        count++;
        sb.append("  - Attribute ");
        sb.append(count);
        sb.append(": --\n");
      }
    }

    return sb.toString();
  }

  private String formatSoulState(SoulState state) {
    if (state == null) {
      return "Unknown";
    }
    return state.getDisplayName();
  }

  private String formatSoulLevel(int level) {
    return level > 0 ? String.valueOf(level) : "--";
  }

  private String formatSoulRarity(SoulRarity rarity) {
    if (rarity == null) {
      return "Unidentified";
    }
    return rarity.getDisplayName();
  }

  private String formatSoulMax(int max) {
    return max > 0 ? String.valueOf(max) : "--";
  }

  @Override
  public boolean isVisible() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return true;
  }
}
