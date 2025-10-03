package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.storage;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.tigereye.chestcavity.util.NBTWriter;

import java.util.Objects;
import java.util.Optional;

/**
 * Default {@link BeastSoulStorage} implementation that persists data into the organ's CustomData payload.
 */
public final class OrganBeastSoulStorage implements BeastSoulStorage {

    private static final String DEFAULT_ROOT_KEY = "HunDaoSoulBeast";
    private static final String STORAGE_KEY = "BeastSoul";
    private static final String KEY_ENTITY_TYPE = "EntityType";
    private static final String KEY_ENTITY_DATA = "EntityData";
    private static final String KEY_STORED_AT = "StoredGameTime";

    private final String rootKey;

    public OrganBeastSoulStorage() {
        this(DEFAULT_ROOT_KEY);
    }

    public OrganBeastSoulStorage(String rootKey) {
        this.rootKey = Objects.requireNonNull(rootKey, "rootKey");
    }

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

    @Override
    public Optional<BeastSoulRecord> store(ItemStack organ, net.minecraft.world.entity.LivingEntity entity, long storedGameTime) {
        if (!canStore(organ, entity)) {
            return Optional.empty();
        }
        return BeastSoulRecord.fromEntity(entity, storedGameTime).map(record -> {
            writeRecord(organ, record);
            return record;
        });
    }

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
        CompoundTag entityData = storage.contains(KEY_ENTITY_DATA, Tag.TAG_COMPOUND)
                ? storage.getCompound(KEY_ENTITY_DATA).copy()
                : new CompoundTag();
        long storedAt = storage.contains(KEY_STORED_AT, Tag.TAG_LONG) ? storage.getLong(KEY_STORED_AT) : 0L;
        return Optional.of(new BeastSoulRecord(id, entityData, storedAt));
    }

    @Override
    public Optional<BeastSoulRecord> consume(ItemStack organ) {
        Optional<BeastSoulRecord> record = peek(organ);
        record.ifPresent(unused -> clear(organ));
        return record;
    }

    @Override
    public void clear(ItemStack organ) {
        if (organ == null || organ.isEmpty()) {
            return;
        }
        NBTWriter.updateCustomData(organ, tag -> {
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

    private void writeRecord(ItemStack organ, BeastSoulRecord record) {
        NBTWriter.updateCustomData(organ, tag -> {
            CompoundTag state = tag.contains(rootKey, Tag.TAG_COMPOUND) ? tag.getCompound(rootKey) : new CompoundTag();
            CompoundTag storage = new CompoundTag();
            storage.putString(KEY_ENTITY_TYPE, record.entityTypeId().toString());
            storage.put(KEY_ENTITY_DATA, record.entityData().copy());
            storage.putLong(KEY_STORED_AT, record.storedGameTime());
            state.put(STORAGE_KEY, storage);
            tag.put(rootKey, state);
        });
    }
}
