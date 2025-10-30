package net.tigereye.chestcavity.skill.effects.builtin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.tigereye.chestcavity.skill.ActiveSkillRegistry;
import net.tigereye.chestcavity.skill.effects.AppliedHandle;
import net.tigereye.chestcavity.skill.effects.Effect;
import net.tigereye.chestcavity.skill.effects.EffectContext;

/**
 * 简单的效果组合器：顺序执行多个 Effect，pre 阶段返回组合句柄用于统一回滚。
 */
public final class CompositeEffect implements Effect {

  private final List<Effect> effects;

  public CompositeEffect(List<Effect> effects) {
    this.effects = effects == null ? List.of() : List.copyOf(effects);
  }

  public static CompositeEffect of(Effect... effects) {
    return new CompositeEffect(effects == null ? List.of() : Arrays.asList(effects));
  }

  @Override
  public AppliedHandle applyPre(EffectContext ctx) {
    if (effects.isEmpty()) return null;
    List<AppliedHandle> handles = new ArrayList<>();
    int maxTtl = 0;
    for (Effect e : effects) {
      AppliedHandle h = null;
      try {
        h = e.applyPre(ctx);
      } catch (Throwable ignored) {
      }
      if (h != null) {
        handles.add(h);
        maxTtl = Math.max(maxTtl, Math.max(0, h.ttlTicks()));
      }
    }
    if (handles.isEmpty()) return null;
    final int ttl = maxTtl;
    return new AppliedHandle() {
      @Override
      public int ttlTicks() {
        return ttl;
      }

      @Override
      public String debugName() {
        return "CompositeHandle(" + handles.size() + ")";
      }

      @Override
      public void revert() {
        for (AppliedHandle h : handles) {
          try {
            h.revert();
          } catch (Throwable ignored) {
          }
        }
      }
    };
  }

  @Override
  public void applyPost(EffectContext ctx, ActiveSkillRegistry.TriggerResult result) {
    for (Effect e : effects) {
      try {
        e.applyPost(ctx, result);
      } catch (Throwable ignored) {
      }
    }
  }
}

