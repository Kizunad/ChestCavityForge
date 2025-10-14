package net.tigereye.chestcavity.compat.guzhenren.util.behavior;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * Generic short-range teleport helpers for Guzhenren organ behaviours.
 *
 * <p>Designed to provide deterministic blinks without duplicating caller-specific logic.
 * Performs conservative safety checks (chunk loaded, world border, collision) and
 * supports limited vertical adjustment when the target spot is obstructed.</p>
 */
public final class TeleportOps {

    private TeleportOps() {
    }

    private static final double DEFAULT_VERTICAL_STEP = 0.5D;
    private static final int DEFAULT_VERTICAL_ATTEMPTS = 6;

    /**
     * Attempt to blink an entity towards the provided absolute position. Returns the final
     * teleport destination if successful.
     *
     * @param entity           living entity to teleport (server-side only)
     * @param target           desired absolute destination
     * @param verticalAttempts number of vertical adjustments to probe up/down (per direction)
     * @param verticalStep     step size (in blocks) for each vertical probe
     */
    public static Optional<Vec3> blinkTo(
            LivingEntity entity,
            Vec3 target,
            int verticalAttempts,
            double verticalStep
    ) {
        if (entity == null || target == null) {
            return Optional.empty();
        }
        Level level = entity.level();
        if (!(level instanceof ServerLevel serverLevel) || entity.isRemoved()) {
            return Optional.empty();
        }
        Vec3 origin = entity.position();
        Vec3 delta = target.subtract(origin);

        if (tryTeleport(entity, serverLevel, delta)) {
            return Optional.of(entity.position());
        }

        double step = Math.max(0.1D, verticalStep);
        int attempts = Math.max(0, verticalAttempts);

        // Probe upwards first, then downwards, to prefer safe headroom.
        for (int i = 1; i <= attempts; i++) {
            Vec3 offsetUp = delta.add(0.0D, step * i, 0.0D);
            if (tryTeleport(entity, serverLevel, offsetUp)) {
                return Optional.of(entity.position());
            }
        }
        for (int i = 1; i <= attempts; i++) {
            Vec3 offsetDown = delta.add(0.0D, -step * i, 0.0D);
            if (tryTeleport(entity, serverLevel, offsetDown)) {
                return Optional.of(entity.position());
            }
        }
        return Optional.empty();
    }

    /**
     * Attempt to blink an entity along the provided offset. The final destination equals
     * {@code entity.position() + offset} (subject to vertical adjustments).
     */
    public static Optional<Vec3> blinkOffset(LivingEntity entity, Vec3 offset) {
        if (entity == null || offset == null) {
            return Optional.empty();
        }
        Vec3 target = entity.position().add(offset);
        return blinkTo(entity, target, DEFAULT_VERTICAL_ATTEMPTS, DEFAULT_VERTICAL_STEP);
    }

    /**
     * Attempt to blink the entity away from an anchor position by a fixed distance.
     * When the anchor coincides with the entity, the entity's look direction is used.
     */
    public static Optional<Vec3> blinkAwayFrom(LivingEntity entity, Vec3 anchor, double distance) {
        if (entity == null || distance <= 0.0D) {
            return Optional.empty();
        }
        Vec3 origin = entity.position();
        Vec3 direction = (anchor == null ? null : origin.subtract(anchor));
        if (direction == null || direction.lengthSqr() < 1.0E-4D) {
            direction = entity.getLookAngle();
        }
        if (direction.lengthSqr() < 1.0E-4D) {
            // Fallback to unit X to avoid NaN
            direction = new Vec3(1.0D, 0.0D, 0.0D);
        }
        Vec3 offset = direction.normalize().scale(distance);
        return blinkOffset(entity, offset);
    }

    private static boolean tryTeleport(LivingEntity entity, ServerLevel level, Vec3 offset) {
        if (entity == null || offset == null || level == null) {
            return false;
        }
        Vec3 origin = entity.position();
        Vec3 destination = origin.add(offset);
        double targetX = destination.x;
        double targetY = destination.y;
        double targetZ = destination.z;

        BlockPos blockPos = BlockPos.containing(targetX, targetY, targetZ);
        if (!level.isLoaded(blockPos)) {
            return false;
        }
        WorldBorder border = level.getWorldBorder();
        if (!border.isWithinBounds(targetX, targetZ)) {
            return false;
        }
        if (targetY < level.getMinBuildHeight()) {
            return false;
        }
        if (targetY > level.getMaxBuildHeight() + 1) {
            return false;
        }

        Vec3 movement = destination.subtract(origin);
        AABB movedBox = entity.getBoundingBox().move(movement);
        if (!level.noCollision(entity, movedBox)) {
            return false;
        }
        entity.teleportTo(targetX, targetY, targetZ);
        entity.setDeltaMovement(entity.getDeltaMovement().scale(0.25D));
        entity.fallDistance = 0.0F;
        return true;
    }
}
