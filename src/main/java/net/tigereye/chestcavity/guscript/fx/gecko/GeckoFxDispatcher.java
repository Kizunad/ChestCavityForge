package net.tigereye.chestcavity.guscript.fx.gecko;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.guscript.network.packets.GeckoFxEventPayload;
import net.tigereye.chestcavity.network.NetworkHandler;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

/**
 * Broadcasts Gecko FX payloads to nearby players. Server-only.
 */
public final class GeckoFxDispatcher {

    private static final double BROADCAST_RADIUS = 64.0D;

    private static BiConsumer<DispatchContext, GeckoFxEventPayload> testOverride;

    private GeckoFxDispatcher() {
    }

    public static void emit(ServerLevel level, Vec3 origin, GeckoFxEventPayload payload) {
        if (level == null || payload == null || level.isClientSide()) {
            return;
        }
        if (testOverride != null) {
            testOverride.accept(new DispatchContext(level, origin), payload);
            return;
        }
        double radiusSq = BROADCAST_RADIUS * BROADCAST_RADIUS;
        for (ServerPlayer viewer : level.players()) {
            if (viewer == null) {
                continue;
            }
            if (viewer.connection == null) {
                continue;
            }
            if (viewer.getId() == payload.attachedEntityId()) {
                NetworkHandler.sendGeckoFx(viewer, payload);
                continue;
            }
            if (origin == null || viewer.distanceToSqr(origin) <= radiusSq) {
                NetworkHandler.sendGeckoFx(viewer, payload);
            }
        }
    }

    public static void overrideForTests(BiConsumer<DispatchContext, GeckoFxEventPayload> override) {
        testOverride = override;
    }

    public static void resetOverrideForTests() {
        testOverride = null;
    }

    public record DispatchContext(ServerLevel level, Vec3 origin) {}
}

