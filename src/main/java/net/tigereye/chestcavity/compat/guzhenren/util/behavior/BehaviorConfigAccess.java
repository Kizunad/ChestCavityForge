package net.tigereye.chestcavity.compat.guzhenren.util.behavior;

import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.config.CCConfig;

/** Lightweight helper that sources Guzhenren behaviour tuning values from the config. */
public final class BehaviorConfigAccess {

  private BehaviorConfigAccess() {}

  public static int getInt(Class<?> owner, String fieldName, int defaultValue) {
    CCConfig.GuzhenrenBehaviorTuningConfig tuning = tuning();
    if (tuning == null) {
      return defaultValue;
    }
    return tuning.resolveInt(key(owner, fieldName), defaultValue);
  }

  public static float getFloat(Class<?> owner, String fieldName, float defaultValue) {
    CCConfig.GuzhenrenBehaviorTuningConfig tuning = tuning();
    if (tuning == null) {
      return defaultValue;
    }
    return tuning.resolveFloat(key(owner, fieldName), defaultValue);
  }

  public static boolean getBoolean(Class<?> owner, String fieldName, boolean defaultValue) {
    CCConfig.GuzhenrenBehaviorTuningConfig tuning = tuning();
    if (tuning == null) {
      return defaultValue;
    }
    return tuning.resolveBoolean(key(owner, fieldName), defaultValue);
  }

  private static CCConfig.GuzhenrenBehaviorTuningConfig tuning() {
    CCConfig config = ChestCavity.config;
    if (config == null) {
      return null;
    }
    return config.GUZHENREN_BEHAVIOR;
  }

  private static String key(Class<?> owner, String fieldName) {
    String ownerName = owner != null ? owner.getName() : "unknown";
    return ownerName + "." + fieldName;
  }
}
