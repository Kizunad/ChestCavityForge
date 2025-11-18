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
   * Render the content of this tab (Phase 7.1: deprecated in favor of getFormattedContent).
   *
   * @param canvas the Modern UI canvas
   * @param mouseX the mouse X position
   * @param mouseY the mouse Y position
   * @param partialTick the partial tick time
   */
  default void renderContent(@NonNull Canvas canvas, int mouseX, int mouseY, float partialTick) {
    // Default no-op implementation for Phase 7.1 TextView-based rendering
  }

  /**
   * Get formatted text content for this tab.
   *
   * <p>Phase 7.1: Returns the tab's content as formatted text for display in TextView.
   *
   * @return formatted text content
   */
  @NonNull
  String getFormattedContent();

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
