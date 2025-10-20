package net.tigereye.chestcavity.soulbeast.storage;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.Objects;
import java.util.Optional;

/**
 * @deprecated 迁移至 {@link net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.storage.BeastSoulRecord}
 */
@Deprecated(forRemoval = true)
public record BeastSoulRecord(ResourceLocation entityTypeId, CompoundTag entityData, long storedGameTime) {

    public BeastSoulRecord {
        Objects.requireNonNull(entityTypeId, "entityTypeId");
        entityData = entityData == null ? new CompoundTag() : entityData.copy();
    }

    public static Optional<BeastSoulRecord> fromEntity(LivingEntity entity, long storedGameTime) {
        return net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.storage.BeastSoulRecord
                .fromEntity(entity, storedGameTime)
                .map(BeastSoulRecord::fromCompat);
    }

    public Optional<Entity> createEntity(Level level) {
        return toCompat().createEntity(level);
    }

    public net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.storage.BeastSoulRecord toCompat() {
        return new net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.storage.BeastSoulRecord(entityTypeId,
                entityData == null ? new CompoundTag() : entityData.copy(), storedGameTime);
    }

    static BeastSoulRecord fromCompat(net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.storage.BeastSoulRecord record) {
        return new BeastSoulRecord(record.entityTypeId(), record.entityData(), record.storedGameTime());
    }
}
