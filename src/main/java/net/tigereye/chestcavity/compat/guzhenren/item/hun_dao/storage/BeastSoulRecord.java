package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.storage;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.Objects;
import java.util.Optional;

/**
 * Immutable representation of a stored beast soul.
 * Encapsulates the entity type, raw serialized data and the game time when the soul was captured.
 */
public record BeastSoulRecord(ResourceLocation entityTypeId, CompoundTag entityData, long storedGameTime) {

    public BeastSoulRecord {
        Objects.requireNonNull(entityTypeId, "entityTypeId");
        entityData = entityData == null ? new CompoundTag() : entityData.copy();
    }

    /**
     * Serialises the provided entity into a {@link BeastSoulRecord} snapshot.
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
     * Attempts to materialise the stored entity in the provided level.
     * The caller is responsible for positioning and adding the entity to the world.
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
