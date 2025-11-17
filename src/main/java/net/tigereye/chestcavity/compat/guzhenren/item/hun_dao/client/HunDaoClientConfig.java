package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.client;

/**
 * Client-side configuration for Hun Dao HUD and FX rendering.
 *
 * <p>Controls whether HUD elements and notifications are displayed on the client.
 * This is a simple static configuration for Phase 6; can be expanded to ModConfigSpec in future.
 *
 * <p>Phase 6: Basic client-side rendering toggles.
 */
public final class HunDaoClientConfig {

  private HunDaoClientConfig() {}

  /**
   * Whether to render Hun Dao HUD elements (hun po bar, soul beast timer, etc.).
   *
   * <p>Default: true
   */
  public static boolean renderHud = true;

  /**
   * Whether to render Hun Dao notification toasts.
   *
   * <p>Default: true
   */
  public static boolean renderNotifications = true;

  /**
   * Whether to render soul flame stacks on crosshair target.
   *
   * <p>Default: true
   */
  public static boolean renderSoulFlameStacks = true;

  /**
   * Whether to render hun po bar.
   *
   * <p>Default: true
   */
  public static boolean renderHunPoBar = true;

  /**
   * Whether to render soul beast and gui wu timers.
   *
   * <p>Default: true
   */
  public static boolean renderTimers = true;

  /**
   * Checks if HUD rendering is enabled globally.
   *
   * @return true if any HUD element should be rendered
   */
  public static boolean isHudEnabled() {
    return renderHud && (renderHunPoBar || renderTimers || renderSoulFlameStacks);
  }

  /**
   * Checks if notifications should be rendered.
   *
   * @return true if notifications are enabled
   */
  public static boolean areNotificationsEnabled() {
    return renderNotifications;
  }

  // Future expansion: Add keybinding to toggle HUD in-game
  // Future expansion: Add ModConfigSpec integration for persistent config
  // Future expansion: Add config GUI via Cloth Config or similar
}
