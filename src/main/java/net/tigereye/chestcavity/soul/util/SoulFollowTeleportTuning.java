package net.tigereye.chestcavity.soul.util;

/**
 * 跟随/传送参数的运行时调优点，支持 ModernUI 动态下发。
 *
 * <p>默认： - followTriggerDist = 3.0 格 - teleportEnabled = true - teleportDist = 20.0 格
 */
public final class SoulFollowTeleportTuning {

  private SoulFollowTeleportTuning() {}

  private static volatile double followTriggerDist =
      getDoubleProp("chestcavity.soul.followTriggerDist", 3.0, 1.0, 8.0);
  private static volatile boolean teleportEnabled =
      Boolean.parseBoolean(System.getProperty("chestcavity.soul.teleportEnabled", "true"));
  private static volatile double teleportDist =
      getDoubleProp("chestcavity.soul.teleportDist", 20.0, 8.0, 128.0);

  public static double followTriggerDist() {
    return followTriggerDist;
  }

  public static boolean teleportEnabled() {
    return teleportEnabled;
  }

  public static double teleportDist() {
    return teleportDist;
  }

  public static void setFollowTriggerDist(double v) {
    followTriggerDist = clamp(v, 1.0, 8.0);
  }

  public static void setTeleportEnabled(boolean enabled) {
    teleportEnabled = enabled;
  }

  public static void setTeleportDist(double v) {
    teleportDist = clamp(v, 8.0, 128.0);
  }

  private static double clamp(double v, double min, double max) {
    if (v < min) {
      return min;
    }
    if (v > max) {
      return max;
    }
    return v;
  }

  private static double getDoubleProp(String key, double def, double min, double max) {
    String s = System.getProperty(key);
    if (s == null) {
      return def;
    }
    try {
      return clamp(Double.parseDouble(s), min, max);
    } catch (NumberFormatException e) {
      return def;
    }
  }
}
