package net.tigereye.chestcavity.soul.runtime;

import net.tigereye.chestcavity.soul.registry.SoulRuntimeHandlerRegistry;

import java.util.concurrent.atomic.AtomicBoolean;

public final class SoulRuntimeHandlers {

    private static final AtomicBoolean BOOT = new AtomicBoolean(false);

    private SoulRuntimeHandlers() {}

    public static void bootstrap() {
        if (BOOT.compareAndSet(false, true)) {
            SoulRuntimeHandlerRegistry.register(new DefaultSoulRuntimeHandler());
        }
    }
}

