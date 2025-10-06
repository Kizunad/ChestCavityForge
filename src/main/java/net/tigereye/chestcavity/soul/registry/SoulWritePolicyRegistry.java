package net.tigereye.chestcavity.soul.registry;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Global registry for the active write policy. Keeps a single active policy
 * reference with a conservative default.
 */
public final class SoulWritePolicyRegistry {

    private static final AtomicReference<SoulWritePolicy> ACTIVE = new AtomicReference<>(new StrictPolicy());

    private SoulWritePolicyRegistry() {}

    public static SoulWritePolicy get() { return ACTIVE.get(); }

    public static void set(SoulWritePolicy policy) {
        if (policy != null) ACTIVE.set(policy);
    }

    /** Default strict policy that mirrors current engine behaviour. */
    public static final class StrictPolicy implements SoulWritePolicy { }
}

