package net.tigereye.chestcavity.compat.guzhenren.flyingsword.client.profile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.FlyingSwordEntity;

/** 运行时视觉配置注册表。 */
public final class SwordVisualProfileRegistry {
  private static final Map<String, SwordVisualProfile> BY_KEY = new HashMap<>();

  private SwordVisualProfileRegistry() {}

  public static Optional<SwordVisualProfile> getForSword(FlyingSwordEntity sword) {
    if (sword == null) return Optional.empty();
    String mk = sword.getModelKey();
    if (mk == null || mk.isEmpty()) return Optional.empty();
    SwordVisualProfile p = BY_KEY.get(mk);
    if (p == null || !p.enabled) return Optional.empty();
    // 简化匹配：key==modelKey 或 列表包含
    List<String> matches = p.matchModelKeys;
    if (!matches.isEmpty() && !matches.contains(mk)) return Optional.empty();
    return Optional.of(p);
  }

  public static void replaceAll(Map<String, SwordVisualProfile> defs) {
    BY_KEY.clear();
    if (defs != null) BY_KEY.putAll(defs);
  }
}
