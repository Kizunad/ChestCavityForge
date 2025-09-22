package net.tigereye.chestcavity.compat.guzhenren.linkage;

import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;

import net.minecraft.nbt.CompoundTag;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Central access point for Guzhenren linkage contexts. Provides a weakly-referenced cache of
 * {@link ActiveLinkageContext} keyed by chest cavities so the GC can reclaim unused entries
 * once the owning entity is gone.
 */
public final class GuzhenrenLinkageManager {

    private static final Map<ChestCavityInstance, ActiveLinkageContext> CONTEXTS = new WeakHashMap<>();
    private static final String STORAGE_KEY = "GuzhenrenLinkage";

    private GuzhenrenLinkageManager() {
    }

    /** Obtains the linkage context for the provided chest cavity, creating it if missing. */
    public static ActiveLinkageContext getContext(ChestCavityInstance cc) {
        synchronized (CONTEXTS) {
            return CONTEXTS.computeIfAbsent(cc, ActiveLinkageContext::new);
        }
    }

    /**
     * Dispatches a slow tick to the linkage context if one exists. Callers should gate this on whatever
     * cadence they consider a "slow" tick (e.g. once every 20 game ticks).
     */
    public static void tickSlow(ChestCavityInstance cc) {
        ActiveLinkageContext context;
        synchronized (CONTEXTS) {
            context = CONTEXTS.get(cc);
        }
        if (context != null) {
            context.onSlowTick();
        }
    }

    /** Writes linkage channel values into the provided chest cavity tag. */
    public static void save(ChestCavityInstance cc, CompoundTag ccTag) {
        ActiveLinkageContext context;
        synchronized (CONTEXTS) {
            context = CONTEXTS.get(cc);
        }
        if (context == null || context.isEmpty()) {
            ccTag.remove(STORAGE_KEY);
            return;
        }
        CompoundTag data = context.writeToTag();
        if (!data.isEmpty()) {
            ccTag.put(STORAGE_KEY, data);
        } else {
            ccTag.remove(STORAGE_KEY);
        }
    }

    /** Restores linkage channel values from the given chest cavity tag. */
    public static void load(ChestCavityInstance cc, CompoundTag ccTag) {
        if (ccTag == null || !ccTag.contains(STORAGE_KEY)) {
            return;
        }
        CompoundTag data = ccTag.getCompound(STORAGE_KEY).copy();
        getContext(cc).readFromTag(data);
    }
}
