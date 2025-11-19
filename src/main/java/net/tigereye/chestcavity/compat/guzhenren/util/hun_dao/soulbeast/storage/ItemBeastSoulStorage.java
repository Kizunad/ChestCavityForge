package net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.storage;

import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.tigereye.chestcavity.util.NBTWriter;

/**
 * 默认的 {@link BeastSoulStorage} 实现，面向“承载物”（ItemStack）。 将“兽魂”数据持久化到物品堆的 {@code CustomData}（1.20+
 * 组件）中： - 根键为 {@code rootKey}（默认 {@code HunDaoSoulBeast}）； - 子键 {@code BeastSoul} 下保存：实体类型、实体
 * NBT、存储时间。
 */
public final class ItemBeastSoulStorage implements BeastSoulStorage {

  private static final String DEFAULT_ROOT_KEY = "HunDaoSoulBeast";
  private static final String STORAGE_KEY = "BeastSoul";
  private static final String KEY_ENTITY_TYPE = "EntityType";
  private static final String KEY_ENTITY_DATA = "EntityData";
  private static final String KEY_STORED_AT = "StoredGameTime";

  private final String rootKey;

  public ItemBeastSoulStorage() {
    this(DEFAULT_ROOT_KEY);
  }

  public ItemBeastSoulStorage(String rootKey) {
    this.rootKey = Objects.requireNonNull(rootKey, "rootKey");
  }

  /**
   * Checks if the item stack has a stored soul.
   *
   * @param organ The item stack.
   * @return {@code true} if the item stack has a stored soul, {@code false} otherwise.
   */
  @Override
  public boolean hasStoredSoul(ItemStack organ) {
    if (organ == null || organ.isEmpty()) {
      return false;
    }
    CustomData data = organ.get(DataComponents.CUSTOM_DATA);
    if (data == null) {
      return false;
    }
    CompoundTag root = data.copyTag();
    if (!root.contains(rootKey, Tag.TAG_COMPOUND)) {
      return false;
    }
    CompoundTag state = root.getCompound(rootKey);
    return state.contains(STORAGE_KEY, Tag.TAG_COMPOUND);
  }

  /**
   * Stores the soul of the given entity in the item stack.
   *
   * @param organ The item stack.
   * @param entity The entity.
   * @param storedGameTime The game time at which the entity was stored.
   * @return An optional containing the beast soul record, or an empty optional if the soul could
   *     not be stored.
   */
  @Override
  public Optional<BeastSoulRecord> store(
      ItemStack organ, net.minecraft.world.entity.LivingEntity entity, long storedGameTime) {
    if (!canStore(organ, entity)) {
      return Optional.empty();
    }
    return BeastSoulRecord.fromEntity(entity, storedGameTime)
        .map(
            record -> {
              writeRecord(organ, record);
              return record;
            });
  }

  /**
   * Peeks at the stored soul in the item stack.
   *
   * @param organ The item stack.
   * @return An optional containing the beast soul record, or an empty optional if no soul is
   *     stored.
   */
  @Override
  public Optional<BeastSoulRecord> peek(ItemStack organ) {
    if (organ == null || organ.isEmpty()) {
      return Optional.empty();
    }
    CustomData data = organ.get(DataComponents.CUSTOM_DATA);
    if (data == null) {
      return Optional.empty();
    }
    CompoundTag root = data.copyTag();
    if (!root.contains(rootKey, Tag.TAG_COMPOUND)) {
      return Optional.empty();
    }
    CompoundTag state = root.getCompound(rootKey);
    if (!state.contains(STORAGE_KEY, Tag.TAG_COMPOUND)) {
      return Optional.empty();
    }
    CompoundTag storage = state.getCompound(STORAGE_KEY);
    if (!storage.contains(KEY_ENTITY_TYPE, Tag.TAG_STRING)) {
      return Optional.empty();
    }
    String rawId = storage.getString(KEY_ENTITY_TYPE);
    ResourceLocation id = ResourceLocation.tryParse(rawId);
    if (id == null) {
      return Optional.empty();
    }
    CompoundTag entityData =
        storage.contains(KEY_ENTITY_DATA, Tag.TAG_COMPOUND)
            ? storage.getCompound(KEY_ENTITY_DATA).copy()
            : new CompoundTag();
    long storedAt =
        storage.contains(KEY_STORED_AT, Tag.TAG_LONG) ? storage.getLong(KEY_STORED_AT) : 0L;
    return Optional.of(new BeastSoulRecord(id, entityData, storedAt));
  }

  /**
   * Consumes the stored soul from the item stack.
   *
   * @param organ The item stack.
   * @return An optional containing the beast soul record, or an empty optional if no soul was
   *     stored.
   */
  @Override
  public Optional<BeastSoulRecord> consume(ItemStack organ) {
    Optional<BeastSoulRecord> record = peek(organ);
    record.ifPresent(unused -> clear(organ));
    return record;
  }

  /**
   * Clears the stored soul from the item stack.
   *
   * @param organ The item stack.
   */
  @Override
  public void clear(ItemStack organ) {
    if (organ == null || organ.isEmpty()) {
      return;
    }
    NBTWriter.updateCustomData(
        organ,
        tag -> {
          if (!tag.contains(rootKey, Tag.TAG_COMPOUND)) {
            return;
          }
          CompoundTag state = tag.getCompound(rootKey);
          if (state.contains(STORAGE_KEY)) {
            state.remove(STORAGE_KEY);
          }
          if (state.isEmpty()) {
            tag.remove(rootKey);
          } else {
            tag.put(rootKey, state);
          }
        });
  }

  /** 将快照写入到物品的 {@code CustomData} 中。 */
  private void writeRecord(ItemStack organ, BeastSoulRecord record) {
    NBTWriter.updateCustomData(
        organ,
        tag -> {
          CompoundTag state =
              tag.contains(rootKey, Tag.TAG_COMPOUND)
                  ? tag.getCompound(rootKey)
                  : new CompoundTag();
          CompoundTag storage = new CompoundTag();
          storage.putString(KEY_ENTITY_TYPE, record.entityTypeId().toString());
          storage.put(KEY_ENTITY_DATA, record.entityData().copy());
          storage.putLong(KEY_STORED_AT, record.storedGameTime());
          state.put(STORAGE_KEY, storage);
          tag.put(rootKey, state);
        });
  }
}
