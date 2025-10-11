package net.tigereye.chestcavity.soul.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.guzhenren.util.GuzhenrenResourceCostHelper;
import net.tigereye.chestcavity.soul.fakeplayer.SoulPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Evade/kite helper for SoulPlayer: performs an instant blink 10–20 blocks away
 * (prefers higher valid landing blocks) and consumes 50 jingli when used.
 * Applies a 5-second cooldown per soul to avoid spam.
 */
public final class SoulEvadeHelper {

    private SoulEvadeHelper() {}

    private static final int COOLDOWN_TICKS = 100; // 5s at 20tps
    private static final double MIN_DIST = 10.0;
    private static final double MAX_DIST = 20.0;
    private static final int SAMPLE_DIRECTIONS = 12;
    private static final int MAX_ASCEND = 8; // prefer up to +8 blocks higher

    private static final Map<UUID, Long> LAST_EVADE = new ConcurrentHashMap<>();

    /** Attempt an evade blink if off cooldown and resource is available. */
    public static boolean tryEvade(SoulPlayer soul, LivingEntity hazard, double distance, double threshold) {
        if (soul == null || hazard == null) return false;
        if (soul.level().isClientSide()) return false;
        ServerLevel level = soul.serverLevel();
        long now = level.getGameTime();
        Long last = LAST_EVADE.get(soul.getSoulId());
        if (last != null && now - last < COOLDOWN_TICKS) return false;

        // Consume 50 jingli (strict, no HP fallback for players). If this returns not ok, abort.
        GuzhenrenResourceCostHelper.ConsumptionResult cost = ResourceOps.consumeStrict(soul, 0.0, 50.0);
        if (!cost.succeeded()) {
            net.tigereye.chestcavity.soul.util.SoulLog.info(
                    "[soul][evade] denied: soul={} reason={} dist={}/{}", soul.getSoulId(),
                    cost.failureReason(), String.format("%.2f", distance), String.format("%.2f", threshold));
            return false;
        }

        Vec3 from = soul.position();
        RandomSource rng = soul.getRandom();
        boolean found = false;
        double bestX = 0, bestY = Double.NEGATIVE_INFINITY, bestZ = 0;
        double bestDist = 0;

        // Prefer directions away from the hazard but allow randomness
        Vec3 away = from.subtract(hazard.position());
        double baseYaw = Math.atan2(away.z, away.x);

        for (int i = 0; i < SAMPLE_DIRECTIONS; i++) {
            // +/- up to ~75 degrees with some randomness
            double angle = baseYaw + (rng.nextDouble() - 0.5) * (Mth.DEG_TO_RAD * 150.0);
            double dist = Mth.lerp(rng.nextDouble(), MIN_DIST, MAX_DIST);
            double cx = from.x + Math.cos(angle) * dist;
            double cz = from.z + Math.sin(angle) * dist;

            // Use heightmap to find a landing Y, preferring higher ground but capped
            int bx = Mth.floor(cx);
            int bz = Mth.floor(cz);
            int motionY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, bx, bz);
            // Constrain candidate Y to within a reasonable ascend window relative to current
            int minY = Mth.floor(from.y) - 2; // allow small drops
            int maxY = Mth.floor(from.y) + MAX_ASCEND;
            int ly = Mth.clamp(motionY, minY, maxY);

            // Ensure two blocks of headroom at (bx, ly, bz)
            BlockPos feet = new BlockPos(bx, ly, bz);
            BlockPos head = feet.above();
            if (!level.isEmptyBlock(feet) || !level.isEmptyBlock(head)) {
                // Heightmap may return top surface; position is block-occupied; try above it
                ly += 1;
                feet = new BlockPos(bx, ly, bz);
                head = feet.above();
                if (!level.isEmptyBlock(feet) || !level.isEmptyBlock(head)) {
                    continue; // not enough space
                }
            }

            // Collision check using current bounding box moved to target
            AABB moved = soul.getBoundingBox().move(cx - soul.getX(), ly - soul.getY(), cz - soul.getZ());
            if (!level.noCollision(moved)) continue;

            double actualHorizontal = Math.sqrt(Math.pow(cx - from.x, 2) + Math.pow(cz - from.z, 2));
            if (actualHorizontal < MIN_DIST * 0.9) {
                continue; // not far enough to count as an evade
            }

            // Prefer highest landing Y among valid candidates
            if (ly > bestY) {
                bestY = ly;
                bestX = cx;
                bestZ = cz;
                found = true;
                bestDist = actualHorizontal;
            }
        }

        if (!found) {
            // Refund resource if we couldn't find a spot
            ResourceOps.refund(soul, cost);
            net.tigereye.chestcavity.soul.util.SoulLog.info(
                    "[soul][evade] no-spot soul={} dist={}/{} refunded", soul.getSoulId(),
                    String.format("%.2f", distance), String.format("%.2f", threshold));
            return false;
        }

        // Teleport to the chosen position
        soul.teleportTo(level, bestX, bestY, bestZ, soul.getYRot(), soul.getXRot());
        net.tigereye.chestcavity.soul.util.SoulLog.info(
                "[soul][evade] success soul={} dist={}/{} actual={} to=({},{},{})", soul.getSoulId(),
                String.format("%.2f", distance), String.format("%.2f", threshold),
                String.format("%.2f", bestDist),
                String.format("%.2f", bestX), bestY, String.format("%.2f", bestZ));
        LAST_EVADE.put(soul.getSoulId(), now);
        return true;
    }
}
