package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.client.modernui;

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.graphics.Canvas;

/**
 * Interface for Hun Dao Modern UI panel tabs.
 *
 * <p>Phase 7: Tab interface for extensible multi-tab panel architecture.
 */
public interface IHunDaoPanelTab {

  /**
   * Get the unique identifier for this tab.
   *
   * @return the tab ID (e.g., "soul_overview", "reserved_1")
   */
  @NonNull
  String getId();

  /**
   * Get the display title for this tab.
   *
   * @return the tab title (e.g., "Soul Overview", "Reserved")
   */
  @NonNull
  String getTitle();

  /**
   * Render the content of this tab.
   *
   * @param canvas the Modern UI canvas
   * @param mouseX the mouse X position
   * @param mouseY the mouse Y position
   * @param partialTick the partial tick time
   */
  void renderContent(@NonNull Canvas canvas, int mouseX, int mouseY, float partialTick);

  /**
   * Check if this tab is visible in the tab bar.
   *
   * @return true if visible, false otherwise
   */
  default boolean isVisible() {
    return true;
  }

  /**
   * Check if this tab is enabled (clickable).
   *
   * @return true if enabled, false otherwise
   */
  default boolean isEnabled() {
    return true;
  }
}
