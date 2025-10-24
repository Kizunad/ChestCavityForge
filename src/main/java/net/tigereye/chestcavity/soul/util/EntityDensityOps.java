package net.tigereye.chestcavity.soul.util;

import java.util.List;
import java.util.Optional;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** 计算附近实体密度，供游荡逻辑使用。 */
public final class EntityDensityOps {

  private EntityDensityOps() {}

  public static Optional<Vec3> findDensePoint(Entity origin, double searchRadius, int samples) {
    if (origin == null || !origin.isAlive()) {
      return Optional.empty();
    }
    Level level = origin.level();
    Vec3 center = origin.position();
    Vec3 best = null;
    double bestScore = Double.NEGATIVE_INFINITY;
    for (int i = 0; i < samples; i++) {
      double angle = origin.getRandom().nextDouble() * Math.PI * 2.0;
      double radius = origin.getRandom().nextDouble() * searchRadius;
      Vec3 candidate = center.add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
      double score = densityScore(level, origin, candidate, searchRadius / 3.0);
      if (score > bestScore) {
        bestScore = score;
        best = candidate;
      }
    }
    return Optional.ofNullable(best);
  }

  private static double densityScore(Level level, Entity origin, Vec3 pos, double radius) {
    AABB box = new AABB(pos, pos).inflate(radius);
    List<LivingEntity> entities =
        level.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e != origin);
    return entities.stream()
        .mapToDouble(
            entity -> 1.0 / Math.max(1.0, entity.distanceToSqr(pos.x, entity.getY(), pos.z)))
        .sum();
  }
}
