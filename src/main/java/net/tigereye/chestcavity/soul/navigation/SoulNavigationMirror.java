package net.tigereye.chestcavity.soul.navigation;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.soul.fakeplayer.SoulPlayer;
import net.tigereye.chestcavity.soul.util.SoulLog;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Spawns a hidden Pathfinder mob to compute ground navigation, and mirrors its
 * position back to the SoulPlayer to achieve pathing on player entities.
 */
public final class SoulNavigationMirror {

    private SoulNavigationMirror() {}

    private static final Map<UUID, Mob> GUIDES = new HashMap<>();
    private static final Map<UUID, Goal> GOALS = new HashMap<>();
    private static final Map<UUID, Double> LAST_DIST2 = new HashMap<>();
    private static final Map<UUID, Integer> STUCK_TICKS = new HashMap<>();

    public static void setGoal(SoulPlayer soul, Vec3 target, double speed, double stopDistance) {
        Mob guide = ensureGuide(soul);
        if (guide == null) return;
        guide.getNavigation().moveTo(target.x, target.y, target.z, speed);
        GOALS.put(soul.getSoulId(), new Goal(target, speed, stopDistance));
        LAST_DIST2.remove(soul.getSoulId());
        STUCK_TICKS.remove(soul.getSoulId());
        SoulLog.info("[soul][nav] setGoal soul={} target=({}, {}, {}) speed={} stop={}",
                soul.getSoulId(), target.x, target.y, target.z, speed, stopDistance);
    }

    public static void clearGoal(SoulPlayer soul) {
        Goal g = GOALS.remove(soul.getSoulId());
        Mob guide = GUIDES.get(soul.getSoulId());
        if (guide != null) {
            guide.getNavigation().stop();
        }
        if (g != null) {
            SoulLog.info("[soul][nav] clearGoal soul={}", soul.getSoulId());
        }
        LAST_DIST2.remove(soul.getSoulId());
        STUCK_TICKS.remove(soul.getSoulId());
    }

    public static void tick(SoulPlayer soul) {
        Mob guide = GUIDES.get(soul.getSoulId());
        if (guide == null) return;
        // keep guide near soul on start/reset
        double distStart = guide.distanceToSqr(soul);
        if (distStart > 16.0) { // >4 blocks
            guide.teleportTo(soul.getX(), soul.getY(), soul.getZ());
        }
        // mirror position towards guide to respect pathing
        Vec3 gp = guide.position();
        Vec3 sp = soul.position();
        Vec3 delta = gp.subtract(sp);
        double d2 = delta.lengthSqr();
        if (d2 > 0.01) {
            double maxStep = 0.6; // blocks per tick
            Vec3 step = delta.length() > maxStep ? delta.normalize().scale(maxStep) : delta;
            soul.moveTo(sp.x + step.x, sp.y + step.y, sp.z + step.z, soul.getYRot(), soul.getXRot());
        }
        // finish when close to target or stuck too long; apply fallback for cliffs
        Goal g = GOALS.get(soul.getSoulId());
        if (g != null) {
            double remain = gp.distanceToSqr(g.target);
            boolean close = remain <= (g.stopDistance * g.stopDistance);
            boolean navDone = guide.getNavigation().isDone();

            // track progress
            double prev = LAST_DIST2.getOrDefault(soul.getSoulId(), Double.MAX_VALUE);
            if (remain < prev - 0.25) { // progressed ~0.5 blocks in distance
                LAST_DIST2.put(soul.getSoulId(), remain);
                STUCK_TICKS.put(soul.getSoulId(), 0);
            } else {
                int stuck = STUCK_TICKS.getOrDefault(soul.getSoulId(), 0) + 1;
                STUCK_TICKS.put(soul.getSoulId(), stuck);
                // if stuck for ~2s or nav done but far, try fallback teleport near target ground
                if ((stuck >= 40 || navDone) && !close) {
                    if (fallbackToSafeGround(soul, guide, g)) {
                        LAST_DIST2.put(soul.getSoulId(), 0.0);
                        STUCK_TICKS.put(soul.getSoulId(), 0);
                        return; // will continue next tick
                    }
                }
            }

            if (close || navDone) {
                clearGoal(soul);
            }
        }
    }

    public static void onSoulRemoved(UUID soulId) {
        Mob guide = GUIDES.remove(soulId);
        GOALS.remove(soulId);
        if (guide != null && !guide.isRemoved()) {
            guide.discard();
        }
    }

    public static void clearAll() {
        GUIDES.values().forEach(m -> { if (!m.isRemoved()) m.discard(); });
        GUIDES.clear();
        GOALS.clear();
        LAST_DIST2.clear();
        STUCK_TICKS.clear();
    }

    private static Mob ensureGuide(SoulPlayer soul) {
        Mob existing = GUIDES.get(soul.getSoulId());
        if (existing != null && !existing.isRemoved()) return existing;
        ServerLevel level = (ServerLevel) soul.level();
        Pig pig = EntityType.PIG.create(level);
        if (pig == null) return null;
        pig.setInvisible(true);
        pig.setSilent(true);
        pig.setNoAi(false); // AI/navigation must run
        pig.setInvulnerable(true);
        pig.setPersistenceRequired();
        pig.moveTo(soul.getX(), soul.getY(), soul.getZ(), soul.getYRot(), soul.getXRot());
        if (!level.tryAddFreshEntityWithPassengers(pig)) {
            SoulLog.warn("[soul][nav] failed to spawn guide for soul={}", soul.getSoulId());
            return null;
        }
        GUIDES.put(soul.getSoulId(), pig);
        SoulLog.info("[soul][nav] spawned guide soul={} dim={}", soul.getSoulId(), level.dimension().location());
        return pig;
    }

    private record Goal(Vec3 target, double speed, double stopDistance) {}

    private static boolean fallbackToSafeGround(SoulPlayer soul, Mob guide, Goal g) {
        ServerLevel level = (ServerLevel) soul.level();
        Vec3 target = g.target;
        // search small XZ neighborhood for safe standable position near target, scanning downward up to 24 blocks
        int[] OFF = new int[]{0, 1, -1, 2, -2};
        for (int dx : OFF) {
            for (int dz : OFF) {
                if (Math.abs(dx) + Math.abs(dz) > 3) continue;
                int x = (int) Math.floor(target.x) + dx;
                int z = (int) Math.floor(target.z) + dz;
                int startY = Math.min(level.getMaxBuildHeight() - 1, (int) Math.floor(target.y) + 2);
                int minY = Math.max(level.getMinBuildHeight(), startY - 24);
                for (int y = startY; y >= minY; y--) {
                    net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(x, y, z);
                    net.minecraft.core.BlockPos below = pos.below();
                    if (level.isEmptyBlock(pos) && !level.isEmptyBlock(below) && level.getBlockState(below).isSolid()) {
                        // found ground; teleport guide and move soul towards it
                        double fx = x + 0.5;
                        double fy = y + 0.01;
                        double fz = z + 0.5;
                        guide.teleportTo(fx, fy, fz);
                        // move soul closer or snap if very far
                        Vec3 sp = soul.position();
                        Vec3 step = new Vec3(fx - sp.x, fy - sp.y, fz - sp.z);
                        if (step.lengthSqr() > 16.0) {
                            soul.teleportTo(level, fx, fy, fz, soul.getYRot(), soul.getXRot());
                        } else {
                            soul.moveTo(fx, fy, fz, soul.getYRot(), soul.getXRot());
                        }
                        SoulLog.info("[soul][nav] fallback teleport soul={} to safe ground at ({},{},{})", soul.getSoulId(), fx, fy, fz);
                        // reissue navigation to target; guide already moved near
                        guide.getNavigation().moveTo(g.target.x, g.target.y, g.target.z, g.speed);
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
