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

  /** 将给定实体序列化为 {@link BeastSoulRecord} 快照。 仅在服务端、且实体类型可解析时返回。 */
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

  /** 尝试在指定维度实例化该快照对应的实体。 注意：调用方需要自行设置位置并加入世界。 */
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
