package net.tigereye.chestcavity.soul.navigation.barintegrate;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import net.tigereye.chestcavity.soul.util.SoulLog;

/**
 * Reflection-based bridge to Baritone API. This class intentionally avoids compile-time
 * dependencies so that the mod can run without Baritone present.
 *
 * <p>Responsibilities - Detect if Baritone API classes are available (considering relocated package
 * names). - Apply a minimal baseline settings profile that is safe for servers: allowBreak=false,
 * allowPlace=false, allowParkour=false, allowParkourPlace=false, allowSprint=true,
 * primaryTimeoutMS=2500L.
 */
public final class BaritoneFacade {

  private static final String[] API_CLASS_CANDIDATES =
      new String[] {
        // Relocated simple package
        "net.tigereye.chestcavity.shadow.baritone.api.BaritoneAPI",
        // Relocated com.github package
        "net.tigereye.chestcavity.shadow.com.github.cabaletta.baritone.api.BaritoneAPI",
        // Original simple
        "baritone.api.BaritoneAPI",
        // Original com.github package
        "com.github.cabaletta.baritone.api.BaritoneAPI"
      };

  private static volatile boolean initialized = false;
  private static volatile boolean available = false;

  private static Class<?> clazzBaritoneAPI;
  private static Method mGetSettings;
  private static Object settingsInstance;
  private static Class<?> clazzSettings;
  private static Class<?> clazzSetting;
  private static Field fSettingValue;

  private static final Map<String, Field> SETTINGS_FIELDS = new HashMap<>();

  private BaritoneFacade() {}

  public static boolean isAvailable() {
    ensureInitialized();
    return available;
  }

  public static void ensureInitialized() {
    if (initialized) return;
    synchronized (BaritoneFacade.class) {
      if (initialized) return;
      try {
        clazzBaritoneAPI = tryLoadAny(API_CLASS_CANDIDATES);
        if (clazzBaritoneAPI == null) {
          available = false;
          initialized = true;
          return;
        }
        mGetSettings = clazzBaritoneAPI.getMethod("getSettings");
        settingsInstance = mGetSettings.invoke(null);
        if (settingsInstance == null) {
          available = false;
          initialized = true;
          return;
        }
        clazzSettings = settingsInstance.getClass();
        // A representative field type is baritone.api.Settings$Setting
        // We discover it via one known field (e.g., allowSprint), else via any field type named
        // *Setting
        Field probe = findSettingsField("allowSprint");
        if (probe == null) {
          // fallback: grab any field ending with 'Setting'
          for (Field f : clazzSettings.getFields()) {
            if (f.getType().getSimpleName().toLowerCase().contains("setting")) {
              probe = f;
              break;
            }
          }
        }
        if (probe == null) {
          available = false;
          initialized = true;
          return;
        }
        clazzSetting = probe.getType();
        fSettingValue = clazzSetting.getField("value");

        // Cache commonly used fields for speed
        cacheField("allowBreak");
        cacheField("allowPlace");
        cacheField("allowParkour");
        cacheField("allowParkourPlace");
        cacheField("allowSprint");
        cacheField("primaryTimeoutMS");

        available = true;
      } catch (Throwable t) {
        available = false;
        SoulLog.info("[soul][nav][baritone] init failed: {}", t.toString());
      } finally {
        initialized = true;
        // 成功时也打印一条，便于定位初始化完成时机
        if (available) {
          SoulLog.info("[soul][nav][baritone] initialized (facade ready)");
        }
      }
    }
  }

  private static Class<?> tryLoadAny(String[] candidates) {
    for (String name : candidates) {
      try {
        return Class.forName(name);
      } catch (ClassNotFoundException ignored) {
      }
    }
    return null;
  }

  private static void cacheField(String name) {
    Field f = findSettingsField(name);
    if (f != null) SETTINGS_FIELDS.put(name, f);
  }

  private static Field findSettingsField(String name) {
    try {
      return clazzSettings.getField(name);
    } catch (NoSuchFieldException e) {
      return null;
    }
  }

  /**
   * Apply a minimal baseline that ensures Baritone won't break/place blocks and behaves
   * conservatively on servers.
   */
  public static void applyBaselineSettings() {
    if (!isAvailable()) return;
    try {
      setBoolean("allowBreak", false);
      setBoolean("allowPlace", false);
      setBoolean("allowParkour", false);
      setBoolean("allowParkourPlace", false);
      setBoolean("allowSprint", true);
      setLong("primaryTimeoutMS", getLong("primaryTimeoutMS", 2500L));
    } catch (Throwable t) {
      if (SoulLog.DEBUG_LOGS) {
        SoulLog.info("[soul][nav][baritone] applyBaseline failed: {}", t.toString());
      }
    }
  }

  private static Object getSettingObject(String fieldName) throws IllegalAccessException {
    Field f = SETTINGS_FIELDS.get(fieldName);
    if (f == null) throw new IllegalStateException("Unknown setting field: " + fieldName);
    return f.get(settingsInstance);
  }

  private static void setBoolean(String fieldName, boolean v) throws IllegalAccessException {
    Object setting = getSettingObject(fieldName);
    fSettingValue.setBoolean(setting, v);
  }

  private static void setLong(String fieldName, long v) throws IllegalAccessException {
    Object setting = getSettingObject(fieldName);
    fSettingValue.setLong(setting, v);
  }

  private static long getLong(String fieldName, long def) throws IllegalAccessException {
    Object setting = getSettingObject(fieldName);
    try {
      return fSettingValue.getLong(setting);
    } catch (IllegalArgumentException e) {
      return def;
    }
  }
}
