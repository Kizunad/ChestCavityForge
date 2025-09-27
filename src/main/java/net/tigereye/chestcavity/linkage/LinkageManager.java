package net.tigereye.chestcavity.linkage;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Central access point for linkage contexts. Relocated from the legacy compat layer so linkage becomes a first-class feature. Provides a weakly-referenced cache of
 * {@link ActiveLinkageContext} keyed by chest cavities so the GC can reclaim unused entries
 * once the owning entity is gone.
 */
public final class LinkageManager {

    private static final Map<ChestCavityInstance, ActiveLinkageContext> CONTEXTS = new WeakHashMap<>();
    private static final String STORAGE_KEY = "GuzhenrenLinkage";

    private LinkageManager() {
    }

    /** Obtains the linkage context for the provided chest cavity, creating it if missing. */
    public static ActiveLinkageContext getContext(ChestCavityInstance cc) {
        synchronized (CONTEXTS) {
            ActiveLinkageContext context = CONTEXTS.get(cc);
            if (context == null) {
                context = new ActiveLinkageContext(cc);
                CONTEXTS.put(cc, context);
                if (ChestCavity.LOGGER.isDebugEnabled()) {
                    ChestCavity.LOGGER.debug("[linkage] Created linkage context for {}", describeChestCavity(cc));
                }
            } else if (ChestCavity.LOGGER.isTraceEnabled()) {
                ChestCavity.LOGGER.trace("[linkage] Reusing cached linkage context for {}", describeChestCavity(cc));
            }
            return context;
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
            if (ChestCavity.LOGGER.isTraceEnabled()) {
                ChestCavity.LOGGER.trace(
                        "[linkage] Slow tick dispatch for {} ({} channels)",
                        describeChestCavity(cc),
                        context.channelCount()
                );
            }
            context.onSlowTick();
        } else if (ChestCavity.LOGGER.isTraceEnabled()) {
            ChestCavity.LOGGER.trace("[linkage] Slow tick skipped for {} because no context exists", describeChestCavity(cc));
        }
    }

    /** Writes linkage channel values into the provided chest cavity tag. */
    public static void save(ChestCavityInstance cc, CompoundTag ccTag) {
        ActiveLinkageContext context;
        synchronized (CONTEXTS) {
            context = CONTEXTS.get(cc);
        }
        if (context == null || context.isEmpty()) {
            if (ChestCavity.LOGGER.isTraceEnabled()) {
                ChestCavity.LOGGER.trace("[linkage] Clearing linkage data for {} during save", describeChestCavity(cc));
            }
            ccTag.remove(STORAGE_KEY);
            return;
        }
        CompoundTag data = context.writeToTag();
        if (!data.isEmpty()) {
            ccTag.put(STORAGE_KEY, data);
            if (ChestCavity.LOGGER.isDebugEnabled()) {
                ChestCavity.LOGGER.debug(
                        "[linkage] Saved {} linkage channels for {}",
                        data.getAllKeys().size(),
                        describeChestCavity(cc)
                );
            }
        } else {
            ccTag.remove(STORAGE_KEY);
            if (ChestCavity.LOGGER.isTraceEnabled()) {
                ChestCavity.LOGGER.trace("[linkage] No linkage data persisted for {} (empty tag)", describeChestCavity(cc));
            }
        }
    }

    /** Restores linkage channel values from the given chest cavity tag. */
    public static void load(ChestCavityInstance cc, CompoundTag ccTag) {
        if (ccTag == null || !ccTag.contains(STORAGE_KEY)) {
            if (ChestCavity.LOGGER.isTraceEnabled()) {
                ChestCavity.LOGGER.trace("[linkage] No linkage NBT found when loading {}", describeChestCavity(cc));
            }
            return;
        }
        CompoundTag data = ccTag.getCompound(STORAGE_KEY).copy();
        if (ChestCavity.LOGGER.isDebugEnabled()) {
            ChestCavity.LOGGER.debug(
                    "[linkage] Loading linkage data for {} ({} keys)",
                    describeChestCavity(cc),
                    data.getAllKeys().size()
            );
        }
        getContext(cc).readFromTag(data);
    }

    private static String describeChestCavity(ChestCavityInstance cc) {
        if (cc == null) {
            return "<null>";
        }
        LivingEntity owner = cc.owner;
        if (owner == null) {
            return "<unbound@" + System.identityHashCode(cc) + ">";
        }
        return owner.getScoreboardName();
    }
}
