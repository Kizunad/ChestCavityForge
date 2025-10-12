package net.tigereye.chestcavity.soul.navigation;

/**
 * Available navigation engine choices for souls. BARITONE currently delegates to
 * the vanilla engine until the real integration is enabled, but keeping the enum
 * public allows commands/config to switch per-soul or globally without code churn.
 */
public enum SoulNavEngine {
    VANILLA,
    BARITONE;

    public static SoulNavEngine fromProperty(String v) {
        if (v == null) return VANILLA;
        String s = v.trim().toLowerCase(java.util.Locale.ROOT);
        // Accept common aliases and fqcn-like inputs that contain the keyword
        if (s.equals("baritone") || s.equals("b") || s.contains("baritone")) {
            return BARITONE;
        }
        if (s.equals("vanilla") || s.equals("v")) {
            return VANILLA;
        }
        return VANILLA;
    }
}
