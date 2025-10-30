package net.tigereye.chestcavity.skill.effects.builtin;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.tigereye.chestcavity.skill.effects.AppliedHandle;
import net.tigereye.chestcavity.skill.effects.Effect;
import net.tigereye.chestcavity.skill.effects.EffectContext;

/**
 * 一次性标记：用于在技能行为或伤害钩子中读取本次施放的临时倍率/标志。
 * 读取方应消费后删除（见 {@link #consume(EffectContext, String)}）。
 */
public final class FlagEffect implements Effect {

  private static final Map<String, Double> FLAGS = new ConcurrentHashMap<>();

  private final String name;
  private final double value;
  private final int ttlTicks;

  public FlagEffect(String name, double value, int ttlTicks) {
    this.name = name == null ? "flag" : name;
    this.value = value;
    this.ttlTicks = Math.max(0, ttlTicks);
  }

  @Override
  public AppliedHandle applyPre(EffectContext ctx) {
    String key = keyOf(ctx, name);
    FLAGS.put(key, value);
    return new AppliedHandle() {
      @Override
      public int ttlTicks() {
        return ttlTicks;
      }

      @Override
      public String debugName() {
        return "Flag:" + name;
      }

      @Override
      public void revert() {
        FLAGS.remove(key);
      }
    };
  }

  public static double consume(EffectContext ctx, String name) {
    String key = keyOf(ctx, name);
    Double v = FLAGS.remove(key);
    return v == null ? 0.0D : v;
  }

  private static String keyOf(EffectContext ctx, String name) {
    return ctx.player().getUUID() + ":" + ctx.skillId() + ":" + ctx.castId() + ":" + name;
  }
}

