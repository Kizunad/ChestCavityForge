package net.tigereye.chestcavity.guzhenren.network;

import net.neoforged.fml.loading.FMLEnvironment;
import net.tigereye.chestcavity.ChestCavity;

/**
 * Entry point for Guzhenren player-variable interception. Client-only logic lives in
 * {@link GuzhenrenClientNetworkBridge}; this wrapper keeps the dedicated server classpath clean.
 */
public final class GuzhenrenNetworkBridge {

    private GuzhenrenNetworkBridge() {
    }

    public static void bootstrap() {
        var dist = FMLEnvironment.dist;
        ChestCavity.LOGGER.debug("[compat/guzhenren][network] bootstrap request (dist={})", dist);
        if (!dist.isClient()) {
            ChestCavity.LOGGER.debug("[compat/guzhenren][network] bootstrap skipped: non-client dist");
            return;
        }
        GuzhenrenClientNetworkBridge.bootstrap();
    }

    public static boolean registerListener(GuzhenrenPayloadListener listener) {
        if (!FMLEnvironment.dist.isClient()) {
            ChestCavity.LOGGER.debug("[compat/guzhenren][network] listener registration skipped: non-client dist");
            return false;
        }
        return GuzhenrenClientNetworkBridge.registerListener(listener);
    }

    public static boolean unregisterListener(GuzhenrenPayloadListener listener) {
        if (!FMLEnvironment.dist.isClient()) {
            return false;
        }
        return GuzhenrenClientNetworkBridge.unregisterListener(listener);
    }
}
