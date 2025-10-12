package net.tigereye.chestcavity.soul.navigation.plan;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.soul.fakeplayer.SoulPlayer;
import net.tigereye.chestcavity.soul.navigation.barintegrate.BaritoneFacade;
import net.tigereye.chestcavity.soul.util.SoulLog;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Reflection-based Baritone planner stub.
 *
 * Current stage: only reports availability and returns empty (caller must fallback).
 * Next stages: use reflection to invoke Baritone path finding and convert to Vec3 waypoints.
 */
public final class BaritonePathPlanner implements ISoulPathPlanner {

    private final long defaultTimeoutMs;

    public BaritonePathPlanner() {
        String t = System.getProperty("chestcavity.soul.baritone.timeoutMs", "2500");
        long parsed;
        try { parsed = Long.parseLong(t); } catch (NumberFormatException e) { parsed = 2500L; }
        this.defaultTimeoutMs = Math.max(250L, Math.min(10_000L, parsed));
    }

    @Override
    public Optional<List<Vec3>> planPath(ServerLevel level, SoulPlayer soul, Vec3 target, long timeoutMs) {
        if (!BaritoneFacade.isAvailable()) {
            return Optional.empty();
        }
        var ownerId = soul.getOwnerId().orElse(null);
        if (ownerId == null) return Optional.empty();
        var owner = level.getServer().getPlayerList().getPlayer(ownerId);
        if (owner == null) return Optional.empty();
        long to = timeoutMs > 0 ? timeoutMs : this.defaultTimeoutMs;
        return net.tigereye.chestcavity.soul.navigation.net.SoulNavPlanBroker.requestPlan(owner, soul.getSoulId(), soul.position(), target, to);
    }
}
