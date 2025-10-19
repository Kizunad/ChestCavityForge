package net.tigereye.chestcavity.soul.util;

import java.util.function.Predicate;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * 通用地表传送工具。
 */
public final class SurfaceTeleportOps {

    private SurfaceTeleportOps() {
    }

    public static boolean tryTeleportToSurface(LivingEntity entity,
                                               double chance,
                                               double minPlayerDistance) {
        if (entity == null || entity.level().isClientSide) {
            return false;
        }
        if (entity.getRandom().nextDouble() >= chance) {
            return false;
        }
        Level level = entity.level();
        if (level.players().stream()
                .anyMatch(player -> player.distanceTo(entity) < minPlayerDistance)) {
            return false;
        }
        Vec3 pos = entity.position();
        BlockPos column = BlockPos.containing(pos.x, pos.y, pos.z);
        int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column.getX(), column.getZ());
        if (surfaceY <= column.getY()) {
            return false;
        }
        Vec3 destination = new Vec3(pos.x, surfaceY + 1.0, pos.z);
        return entity.randomTeleport(destination.x, destination.y, destination.z, true);
    }
}
