package net.tigereye.chestcavity.soul.combat;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class SoulFleeRegistry {
    private static final List<SoulFleeHandler> HANDLERS = new CopyOnWriteArrayList<>();

    private SoulFleeRegistry() {}

    public static void register(SoulFleeHandler handler) {
        if (handler != null) HANDLERS.add(handler);
    }

    public static void unregister(SoulFleeHandler handler) {
        HANDLERS.remove(handler);
    }

    public static boolean tryFlee(FleeContext ctx) {
        for (SoulFleeHandler h : HANDLERS) {
            try {
                if (h.tryFlee(ctx)) return true;
            } catch (Throwable t) {
                net.tigereye.chestcavity.soul.util.SoulLog.error("[soul][flee] handler threw", t);
            }
        }
        return false;
    }
}

