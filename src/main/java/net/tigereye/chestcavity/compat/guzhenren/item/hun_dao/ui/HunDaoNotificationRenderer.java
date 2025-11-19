package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Placeholder notification renderer.
 *
 * <p>Phase 6 UI requirements have been closed, so all methods are empty implementations. The API
 * is preserved for future re-enabling, but no toasts are currently rendered on the client.
 */
public final class HunDaoNotificationRenderer {

  private static final Logger LOGGER = LoggerFactory.getLogger(HunDaoNotificationRenderer.class);

  private HunDaoNotificationRenderer() {}

  /**
   * Displays a notification.
   *
   * @param message The message to be displayed in the notification.
   * @param category The category of the notification, which determines its appearance and behavior.
   */
  public static void show(Component message, NotificationCategory category) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(
          "[hun_dao][notifications] UI disabled, drop message='{}' category={}",
          message != null ? message.getString() : "<null>",
          category);
    }
  }

  /**
   * Renders the notification on the screen. This method is currently a no-op since the UI is
   * disabled.
   *
   * @param guiGraphics The GuiGraphics context for rendering.
   * @param partialTicks The fraction of a tick that has passed since the last tick.
   */
  public static void render(GuiGraphics guiGraphics, float partialTicks) {
    // UI removed intentionally
  }

  /**
   * Ticks the notification logic. This method is currently a no-op since the UI is disabled.
   */
  public static void tick() {
    // UI removed intentionally
  }

  /**
   * Clears any active notifications. This method is currently a no-op since the UI is disabled.
   */
  public static void clear() {
    // UI removed intentionally
  }

  /** Defines the category of a notification, which can be used to style it differently. */
  public enum NotificationCategory {
    /** For informational messages. */
    INFO,
    /** For warnings. */
    WARNING,
    SUCCESS,
    ERROR
  }
}
