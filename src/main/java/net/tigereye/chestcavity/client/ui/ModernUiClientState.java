package net.tigereye.chestcavity.client.ui;

/** Client-only toggles controlled by /testmodernUI panel. */
public final class ModernUiClientState {
  private static volatile boolean showToasts = true;
  private static volatile boolean showActionHud = false;
  private static volatile boolean showActionHints = true;
  private static volatile boolean keyListenEnabled = true;

  private ModernUiClientState() {}

  public static boolean showToasts() {
    return showToasts;
  }

  public static boolean showActionHud() {
    return showActionHud;
  }

  public static boolean showActionHints() {
    return showActionHints;
  }

  public static boolean isKeyListenEnabled() {
    return keyListenEnabled;
  }

  public static void setShowToasts(boolean v) {
    showToasts = v;
  }

  public static void setShowActionHud(boolean v) {
    showActionHud = v;
  }

  public static void setShowActionHints(boolean v) {
    showActionHints = v;
  }

  public static void setKeyListenEnabled(boolean value) {
    keyListenEnabled = value;
  }
}
