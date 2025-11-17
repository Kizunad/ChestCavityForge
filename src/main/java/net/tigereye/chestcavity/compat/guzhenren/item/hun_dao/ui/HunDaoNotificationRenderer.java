package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.ui;

import com.mojang.logging.LogUtils;
import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;

/**
 * Renders notification messages from server for Hun Dao events.
 *
 * <p>Displays temporary toast-style notifications for events like hun po leak warnings, soul beast
 * activation, gui wu activation, etc. Messages queue and display sequentially with fade-out
 * animations.
 *
 * <p>Phase 5: Basic framework. Full rendering implementation in Phase 6+.
 */
public final class HunDaoNotificationRenderer {

  private static final Logger LOGGER = LogUtils.getLogger();

  private static final int MAX_NOTIFICATIONS = 5;
  private static final int NOTIFICATION_DURATION_TICKS = 60; // 3 seconds
  private static final int FADE_OUT_TICKS = 10;

  private static final Deque<Notification> notifications = new ArrayDeque<>();

  private HunDaoNotificationRenderer() {}

  /**
   * Queues a notification for display.
   *
   * @param message the notification message
   * @param category notification category for styling
   */
  public static void show(Component message, NotificationCategory category) {
    if (message == null || category == null) {
      return;
    }

    Notification notification = new Notification(message, category, NOTIFICATION_DURATION_TICKS);
    notifications.addLast(notification);

    // Limit queue size
    while (notifications.size() > MAX_NOTIFICATIONS) {
      notifications.removeFirst();
    }

    LOGGER.debug(
        "[hun_dao][notifications] Queued notification: {} (category={})",
        message.getString(),
        category);
  }

  /**
   * Renders all active notifications.
   *
   * <p>Called from RenderGuiEvent.Post or similar client rendering event.
   *
   * @param guiGraphics the GUI graphics context
   * @param partialTicks partial tick time for smooth rendering
   */
  public static void render(GuiGraphics guiGraphics, float partialTicks) {
    Minecraft mc = Minecraft.getInstance();
    if (mc.options.hideGui || notifications.isEmpty()) {
      return;
    }

    int screenWidth = mc.getWindow().getGuiScaledWidth();
    int screenHeight = mc.getWindow().getGuiScaledHeight();

    int yOffset = 10;
    for (Notification notification : notifications) {
      renderNotification(guiGraphics, notification, screenWidth, screenHeight, yOffset, partialTicks);
      yOffset += 20; // Stack notifications vertically
    }
  }

  /**
   * Renders a single notification.
   *
   * @param guiGraphics the GUI graphics context
   * @param notification the notification to render
   * @param screenWidth screen width
   * @param screenHeight screen height
   * @param yOffset vertical offset from top of screen
   * @param partialTicks partial tick time
   */
  private static void renderNotification(
      GuiGraphics guiGraphics,
      Notification notification,
      int screenWidth,
      int screenHeight,
      int yOffset,
      float partialTicks) {
    // TODO Phase 6+: Render notification using GuiGraphics
    // Example: Toast-style message in top-right corner with fade-out
    // int x = screenWidth - 200;
    // int y = yOffset;
    // int alpha = calculateAlpha(notification);
    // guiGraphics.fill(...); // Background
    // guiGraphics.drawString(...); // Message text
  }

  /**
   * Calculates alpha value for fade-out animation.
   *
   * @param notification the notification
   * @return alpha value (0-255)
   */
  private static int calculateAlpha(Notification notification) {
    int remainingTicks = notification.remainingTicks;
    if (remainingTicks <= FADE_OUT_TICKS) {
      return (int) (255 * ((float) remainingTicks / FADE_OUT_TICKS));
    }
    return 255;
  }

  /**
   * Ticks all notifications and removes expired ones.
   *
   * <p>Called every client tick.
   */
  public static void tick() {
    notifications.removeIf(notification -> {
      notification.remainingTicks--;
      return notification.remainingTicks <= 0;
    });
  }

  /**
   * Clears all notifications.
   */
  public static void clear() {
    notifications.clear();
  }

  /**
   * Notification data class.
   */
  private static final class Notification {
    final Component message;
    final NotificationCategory category;
    int remainingTicks;

    Notification(Component message, NotificationCategory category, int durationTicks) {
      this.message = message;
      this.category = category;
      this.remainingTicks = durationTicks;
    }
  }

  /**
   * Notification categories for styling.
   */
  public enum NotificationCategory {
    INFO, // General information
    WARNING, // Warnings (e.g., hun po low)
    SUCCESS, // Success messages (e.g., soul beast activated)
    ERROR // Error messages
  }
}
