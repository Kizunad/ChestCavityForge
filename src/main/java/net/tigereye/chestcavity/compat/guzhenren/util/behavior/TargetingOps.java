package net.tigereye.chestcavity.compat.guzhenren.util.behavior;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Targeting helpers for common AoE patterns.
 */
public final class TargetingOps {

    private TargetingOps() {}

    /**
     * Returns all living entities within radius of user, excluding the user, excluding allies, and requiring alive.
     */
    public static List<LivingEntity> hostilesWithinRadius(LivingEntity user, ServerLevel level, double radius) {
        if (user == null || level == null || radius <= 0.0) {
            return java.util.List.of();
        }
        AABB area = user.getBoundingBox().inflate(radius);
        return level.getEntitiesOfClass(LivingEntity.class, area, target ->
                target != null && target != user && target.isAlive() && !target.isAlliedTo(user));
    }
}
