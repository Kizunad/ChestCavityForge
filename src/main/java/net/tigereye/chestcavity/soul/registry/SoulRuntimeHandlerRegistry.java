package net.tigereye.chestcavity.soul.registry;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.world.damagesource.DamageSource;
import net.tigereye.chestcavity.soul.fakeplayer.SoulPlayer;

public final class SoulRuntimeHandlerRegistry {

  private static final List<SoulRuntimeHandler> HANDLERS = new CopyOnWriteArrayList<>();

  private SoulRuntimeHandlerRegistry() {}

  public static void register(SoulRuntimeHandler handler) {
    if (handler != null) HANDLERS.add(handler);
  }

  public static void unregister(SoulRuntimeHandler handler) {
    HANDLERS.remove(handler);
  }

  public static void onTickStart(SoulPlayer player) {
    for (SoulRuntimeHandler h : HANDLERS) h.onTickStart(player);
  }

  public static void onTickEnd(SoulPlayer player) {
    for (SoulRuntimeHandler h : HANDLERS) h.onTickEnd(player);
  }

  public static SoulHurtResult onHurt(SoulPlayer player, DamageSource source, float amount) {
    float current = amount;
    for (SoulRuntimeHandler h : HANDLERS) {
      SoulHurtResult res = h.onHurt(player, source, current);
      if (res == null) {
        continue;
      }
      switch (res.action()) {
        case CANCEL:
          return SoulHurtResult.cancel();
        case APPLY:
          return SoulHurtResult.applied(res.appliedResult());
        case MODIFY:
          current = res.amount();
          break;
        case PASS:
        default:
          break;
      }
    }
    return SoulHurtResult.modify(current);
  }
}
