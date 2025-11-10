package net.tigereye.chestcavity.compat.common.skillcalc.mod;

import net.minecraft.server.level.ServerPlayer;
import net.tigereye.chestcavity.compat.common.skillcalc.DamageComputeContext;
import net.tigereye.chestcavity.compat.common.skillcalc.SkillDamageModifier;
import net.tigereye.chestcavity.skill.effects.EffectContext;
import net.tigereye.chestcavity.skill.effects.builtin.FlagEffect;

/** 从 SkillEffect 的一次性标记中提取增伤（如 one_hit_bonus / one_cast_bonus）。 计算阶段消费标记，避免重复累计。 */
public final class AttackerFlagBonusModifier implements SkillDamageModifier {

  @Override
  public double apply(
      DamageComputeContext ctx, double current, SkillDamageModifier.SkillDamageSink sink) {
    if (current <= 0.0) return 0.0;
    if (ctx.skillId() == null || ctx.castId() <= 0) return current;
    if (ctx.attacker().asPlayer().isEmpty()) return current;
    ServerPlayer sp = ctx.attacker().asPlayer().get();

    // 构建 EffectContext 以消费 Flag（只在攻击者侧生效）
    EffectContext ectx =
        EffectContext.build(sp, ctx.skillId(), ctx.attacker().chestCavity(), ctx.castId());
    double bonus = 0.0;
    double v1 = FlagEffect.consume(ectx, "one_hit_bonus");
    if (Double.isFinite(v1)) bonus += v1;
    double v2 = FlagEffect.consume(ectx, "one_cast_bonus");
    if (Double.isFinite(v2)) bonus += v2;

    if (bonus != 0.0) {
      double after = current * Math.max(0.0, 1.0 + bonus);
      sink.mul("flag_bonus", 1.0 + bonus, after);
      return after;
    }
    return current;
  }
}
