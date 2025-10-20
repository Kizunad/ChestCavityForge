package net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.damage;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Registry for Soul Beast damage listeners.
 */
public final class SoulBeastDamageHooks {

    private static final List<SoulBeastDamageListener> LISTENERS = new CopyOnWriteArrayList<>();

    private SoulBeastDamageHooks() {
    }

    public static void register(SoulBeastDamageListener listener) {
        Objects.requireNonNull(listener, "listener");
        if (!LISTENERS.contains(listener)) {
            LISTENERS.add(listener);
        }
    }

    public static void unregister(SoulBeastDamageListener listener) {
        if (listener != null) {
            LISTENERS.remove(listener);
        }
    }

    public static double applyHunpoCostModifiers(SoulBeastDamageContext context, double baseHunpoCost) {
        double cost = baseHunpoCost;
        for (SoulBeastDamageListener listener : LISTENERS) {
            cost = listener.modifyHunpoCost(context, cost);
        }
        return cost;
    }

    public static float applyPostConversionDamageModifiers(SoulBeastDamageContext context, float baseDamage) {
        float damage = baseDamage;
        for (SoulBeastDamageListener listener : LISTENERS) {
            damage = listener.modifyPostConversionDamage(context, damage);
        }
        return damage;
    }
}
