package net.tigereye.chestcavity.compat.guzhenren.flyingsword.client.override;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.FlyingSwordEntity;

/** 运行时覆盖注册表，由资源重载器填充。 */
public final class SwordModelOverrideRegistry {
  private static final Map<String, SwordModelOverrideDef> BY_KEY = new HashMap<>();

  private SwordModelOverrideRegistry() {}

  public static Optional<SwordModelOverrideDef> getForKey(String key) {
    if (key == null || key.isEmpty()) return Optional.empty();
    return Optional.ofNullable(BY_KEY.get(key));
  }

  public static Optional<SwordModelOverrideDef> getForSword(FlyingSwordEntity sword) {
    if (sword == null) return Optional.empty();
    String key = sword.getModelKey();
    return getForKey(key);
  }

  public static Map<String, SwordModelOverrideDef> snapshot() {
    return Collections.unmodifiableMap(BY_KEY);
  }

  public static void replaceAll(Map<String, SwordModelOverrideDef> defs) {
    BY_KEY.clear();
    if (defs != null) BY_KEY.putAll(defs);
  }
}
