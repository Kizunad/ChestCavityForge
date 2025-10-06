package net.tigereye.chestcavity.soul.combat;

import net.minecraft.world.entity.LivingEntity;
import net.tigereye.chestcavity.soul.fakeplayer.SoulPlayer;
import net.tigereye.chestcavity.soul.util.SoulLog;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class SoulAttackRegistry {
    private static final List<SoulAttackHandler> HANDLERS = new CopyOnWriteArrayList<>();

    private SoulAttackRegistry() {}

    public static void register(SoulAttackHandler handler) {
        if (handler != null) HANDLERS.add(handler);
    }

    public static void unregister(SoulAttackHandler handler) {
        HANDLERS.remove(handler);
    }

    /** Attempts all handlers in order; executes the first that is in-range and succeeds. */
    public static boolean attackIfInRange(SoulPlayer self, LivingEntity target) {
        if (target == null || !target.isAlive()) return false;
        AttackContext ctx = AttackContext.of(self, target);
        for (SoulAttackHandler h : HANDLERS) {
            double r = Math.max(0.0, h.getRange(self, target));
            if (ctx.distance() <= r) {
                boolean ok = false;
                try {
                    ok = h.tryAttack(ctx);
                } catch (Throwable t) {
                    SoulLog.error("[soul][attack] handler threw", t);
                }
                if (ok) return true;
            }
        }
        return false;
    }

    /** Returns the maximum range among registered handlers for this target. */
    public static double maxRange(SoulPlayer self, LivingEntity target) {
        double max = 0.0;
        for (SoulAttackHandler h : HANDLERS) {
            max = Math.max(max, h.getRange(self, target));
        }
        return max;
    }
}

