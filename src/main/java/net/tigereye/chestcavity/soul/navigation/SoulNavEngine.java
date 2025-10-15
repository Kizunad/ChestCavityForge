package net.tigereye.chestcavity.soul.navigation;

/**
 * Available navigation engine choices for souls. BARITONE currently delegates to
 * the vanilla engine until the real integration is enabled, but keeping the enum
 * public allows commands/config to switch per-soul or globally without code churn.
 */
public enum SoulNavEngine {
    VANILLA,
    BARITONE,
    /**
     * 虚拟导航的“自动步进”策略：更偏向台阶抬升而非跳跃，
     * 放宽步进判定阈值并缩短冷却，尽量模拟“autostep”体验。
     */
    AUTOSTEP;

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
        if (s.equals("autostep") || s.equals("auto") || s.equals("as") || s.contains("autostep")) {
            return AUTOSTEP;
        }
        return VANILLA;
    }
}
