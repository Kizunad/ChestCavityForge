package net.tigereye.chestcavity.compat.guzhenren.item.common;

import java.util.Objects;
import java.util.function.DoubleUnaryOperator;
import java.util.function.IntUnaryOperator;
import java.util.function.LongUnaryOperator;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.tigereye.chestcavity.util.NBTWriter;

/** Simple wrapper around an organ's CustomData payload with change tracking helpers. */
public final class OrganState {

  private static final IntUnaryOperator IDENTITY_INT = value -> value;
  private static final DoubleUnaryOperator IDENTITY_DOUBLE = value -> value;
  private static final LongUnaryOperator IDENTITY_LONG = value -> value;

  private final ItemStack stack;
  private final String rootKey;

  private OrganState(ItemStack stack, String rootKey) {
    this.stack = stack;
    this.rootKey = rootKey;
  }

  public static OrganState of(ItemStack stack, String rootKey) {
    return new OrganState(stack, rootKey);
  }

  public int getInt(String key, int defaultValue) {
    if (!isUsable()) {
      return defaultValue;
    }
    CustomData data = stack.get(DataComponents.CUSTOM_DATA);
    if (data == null) {
      return defaultValue;
    }
    CompoundTag root = data.copyTag();
    if (!root.contains(rootKey, Tag.TAG_COMPOUND)) {
      return defaultValue;
    }
    CompoundTag state = root.getCompound(rootKey);
    if (!state.contains(key, Tag.TAG_INT)) {
      return defaultValue;
    }
    return state.getInt(key);
  }

  public double getDouble(String key, double defaultValue) {
    if (!isUsable()) {
      return defaultValue;
    }
    CustomData data = stack.get(DataComponents.CUSTOM_DATA);
    if (data == null) {
      return defaultValue;
    }
    CompoundTag root = data.copyTag();
    if (!root.contains(rootKey, Tag.TAG_COMPOUND)) {
      return defaultValue;
    }
    CompoundTag state = root.getCompound(rootKey);
    if (!state.contains(key, Tag.TAG_DOUBLE)) {
      return defaultValue;
    }
    return state.getDouble(key);
  }

  public long getLong(String key, long defaultValue) {
    if (!isUsable()) {
      return defaultValue;
    }
    CustomData data = stack.get(DataComponents.CUSTOM_DATA);
    if (data == null) {
      return defaultValue;
    }
    CompoundTag root = data.copyTag();
    if (!root.contains(rootKey, Tag.TAG_COMPOUND)) {
      return defaultValue;
    }
    CompoundTag state = root.getCompound(rootKey);
    if (!state.contains(key, Tag.TAG_LONG)) {
      return defaultValue;
    }
    return state.getLong(key);
  }

  public boolean getBoolean(String key, boolean defaultValue) {
    if (!isUsable()) {
      return defaultValue;
    }
    CustomData data = stack.get(DataComponents.CUSTOM_DATA);
    if (data == null) {
      return defaultValue;
    }
    CompoundTag root = data.copyTag();
    if (!root.contains(rootKey, Tag.TAG_COMPOUND)) {
      return defaultValue;
    }
    CompoundTag state = root.getCompound(rootKey);
    if (!state.contains(key, Tag.TAG_BYTE)) {
      return defaultValue;
    }
    return state.getBoolean(key);
  }

  public Change<Integer> setInt(String key, int value) {
    return setInt(key, value, IDENTITY_INT, 0);
  }

  public Change<Integer> setInt(String key, int value, IntUnaryOperator clamp, int defaultValue) {
    if (!isUsable()) {
      return new Change<>(defaultValue, defaultValue);
    }
    int clamped = (clamp == null ? IDENTITY_INT : clamp).applyAsInt(value);
    int previous = getInt(key, defaultValue);
    NBTWriter.updateCustomData(
        stack,
        tag -> {
          CompoundTag state =
              tag.contains(rootKey, Tag.TAG_COMPOUND)
                  ? tag.getCompound(rootKey)
                  : new CompoundTag();
          state.putInt(key, clamped);
          tag.put(rootKey, state);
        });
    return new Change<>(previous, clamped);
  }

  public Change<Double> setDouble(String key, double value) {
    return setDouble(key, value, IDENTITY_DOUBLE, 0.0);
  }

  public Change<Double> setDouble(
      String key, double value, DoubleUnaryOperator clamp, double defaultValue) {
    if (!isUsable()) {
      return new Change<>(defaultValue, defaultValue);
    }
    double clamped = (clamp == null ? IDENTITY_DOUBLE : clamp).applyAsDouble(value);
    double previous = getDouble(key, defaultValue);
    NBTWriter.updateCustomData(
        stack,
        tag -> {
          CompoundTag state =
              tag.contains(rootKey, Tag.TAG_COMPOUND)
                  ? tag.getCompound(rootKey)
                  : new CompoundTag();
          state.putDouble(key, clamped);
          tag.put(rootKey, state);
        });
    return new Change<>(previous, clamped);
  }

  public Change<Long> setLong(String key, long value) {
    return setLong(key, value, IDENTITY_LONG, 0L);
  }

  public Change<Long> setLong(String key, long value, LongUnaryOperator clamp, long defaultValue) {
    if (!isUsable()) {
      return new Change<>(defaultValue, defaultValue);
    }
    long clamped = (clamp == null ? IDENTITY_LONG : clamp).applyAsLong(value);
    long previous = getLong(key, defaultValue);
    NBTWriter.updateCustomData(
        stack,
        tag -> {
          CompoundTag state =
              tag.contains(rootKey, Tag.TAG_COMPOUND)
                  ? tag.getCompound(rootKey)
                  : new CompoundTag();
          state.putLong(key, clamped);
          tag.put(rootKey, state);
        });
    return new Change<>(previous, clamped);
  }

  public Change<Boolean> setBoolean(String key, boolean value) {
    return setBoolean(key, value, false);
  }

  public Change<Boolean> setBoolean(String key, boolean value, boolean defaultValue) {
    if (!isUsable()) {
      return new Change<>(defaultValue, defaultValue);
    }
    boolean previous = getBoolean(key, defaultValue);
    NBTWriter.updateCustomData(
        stack,
        tag -> {
          CompoundTag state =
              tag.contains(rootKey, Tag.TAG_COMPOUND)
                  ? tag.getCompound(rootKey)
                  : new CompoundTag();
          state.putBoolean(key, value);
          tag.put(rootKey, state);
        });
    return new Change<>(previous, value);
  }

  public ListTag getList(String key, int type) {
    if (!isUsable()) {
      return new ListTag();
    }
    CustomData data = stack.get(DataComponents.CUSTOM_DATA);
    if (data == null) {
      return new ListTag();
    }
    CompoundTag root = data.copyTag();
    if (!root.contains(rootKey, Tag.TAG_COMPOUND)) {
      return new ListTag();
    }
    CompoundTag state = root.getCompound(rootKey);
    if (!state.contains(key, Tag.TAG_LIST)) {
      return new ListTag();
    }
    return state.getList(key, type);
  }

  public void setList(String key, ListTag value) {
    if (!isUsable()) {
      return;
    }
    NBTWriter.updateCustomData(
        stack,
        tag -> {
          CompoundTag state =
              tag.contains(rootKey, Tag.TAG_COMPOUND)
                  ? tag.getCompound(rootKey)
                  : new CompoundTag();
          state.put(key, value);
          tag.put(rootKey, state);
        });
  }

  private boolean isUsable() {
    return stack != null && !stack.isEmpty() && rootKey != null && !rootKey.isEmpty();
  }

  /** Represents a single state mutation, exposing whether the value actually changed. */
  public record Change<T>(T previous, T current) {
    public boolean changed() {
      return !Objects.equals(previous, current);
    }
  }

  /**
   * Checks if the organ's state contains the given key.
   *
   * @param key The key to check for.
   * @return {@code true} if the key exists, {@code false} otherwise.
   */
  public boolean contains(String key) {
    if (!isUsable()) {
      return false;
    }
    CustomData data = stack.get(DataComponents.CUSTOM_DATA);
    if (data == null) {
      return false;
    }
    CompoundTag root = data.copyTag();
    if (!root.contains(rootKey, Tag.TAG_COMPOUND)) {
      return false;
    }
    CompoundTag state = root.getCompound(rootKey);
    return state.contains(key);
  }

  /**
   * Removes the given key from the organ's state.
   *
   * @param key The key to remove.
   */
  public void remove(String key) {
    if (!isUsable()) {
      return;
    }
    NBTWriter.updateCustomData(
        stack,
        tag -> {
          if (tag.contains(rootKey, Tag.TAG_COMPOUND)) {
            CompoundTag state = tag.getCompound(rootKey);
            state.remove(key);
            if (state.isEmpty()) {
              tag.remove(rootKey);
            } else {
              tag.put(rootKey, state);
            }
          }
        });
  }
}
