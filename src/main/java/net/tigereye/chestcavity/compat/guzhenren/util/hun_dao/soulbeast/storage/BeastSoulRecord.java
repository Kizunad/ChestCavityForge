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
 * 存储于承载物中的“兽魂”快照（不可变）。
 *
 * <p>封装： - {@code entityTypeId}：实体类型标识； - {@code entityData}：实体的原始 NBT 数据（不含实体 ID）； - {@code
 * storedGameTime}：被捕获时的游戏时间（tick）。
 */
public record BeastSoulRecord(
    ResourceLocation entityTypeId, CompoundTag entityData, long storedGameTime) {

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
