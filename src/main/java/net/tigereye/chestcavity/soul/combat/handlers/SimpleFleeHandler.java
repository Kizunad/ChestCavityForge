package net.tigereye.chestcavity.soul.combat.handlers;

import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.soul.combat.FleeContext;
import net.tigereye.chestcavity.soul.combat.SoulFleeHandler;
import net.tigereye.chestcavity.soul.navigation.SoulNavigationMirror;

/**
 * Simple flee: move opposite to threat for a short distance and let navigation
 * find a safe ground; anchor is typically owner's position.
 */
public final class SimpleFleeHandler implements SoulFleeHandler {
    private final double fleeDistance;
    private final double speed;
    private final double stop;

    public SimpleFleeHandler() {
        this(10.0, 1.3, 2.5);
    }

    public SimpleFleeHandler(double fleeDistance, double speed, double stop) {
        this.fleeDistance = fleeDistance;
        this.speed = speed;
        this.stop = stop;
    }

    @Override
    public boolean tryFlee(FleeContext ctx) {
        var self = ctx.self();
        var threat = ctx.threat();
        if (!threat.isAlive()) return false;
        var dir = self.position().subtract(threat.position());
        if (dir.lengthSqr() < 1e-4) {
            dir = self.position().subtract(ctx.anchor());
            if (dir.lengthSqr() < 1e-4) {
                return false;
            }
        }
        Vec3 target = self.position().add(dir.normalize().scale(fleeDistance));
        SoulNavigationMirror.setGoal(self, target, speed, stop);
        return true;
    }
}

