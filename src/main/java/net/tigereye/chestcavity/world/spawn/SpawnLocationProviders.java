package net.tigereye.chestcavity.world.spawn;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * 常用位置选择器。
 */
public final class SpawnLocationProviders {

    private SpawnLocationProviders() {}

    public static SpawnLocationProvider nearRandomPlayer() {
        return SpawnLocationProviders::findNearRandomPlayer;
    }

    private static Optional<SpawnLocation> findNearRandomPlayer(MinecraftServer server,
                                                                CustomMobSpawnDefinition definition,
                                                                RandomSource random) {
        List<ServerPlayer> players = server.getPlayerList().getPlayers().stream()
                .filter(player -> !player.isSpectator())
                .filter(player -> !player.isRemoved())
                .toList();
        if (players.isEmpty()) {
            return Optional.empty();
        }
        ServerPlayer anchor = players.get(random.nextInt(players.size()));
        ServerLevel level = anchor.serverLevel();
        double range = Math.max(1.0D, definition.horizontalRange());
        double distance = Math.sqrt(random.nextDouble()) * range;
        double angle = random.nextDouble() * Math.PI * 2.0D;
        double offsetX = Math.cos(angle) * distance;
        double offsetZ = Math.sin(angle) * distance;
        double baseX = anchor.getX() + offsetX;
        double baseZ = anchor.getZ() + offsetZ;
        BlockPos surface = findSurface(level, BlockPos.containing(baseX, anchor.getY(), baseZ));
        if (surface == null) {
            return Optional.empty();
        }
        Vec3 spawnPos = Vec3.atCenterOf(surface);
        if (spawnPos.distanceToSqr(anchor.position()) < definition.minPlayerDistance() * definition.minPlayerDistance()) {
            Vec3 dir = spawnPos.subtract(anchor.position()).normalize();
            spawnPos = anchor.position().add(dir.scale(Math.max(definition.minPlayerDistance(), 1.0D)));
            surface = findSurface(level, BlockPos.containing(spawnPos.x, spawnPos.y, spawnPos.z));
            if (surface == null) {
                return Optional.empty();
            }
            spawnPos = Vec3.atCenterOf(surface);
        }
        float yaw = random.nextFloat() * 360.0F;
        float pitch = 0.0F;
        return Optional.of(new SpawnLocation(level, spawnPos, yaw, pitch));
    }

    private static BlockPos findSurface(LevelAccessor level, BlockPos origin) {
        if (!level.hasChunkAt(origin)) {
            return null;
        }
        BlockPos pos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, origin);
        if (!level.getBlockState(pos.below()).isSolid()) {
            // 向下搜索最近的实体站立位置
            BlockPos.MutableBlockPos cursor = pos.mutable();
            for (int i = 0; i < 8; i++) {
                cursor.move(0, -1, 0);
                if (level.getBlockState(cursor).isSolid()) {
                    return cursor.above();
                }
            }
        }
        return pos;
    }
}
