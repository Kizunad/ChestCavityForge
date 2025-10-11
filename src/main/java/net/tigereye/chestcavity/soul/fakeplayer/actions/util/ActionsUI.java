package net.tigereye.chestcavity.soul.fakeplayer.actions.util;

/**
 * Bridge for Actions-related UI/telemetry toggles.
 * Hook into CCConfig in future; defaults keep UI on.
 */
public final class ActionsUI {
    private ActionsUI() {}

    public static boolean showActionToasts() { return true; }
    public static boolean showActionHud() { return true; }
    public static boolean showCommandHints() { return true; }
}

