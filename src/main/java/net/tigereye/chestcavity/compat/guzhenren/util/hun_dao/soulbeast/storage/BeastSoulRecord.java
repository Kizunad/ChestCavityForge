package net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.storage;

import java.util.Objects;
import java.util.Optional;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/**
 * Immutable snapshot of a beast soul stored inside a carrier item.
 *
 * <p>Fields capture:
 *
 * <ul>
 *   <li>{@code entityTypeId} – type identifier of the captured entity
 *   <li>{@code entityData} – raw NBT payload (without entity ID)
 *   <li>{@code storedGameTime} – world time (ticks) when the entity was stored
 * </ul>
 */
public record BeastSoulRecord(
    ResourceLocation entityTypeId, CompoundTag entityData, long storedGameTime) {

  /**
   * Normalizes the snapshot fields.
   *
   * @param entityTypeId Captured entity identifier.
   * @param entityData Serialized entity payload.
   * @param storedGameTime Game time when the capture occurred.
   */
  public BeastSoulRecord {
    Objects.requireNonNull(entityTypeId, "entityTypeId");
    entityData = entityData == null ? new CompoundTag() : entityData.copy();
  }

  /**
   * Serializes the given entity into a {@link BeastSoulRecord} snapshot.
   *
   * @param entity The entity to serialize.
   * @param storedGameTime The game time at which the entity was stored.
   * @return An optional containing the beast soul record, or an empty optional if the entity is
   *     null, on the client side, or its type cannot be resolved.
   */
  public static Optional<BeastSoulRecord> fromEntity(LivingEntity entity, long storedGameTime) {
    if (entity == null || entity.level().isClientSide()) {
      return Optional.empty();
    }
    ResourceLocation id = EntityType.getKey(entity.getType());
    if (id == null) {
      return Optional.empty();
    }
    CompoundTag tag = new CompoundTag();
    entity.saveWithoutId(tag);
    return Optional.of(new BeastSoulRecord(id, tag, storedGameTime));
  }

  /**
   * Tries to instantiate the entity corresponding to this snapshot in the specified level.
   *
   * @param level The level in which to create the entity.
   * @return An optional containing the created entity, or an empty optional if the level is null,
   *     the entity type ID is null, or the entity type cannot be resolved.
   */
  public Optional<Entity> createEntity(Level level) {
    if (level == null || entityTypeId == null) {
      return Optional.empty();
    }
    Optional<EntityType<?>> typeOpt = EntityType.byString(entityTypeId.toString());
    if (typeOpt.isEmpty()) {
      return Optional.empty();
    }
    EntityType<?> type = typeOpt.get();
    Entity entity = type.create(level);
    if (entity == null) {
      return Optional.empty();
    }
    entity.load(entityData.copy());
    return Optional.of(entity);
  }
}
