package net.tigereye.chestcavity.compat.common.skillcalc;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.tigereye.chestcavity.compat.common.agent.Agent;
import net.tigereye.chestcavity.compat.common.agent.Agents;
import net.tigereye.chestcavity.compat.common.skillcalc.mod.AttackerFlagBonusModifier;
import net.tigereye.chestcavity.compat.common.skillcalc.mod.DefenderResistanceModifier;

/**
 * 通用伤害计算器：顺序应用注册的修正规则并返回带明细的结果。
 */
public final class DamageCalculator {

  private DamageCalculator() {}

  private static final List<SkillDamageModifier> MODIFIERS = new CopyOnWriteArrayList<>();

  static {
    // 内置规则（顺序很重要）：
    MODIFIERS.add(new AttackerFlagBonusModifier());
    MODIFIERS.add(new DefenderResistanceModifier());
  }

  public static void register(SkillDamageModifier modifier) {
    if (modifier != null && !MODIFIERS.contains(modifier)) {
      MODIFIERS.add(modifier);
    }
  }

  public static void unregister(SkillDamageModifier modifier) {
    MODIFIERS.remove(modifier);
  }

  public static DamageResult compute(
      LivingEntity attacker,
      LivingEntity defender,
      double baseDamage,
      ResourceLocation skillId,
      long castId,
      java.util.Set<DamageKind> kinds) {
    Objects.requireNonNull(attacker, "attacker");
    DamageComputeContext ctx =
        DamageComputeContext
            .builder(attacker, baseDamage)
            .defender(defender)
            .skill(skillId)
            .cast(castId)
            .build();
    if (kinds != null) {
      for (DamageKind k : kinds) {
        if (k != null) ctx = DamageComputeContext.builder(attacker, baseDamage)
            .defender(defender)
            .skill(skillId)
            .cast(castId)
            .addKind(k)
            .build();
      }
    }
    return compute(ctx);
  }

  public static DamageResult compute(DamageComputeContext ctx) {
    double current = Math.max(0.0, ctx.baseDamage());
    List<DamageResult.Entry> breakdown = new ArrayList<>();
    SkillDamageModifier.SkillDamageSink sink =
        new SkillDamageModifier.SkillDamageSink() {
          @Override
          public void mul(String label, double factor, double after) {
            breakdown.add(new DamageResult.Entry(label, DamageResult.Entry.Kind.MULTIPLY, factor, after));
          }

          @Override
          public void add(String label, double delta, double after) {
            breakdown.add(new DamageResult.Entry(label, DamageResult.Entry.Kind.ADD, delta, after));
          }

          @Override
          public void clamp(String label, double after) {
            breakdown.add(new DamageResult.Entry(label, DamageResult.Entry.Kind.CLAMP, 0.0, after));
          }
        };

    for (SkillDamageModifier m : MODIFIERS) {
      try {
        double before = current;
        current = m.apply(ctx, current, sink);
        if (!Double.isFinite(current)) current = before;
      } catch (Throwable ignored) {}
    }
    current = Math.max(0.0, current);

    double spentAbs = 0.0;
    double healthDamage = current;
    Agent defender = ctx.defender();
    if (defender != null) {
      float abs = defender.living().getAbsorptionAmount();
      if (abs > 0.0f) {
        spentAbs = Math.min(abs, current);
        healthDamage = Math.max(0.0, current - spentAbs);
      }
    }
    return new DamageResult(ctx.baseDamage(), current, breakdown, spentAbs, healthDamage);
  }
}

