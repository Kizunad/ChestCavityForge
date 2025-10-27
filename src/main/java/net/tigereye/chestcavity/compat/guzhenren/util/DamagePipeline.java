package net.tigereye.chestcavity.compat.guzhenren.util;

import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * 线程本地的伤害累加器：行为端登记额外加伤/乘区，事件退出时一次性合成回 {@link
 * LivingIncomingDamageEvent#setAmount(float)}。 保留守卫位避免递归。
 */
public final class DamagePipeline implements AutoCloseable {

  private static final ThreadLocal<DamagePipeline> TL = new ThreadLocal<>();
  private static final ThreadLocal<Boolean> GUARD = ThreadLocal.withInitial(() -> false);

  private float bonus = 0f;
  private float multiplier = 1f;
  private final LivingIncomingDamageEvent event;

  private DamagePipeline(LivingIncomingDamageEvent event) {
    this.event = event;
  }

  public static DamagePipeline open(LivingIncomingDamageEvent event) {
    DamagePipeline pipeline = new DamagePipeline(event);
    TL.set(pipeline);
    GUARD.set(true);
    return pipeline;
  }

  public static boolean active() {
    return TL.get() != null;
  }

  public static void addBonus(float value) {
    if (active()) {
      TL.get().bonus += value;
    }
  }

  public static void multiply(float value) {
    if (active()) {
      TL.get().multiplier *= value;
    }
  }

  public static boolean guarded() {
    return GUARD.get();
  }

  @Override
  public void close() {
    try {
      float base = event.getAmount();
      float result = Math.max(0f, base * multiplier + bonus);
      event.setAmount(result);
    } finally {
      TL.remove();
      GUARD.set(false);
    }
  }
}
