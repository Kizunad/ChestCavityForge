package net.tigereye.chestcavity.compat.guzhenren.util.behavior;

import java.util.OptionalDouble;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge.ResourceHandle;

/**
 * Utility operations for DaoHen (道痕) resources.
 * Provides convenient methods for reading and writing dao marks.
 */
public final class DaoHenResourceOps {

  private DaoHenResourceOps() {}

  /**
   * Get the current amount of a dao hen resource.
   * @param handle Resource handle
   * @param daoHenKey Key like "daohen_jindao", "daohen_bianhuadao", etc.
   * @return Current value, or 0.0 if not found
   */
  public static double get(ResourceHandle handle, String daoHenKey) {
    if (handle == null || daoHenKey == null || daoHenKey.isEmpty()) {
      return 0.0;
    }
    return handle.read(daoHenKey).orElse(0.0);
  }

  /**
   * Set a dao hen resource to a specific value.
   * @param handle Resource handle
   * @param daoHenKey Key like "daohen_jindao", "daohen_bianhuadao", etc.
   * @param value New value
   * @return true if successful
   */
  public static boolean set(ResourceHandle handle, String daoHenKey, double value) {
    if (handle == null || daoHenKey == null || daoHenKey.isEmpty()) {
      return false;
    }
    if (!Double.isFinite(value) || value < 0.0) {
      return false;
    }
    OptionalDouble result = handle.writeDouble(daoHenKey, value);
    return result.isPresent();
  }

  /**
   * Adjust a dao hen resource by delta amount.
   * @param handle Resource handle
   * @param daoHenKey Key like "daohen_jindao", "daohen_bianhuadao", etc.
   * @param delta Amount to add (can be negative)
   * @return New value after adjustment, or empty if failed
   */
  public static OptionalDouble adjust(ResourceHandle handle, String daoHenKey, double delta) {
    if (handle == null || daoHenKey == null || daoHenKey.isEmpty()) {
      return OptionalDouble.empty();
    }
    if (!Double.isFinite(delta)) {
      return OptionalDouble.empty();
    }

    double current = get(handle, daoHenKey);
    double newValue = Math.max(0.0, current + delta);

    if (set(handle, daoHenKey, newValue)) {
      return OptionalDouble.of(newValue);
    }
    return OptionalDouble.empty();
  }

  /**
   * Consume (subtract) dao hen resource. Fails if insufficient.
   * @param handle Resource handle
   * @param daoHenKey Key like "daohen_jindao", "daohen_bianhuadao", etc.
   * @param amount Amount to consume
   * @return true if successful (had enough to consume)
   */
  public static boolean consume(ResourceHandle handle, String daoHenKey, double amount) {
    if (amount < 0.0) {
      return false;
    }
    double current = get(handle, daoHenKey);
    if (current < amount) {
      return false;
    }
    return adjust(handle, daoHenKey, -amount).isPresent();
  }

  /**
   * Add dao hen resource.
   * @param handle Resource handle
   * @param daoHenKey Key like "daohen_jindao", "daohen_bianhuadao", etc.
   * @param amount Amount to add
   * @return true if successful
   */
  public static boolean add(ResourceHandle handle, String daoHenKey, double amount) {
    if (amount < 0.0) {
      return false;
    }
    return adjust(handle, daoHenKey, amount).isPresent();
  }

  /**
   * Check if player has at least the specified amount of dao hen.
   * @param handle Resource handle
   * @param daoHenKey Key like "daohen_jindao", "daohen_bianhuadao", etc.
   * @param amount Required amount
   * @return true if has enough
   */
  public static boolean has(ResourceHandle handle, String daoHenKey, double amount) {
    return get(handle, daoHenKey) >= amount;
  }
}
