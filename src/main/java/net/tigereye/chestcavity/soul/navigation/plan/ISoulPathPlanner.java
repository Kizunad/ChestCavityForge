package net.tigereye.chestcavity.soul.navigation.plan;

import java.util.List;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.soul.fakeplayer.SoulPlayer;

/**
 * Computes a waypoint sequence from A to B for a soul. Implementations should do heavy work
 * off-thread and return quickly, or provide a non-blocking API. This minimal interface returns
 * synchronously and is expected to be called from a worker thread.
 */
public interface ISoulPathPlanner {
  Optional<List<Vec3>> planPath(ServerLevel level, SoulPlayer soul, Vec3 target, long timeoutMs);
}
