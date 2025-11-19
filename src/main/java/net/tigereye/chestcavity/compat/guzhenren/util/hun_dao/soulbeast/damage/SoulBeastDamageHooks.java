package net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.damage;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/** Registry for Soul Beast damage listeners. */
public final class SoulBeastDamageHooks {

  private static final List<SoulBeastDamageListener> LISTENERS = new CopyOnWriteArrayList<>();

  private SoulBeastDamageHooks() {}

  /**
   * Registers a soul beast damage listener.
   *
   * @param listener The listener to register.
   */
  public static void register(SoulBeastDamageListener listener) {
    Objects.requireNonNull(listener, "listener");
    if (!LISTENERS.contains(listener)) {
      LISTENERS.add(listener);
    }
  }

  /**
   * Unregisters a soul beast damage listener.
   *
   * @param listener The listener to unregister.
   */
  public static void unregister(SoulBeastDamageListener listener) {
    if (listener != null) {
      LISTENERS.remove(listener);
    }
  }

  /**
   * Applies hunpo cost modifiers to the base hunpo cost.
   *
   * @param context The damage context.
   * @param baseHunpoCost The base hunpo cost.
   * @return The modified hunpo cost.
   */
  public static double applyHunpoCostModifiers(
      SoulBeastDamageContext context, double baseHunpoCost) {
    double cost = baseHunpoCost;
    for (SoulBeastDamageListener listener : LISTENERS) {
      cost = listener.modifyHunpoCost(context, cost);
    }
    return cost;
  }

  /**
   * Applies post-conversion damage modifiers to the base damage.
   *
   * @param context The damage context.
   * @param baseDamage The base damage.
   * @return The modified damage.
   */
  public static float applyPostConversionDamageModifiers(
      SoulBeastDamageContext context, float baseDamage) {
    float damage = baseDamage;
    for (SoulBeastDamageListener listener : LISTENERS) {
      damage = listener.modifyPostConversionDamage(context, damage);
    }
    return damage;
  }
}
