package net.tigereye.chestcavity.compat.guzhenren.util.behavior;

import java.util.function.DoubleUnaryOperator;
import java.util.function.IntUnaryOperator;
import java.util.function.LongUnaryOperator;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.util.NetworkUtil;

/** Convenience setters for OrganState that auto-sync slot updates when changed. */
public final class OrganStateOps {

  private OrganStateOps() {}

  public static OrganState.Change<Integer> setIntSync(
      ChestCavityInstance cc,
      ItemStack stack,
      String rootKey,
      String key,
      int value,
      IntUnaryOperator clamp,
      int defaultValue) {
    OrganState state = OrganState.of(stack, rootKey);
    OrganState.Change<Integer> change = state.setInt(key, value, clamp, defaultValue);
    if (change.changed()) {
      NetworkUtil.sendOrganSlotUpdate(cc, stack);
    }
    return change;
  }

  public static OrganState.Change<Long> setLongSync(
      ChestCavityInstance cc,
      ItemStack stack,
      String rootKey,
      String key,
      long value,
      LongUnaryOperator clamp,
      long defaultValue) {
    OrganState state = OrganState.of(stack, rootKey);
    OrganState.Change<Long> change = state.setLong(key, value, clamp, defaultValue);
    if (change.changed()) {
      NetworkUtil.sendOrganSlotUpdate(cc, stack);
    }
    return change;
  }

  public static OrganState.Change<Double> setDoubleSync(
      ChestCavityInstance cc,
      ItemStack stack,
      String rootKey,
      String key,
      double value,
      DoubleUnaryOperator clamp,
      double defaultValue) {
    OrganState state = OrganState.of(stack, rootKey);
    OrganState.Change<Double> change = state.setDouble(key, value, clamp, defaultValue);
    if (change.changed()) {
      NetworkUtil.sendOrganSlotUpdate(cc, stack);
    }
    return change;
  }

  public static OrganState.Change<Boolean> setBooleanSync(
      ChestCavityInstance cc,
      ItemStack stack,
      String rootKey,
      String key,
      boolean value,
      boolean defaultValue) {
    OrganState state = OrganState.of(stack, rootKey);
    OrganState.Change<Boolean> change = state.setBoolean(key, value, defaultValue);
    if (change.changed()) {
      NetworkUtil.sendOrganSlotUpdate(cc, stack);
    }
    return change;
  }

  public static OrganState.Change<Integer> setInt(
      OrganState state,
      ChestCavityInstance cc,
      ItemStack stack,
      String key,
      int value,
      IntUnaryOperator clamp,
      int defaultValue) {
    if (state == null) {
      throw new IllegalArgumentException("state is null");
    }
    OrganState.Change<Integer> change = state.setInt(key, value, clamp, defaultValue);
    if (change.changed()) {
      NetworkUtil.sendOrganSlotUpdate(cc, stack);
    }
    return change;
  }

  public static OrganState.Change<Long> setLong(
      OrganState state,
      ChestCavityInstance cc,
      ItemStack stack,
      String key,
      long value,
      LongUnaryOperator clamp,
      long defaultValue) {
    if (state == null) {
      throw new IllegalArgumentException("state is null");
    }
    OrganState.Change<Long> change = state.setLong(key, value, clamp, defaultValue);
    if (change.changed()) {
      NetworkUtil.sendOrganSlotUpdate(cc, stack);
    }
    return change;
  }

  public static OrganState.Change<Double> setDouble(
      OrganState state,
      ChestCavityInstance cc,
      ItemStack stack,
      String key,
      double value,
      DoubleUnaryOperator clamp,
      double defaultValue) {
    if (state == null) {
      throw new IllegalArgumentException("state is null");
    }
    OrganState.Change<Double> change = state.setDouble(key, value, clamp, defaultValue);
    if (change.changed()) {
      NetworkUtil.sendOrganSlotUpdate(cc, stack);
    }
    return change;
  }

  public static OrganState.Change<Boolean> setBoolean(
      OrganState state,
      ChestCavityInstance cc,
      ItemStack stack,
      String key,
      boolean value,
      boolean defaultValue) {
    if (state == null) {
      throw new IllegalArgumentException("state is null");
    }
    OrganState.Change<Boolean> change = state.setBoolean(key, value, defaultValue);
    if (change.changed()) {
      NetworkUtil.sendOrganSlotUpdate(cc, stack);
    }
    return change;
  }

  /**
   * Create a collector that aggregates {@link OrganState.Change} instances and emits a single
   * {@link NetworkUtil#sendOrganSlotUpdate(ChestCavityInstance, ItemStack)} call when committed.
   * Useful when multiple state keys are updated in the same tick and the caller only wants to
   * synchronise once after all mutations have been applied.
   */
  public static Collector collector(ChestCavityInstance cc, ItemStack stack) {
    return new Collector(cc, stack);
  }

  /**
   * Aggregates organ state mutations and defers the slot update until {@link #commit()} is invoked.
   */
  public static final class Collector {
    private final ChestCavityInstance cc;
    private final ItemStack stack;
    private boolean dirty;

    private Collector(ChestCavityInstance cc, ItemStack stack) {
      this.cc = cc;
      this.stack = stack;
    }

    /** Record a change and return it for further handling (logging, etc.). */
    public <T> OrganState.Change<T> record(OrganState.Change<T> change) {
      if (change != null && change.changed()) {
        dirty = true;
      }
      return change;
    }

    /** Record an arbitrary dirty flag. */
    public Collector record(boolean changed) {
      if (changed) {
        dirty = true;
      }
      return this;
    }

    /**
     * Record multiple changes at once. Declared final so it can be annotated with {@link
     * SafeVarargs}.
     */
    @SafeVarargs
    public final Collector recordAll(OrganState.Change<?>... changes) {
      if (changes == null) {
        return this;
      }
      for (OrganState.Change<?> change : changes) {
        record(change);
      }
      return this;
    }

    /** Returns whether any recorded change has mutated the state. */
    public boolean changed() {
      return dirty;
    }

    /**
     * Sends the organ slot update if at least one recorded change mutated the state. Returns {@code
     * true} when a packet was dispatched.
     */
    public boolean commit() {
      if (!dirty) {
        return false;
      }
      NetworkUtil.sendOrganSlotUpdate(cc, stack);
      dirty = false;
      return true;
    }
  }
}
