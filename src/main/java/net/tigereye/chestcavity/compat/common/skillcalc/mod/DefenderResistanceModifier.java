package net.tigereye.chestcavity.compat.common.skillcalc.mod;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.tigereye.chestcavity.compat.common.skillcalc.DamageComputeContext;
import net.tigereye.chestcavity.compat.common.skillcalc.SkillDamageModifier;

/**
 * 目标抗性：若目标拥有 vanilla 抗性效果（DAMAGE_RESISTANCE），按 (1-0.2*(amp+1)) 缩放，最大减免 80%。
 * 注意：这是近似实现，用于统一“技能内计算预估值”；最终以游戏引擎实际结算为准。
 */
public final class DefenderResistanceModifier implements SkillDamageModifier {

  @Override
  public double apply(DamageComputeContext ctx, double current, SkillDamageSink sink) {
    if (current <= 0.0) return 0.0;
    if (ctx.defender() == null) return current;
    // TRUE_DAMAGE 视为“绕过常规抗性”，在计算阶段跳过该修正
    if (ctx.kind(net.tigereye.chestcavity.compat.common.skillcalc.DamageKind.TRUE_DAMAGE)) {
      return current;
    }
    MobEffectInstance inst = ctx.defender().living().getEffect(MobEffects.DAMAGE_RESISTANCE);
    if (inst == null) return current;
    int amp = Math.max(0, inst.getAmplifier());
    double reduce = Math.min(0.8, 0.2 * (amp + 1));
    double after = current * (1.0 - reduce);
    sink.mul("defender.resistance", 1.0 - reduce, after);
    return after;
  }
}
