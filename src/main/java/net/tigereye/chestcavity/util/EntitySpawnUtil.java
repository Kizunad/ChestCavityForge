package net.tigereye.chestcavity.util;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/** Minimal helper to spawn arbitrary entities by registry id. */
public final class EntitySpawnUtil {

  private EntitySpawnUtil() {}

  /**
   * Spawns an entity by id at the given position and rotation. Returns the entity if spawned, or
   * null on failure.
   */
  public static Entity spawn(
      ServerLevel level,
      ResourceLocation entityId,
      Vec3 pos,
      float yaw,
      float pitch,
      boolean noAi) {
    if (level == null || entityId == null || pos == null) {
      return null;
    }
    var registry = level.registryAccess().registryOrThrow(Registries.ENTITY_TYPE);
    EntityType<?> type = registry.get(entityId);
    if (type == null) {
      return null;
    }
    Entity entity = type.create(level);
    if (entity == null) {
      return null;
    }
    entity.moveTo(pos.x, pos.y, pos.z, yaw, pitch);
    if (noAi && entity instanceof Mob mob) {
      mob.setNoAi(true);
    }
    level.addFreshEntity(entity);
    return entity;
  }
}
