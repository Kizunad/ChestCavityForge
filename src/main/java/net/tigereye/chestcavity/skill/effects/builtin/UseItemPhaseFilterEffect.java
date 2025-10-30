package net.tigereye.chestcavity.skill.effects.builtin;

import net.tigereye.chestcavity.skill.ActiveSkillRegistry;
import net.tigereye.chestcavity.skill.effects.AppliedHandle;
import net.tigereye.chestcavity.skill.effects.Effect;
import net.tigereye.chestcavity.skill.effects.EffectContext;

/**
 * 物品使用相位过滤器：仅在指定 UseItem 相位触发内层效果。
 */
public final class UseItemPhaseFilterEffect implements Effect {

  public enum PhaseKind {
    START,
    FINISH,
    ABORT
  }

  private final PhaseKind phase;
  private final Effect inner;

  public UseItemPhaseFilterEffect(PhaseKind phase, Effect inner) {
    this.phase = phase;
    this.inner = inner;
  }

  @Override
  public AppliedHandle applyPre(EffectContext ctx) {
    EffectContext.UseItemInfo ui = ctx.useItem();
    if (ui == null) return null;
    if (phase != PhaseKind.START) return null;
    return inner == null ? null : inner.applyPre(ctx);
  }

  @Override
  public void applyPost(EffectContext ctx, ActiveSkillRegistry.TriggerResult result) {
    EffectContext.UseItemInfo ui = ctx.useItem();
    if (ui == null) return;
    if (phase == PhaseKind.FINISH && ui.phase() == EffectContext.UseItemInfo.Phase.FINISH) {
      if (inner != null) inner.applyPost(ctx, result);
      return;
    }
    if (phase == PhaseKind.ABORT && ui.phase() == EffectContext.UseItemInfo.Phase.ABORT) {
      if (inner != null) inner.applyPost(ctx, result);
    }
  }
}

