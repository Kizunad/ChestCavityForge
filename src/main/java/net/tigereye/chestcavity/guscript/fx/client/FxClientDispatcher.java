package net.tigereye.chestcavity.guscript.fx.client;

import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.guscript.fx.FxRegistry;
import net.tigereye.chestcavity.guscript.network.packets.FxEventPayload;

/**
 * Applies incoming FX network payloads on the client.
 */
public final class FxClientDispatcher {

    private FxClientDispatcher() {}

    public static void handle(FxEventPayload payload) {
        Vec3 origin = new Vec3(payload.originX(), payload.originY(), payload.originZ());
        Vec3 fallback = new Vec3(payload.directionX(), payload.directionY(), payload.directionZ());
        Vec3 look = new Vec3(payload.lookX(), payload.lookY(), payload.lookZ());
        Vec3 target = payload.hasTarget() ? new Vec3(payload.targetX(), payload.targetY(), payload.targetZ()) : null;
        FxRegistry.FxContext context = new FxRegistry.FxContext(origin, fallback, look, target, payload.intensity(), payload.performerId(), payload.targetId());
        FxRegistry.play(payload.effectId(), context);
    }
}
