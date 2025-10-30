package net.tigereye.chestcavity.registration;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.tigereye.chestcavity.skill.effects.Effect;
import net.tigereye.chestcavity.skill.effects.SkillEffectBus;
import net.tigereye.chestcavity.skill.effects.builtin.CompositeEffect;
import net.tigereye.chestcavity.skill.effects.builtin.UseItemPhaseFilterEffect;

/**
 * 便捷注册器：将 itemTagRegex / itemIdRegex 映射为 SkillEffectBus 上可匹配的 hookId 正则，
 * 并按相位注册对应的效果链。
 *
 * <p>示例：
 * UseItemEffectBuilder.forItemTagRegex("^guzhenren:ying_dao$")
 *   .onStart(new FlagEffect("use_shadow_tool"))
 *   .onFinish(CompositeEffect.of(new AttributeEffect(...)))
 *   .register();
 */
public final class UseItemEffectBuilder {

  private final String hookRegex;
  private final List<Effect> start = new ArrayList<>();
  private final List<Effect> finish = new ArrayList<>();
  private final List<Effect> abort = new ArrayList<>();

  private UseItemEffectBuilder(String hookRegex) {
    this.hookRegex = Objects.requireNonNull(hookRegex, "hookRegex");
  }

  public static UseItemEffectBuilder forItemTagRegex(String itemTagRegex) {
    String p = normalizeNsPathRegex(itemTagRegex);
    return new UseItemEffectBuilder("^chestcavity:use_item/tag/" + p);
  }

  public static UseItemEffectBuilder forItemIdRegex(String itemIdRegex) {
    String p = normalizeNsPathRegex(itemIdRegex);
    return new UseItemEffectBuilder("^chestcavity:use_item/item/" + p);
  }

  public UseItemEffectBuilder onStart(Effect... effects) {
    if (effects != null) java.util.Collections.addAll(start, effects);
    return this;
  }

  public UseItemEffectBuilder onFinish(Effect... effects) {
    if (effects != null) java.util.Collections.addAll(finish, effects);
    return this;
  }

  public UseItemEffectBuilder onAbort(Effect... effects) {
    if (effects != null) java.util.Collections.addAll(abort, effects);
    return this;
  }

  public void register() {
    if (!start.isEmpty()) {
      SkillEffectBus.register(
          hookRegex,
          new UseItemPhaseFilterEffect(
              UseItemPhaseFilterEffect.PhaseKind.START, CompositeEffect.of(start.toArray(Effect[]::new))));
    }
    if (!finish.isEmpty()) {
      SkillEffectBus.register(
          hookRegex,
          new UseItemPhaseFilterEffect(
              UseItemPhaseFilterEffect.PhaseKind.FINISH,
              CompositeEffect.of(finish.toArray(Effect[]::new))));
    }
    if (!abort.isEmpty()) {
      SkillEffectBus.register(
          hookRegex,
          new UseItemPhaseFilterEffect(
              UseItemPhaseFilterEffect.PhaseKind.ABORT, CompositeEffect.of(abort.toArray(Effect[]::new))));
    }
  }

  private static String normalizeNsPathRegex(String s) {
    if (s == null || s.isBlank()) return ".*";
    // 将 namespace:path 替换为 namespace/path，避免 ResourceLocation 路径中的冒号不被匹配
    return s.replace(':', '/');
  }
}

