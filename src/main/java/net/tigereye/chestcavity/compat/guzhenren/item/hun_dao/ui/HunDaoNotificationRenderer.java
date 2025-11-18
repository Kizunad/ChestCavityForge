package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 占位通知渲染器，Phase 6 UI 需求已被关闭，所有方法均为空实现。
 *
 * <p>保留 API 以便未来重新启用，不再在客户端渲染 toast。
 */
public final class HunDaoNotificationRenderer {

  private static final Logger LOGGER = LoggerFactory.getLogger(HunDaoNotificationRenderer.class);

  private HunDaoNotificationRenderer() {}

  public static void show(Component message, NotificationCategory category) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(
          "[hun_dao][notifications] UI disabled, drop message='{}' category={}",
          message != null ? message.getString() : "<null>",
          category);
    }
  }

  public static void render(GuiGraphics guiGraphics, float partialTicks) {
    // UI removed intentionally
  }

  public static void tick() {
    // UI removed intentionally
  }

  public static void clear() {
    // UI removed intentionally
  }

  public enum NotificationCategory {
    INFO,
    WARNING,
    SUCCESS,
    ERROR
  }
}
