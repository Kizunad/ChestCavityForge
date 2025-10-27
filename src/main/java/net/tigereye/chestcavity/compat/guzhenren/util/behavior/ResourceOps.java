package net.tigereye.chestcavity.compat.guzhenren.util.behavior;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge.ResourceHandle;
import net.tigereye.chestcavity.guzhenren.util.GuzhenrenResourceCostHelper;
import net.tigereye.chestcavity.guzhenren.util.ZhenyuanBaseCosts;
import net.tigereye.chestcavity.guzhenren.util.ZhenyuanBaseCosts.Tier;

/** Thin wrappers around Guzhenren resource helpers to centralize common patterns. */
public final class ResourceOps {

  private ResourceOps() {}

  /** Attempts to open the Guzhenren {@link ResourceHandle} for the given living entity. */
  public static Optional<ResourceHandle> openHandle(LivingEntity entity) {
    if (entity == null) {
      return Optional.empty();
    }
    return GuzhenrenResourceBridge.open(entity);
  }

  /** Attempts to open the Guzhenren {@link ResourceHandle} for the given player. */
  public static Optional<ResourceHandle> openHandle(Player player) {
    if (player == null) {
      return Optional.empty();
    }
    return GuzhenrenResourceBridge.open(player);
  }

  /**
   * Executes the provided mapper when a {@link ResourceHandle} is available for the given entity.
   *
   * @return optional mapping result, empty when handle missing or mapper is null
   */
  public static <T> Optional<T> mapHandle(
      LivingEntity entity, Function<ResourceHandle, T> mapper) {
    if (mapper == null) {
      return Optional.empty();
    }
    return openHandle(entity).map(mapper);
  }

  /**
   * Performs an action with the entity's {@link ResourceHandle} if available.
   *
   * @return true if the consumer ran, false otherwise
   */
  public static boolean withHandle(LivingEntity entity, Consumer<ResourceHandle> consumer) {
    if (consumer == null) {
      return false;
    }
    Optional<ResourceHandle> handleOpt = openHandle(entity);
    handleOpt.ifPresent(consumer);
    return handleOpt.isPresent();
  }

  /** Consume zhenyuan/jingli strictly from a real player; no HP fallback. */
  public static GuzhenrenResourceCostHelper.ConsumptionResult consumeStrict(
      Player player, double zhenyuan, double jingli) {
    if (player == null) throw new IllegalArgumentException("player is null");
    return GuzhenrenResourceCostHelper.consumeStrict(player, zhenyuan, jingli);
  }

  /** Consume zhenyuan/jingli from any living entity; falls back to HP when not a player. */
  public static GuzhenrenResourceCostHelper.ConsumptionResult consumeWithFallback(
      LivingEntity entity, double zhenyuan, double jingli) {
    if (entity == null) throw new IllegalArgumentException("entity is null");
    return GuzhenrenResourceCostHelper.consumeWithFallback(entity, zhenyuan, jingli);
  }

  /** Consume zhenyuan/jingli strictly from any entity (players via attachment; no HP fallback). */
  public static GuzhenrenResourceCostHelper.ConsumptionResult consumeStrict(
      LivingEntity entity, double zhenyuan, double jingli) {
    if (entity == null) throw new IllegalArgumentException("entity is null");
    return GuzhenrenResourceCostHelper.consumeStrict(entity, zhenyuan, jingli);
  }

  /** Consume hunpo strictly (no HP fallback). */
  public static GuzhenrenResourceCostHelper.ConsumptionResult consumeHunpoStrict(
      LivingEntity entity, double hunpo) {
    if (entity == null) throw new IllegalArgumentException("entity is null");
    return GuzhenrenResourceCostHelper.consumeHunpo(entity, hunpo, false);
  }

  /** Consume hunpo with HP fallback for non-players. */
  public static GuzhenrenResourceCostHelper.ConsumptionResult consumeHunpoWithFallback(
      LivingEntity entity, double hunpo) {
    if (entity == null) throw new IllegalArgumentException("entity is null");
    return GuzhenrenResourceCostHelper.consumeHunpo(entity, hunpo, true);
  }

  /** Refund a previous {@link GuzhenrenResourceCostHelper} consumption back to the player. */
  public static boolean refund(
      Player player, GuzhenrenResourceCostHelper.ConsumptionResult result) {
    if (player == null || result == null) {
      return false;
    }
    return GuzhenrenResourceCostHelper.refund(player, result);
  }

  /** Drain health/absorption from an entity while respecting vanilla rollback semantics. */
  public static boolean drainHealth(
      LivingEntity entity, float amount, float minimumReserve, DamageSource source) {
    return GuzhenrenResourceCostHelper.drainHealth(entity, amount, minimumReserve, source);
  }

  /** Convenience overload that uses the entity's generic damage source. */
  public static boolean drainHealth(LivingEntity entity, float amount, float minimumReserve) {
    DamageSource source = entity == null ? null : entity.damageSources().generic();
    return drainHealth(entity, amount, minimumReserve, source);
  }

  /** Convenience overload that omits the minimum reserve. */
  public static boolean drainHealth(LivingEntity entity, float amount, DamageSource source) {
    return drainHealth(entity, amount, 0.0f, source);
  }

  /** Convenience overload that omits both reserve and custom source. */
  public static boolean drainHealth(LivingEntity entity, float amount) {
    return drainHealth(entity, amount, 0.0f);
  }

  public static OptionalDouble tryAdjustJingli(
      ResourceHandle handle, double amount, boolean clamp) {
    if (handle == null || amount == 0.0) {
      return OptionalDouble.empty();
    }
    return handle.adjustJingli(amount, clamp);
  }

  public static OptionalDouble tryAdjustJingli(ResourceHandle handle, double amount) {
    return tryAdjustJingli(handle, amount, true);
  }

  public static OptionalDouble tryAdjustJingli(Player player, double amount, boolean clamp) {
    if (player == null || amount == 0.0) {
      return OptionalDouble.empty();
    }
    Optional<ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
    if (handleOpt.isEmpty()) {
      return OptionalDouble.empty();
    }
    return tryAdjustJingli(handleOpt.get(), amount, clamp);
  }

  public static OptionalDouble tryAdjustJingli(Player player, double amount) {
    return tryAdjustJingli(player, amount, true);
  }

  public static OptionalDouble tryAdjustJingli(LivingEntity entity, double amount, boolean clamp) {
    if (entity == null || amount == 0.0) {
      return OptionalDouble.empty();
    }
    Optional<ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(entity);
    if (handleOpt.isEmpty()) {
      return OptionalDouble.empty();
    }
    return tryAdjustJingli(handleOpt.get(), amount, clamp);
  }

  public static OptionalDouble tryAdjustJingli(LivingEntity entity, double amount) {
    return tryAdjustJingli(entity, amount, true);
  }

  /** Adjust player jingli without needing the return value. */
  public static void adjustJingli(Player player, double amount) {
    tryAdjustJingli(player, amount, true);
  }

  public static OptionalDouble tryReplenishScaledZhenyuan(
      ResourceHandle handle, double amount, boolean clampToMax) {
    if (handle == null || amount <= 0.0) {
      return OptionalDouble.empty();
    }
    return handle.replenishScaledZhenyuan(amount, clampToMax);
  }

  public static OptionalDouble tryReplenishScaledZhenyuan(ResourceHandle handle, double amount) {
    return tryReplenishScaledZhenyuan(handle, amount, true);
  }

  public static OptionalDouble tryReplenishScaledZhenyuan(
      Player player, double amount, boolean clampToMax) {
    if (player == null || amount <= 0.0) {
      return OptionalDouble.empty();
    }
    Optional<ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
    if (handleOpt.isEmpty()) {
      return OptionalDouble.empty();
    }
    return tryReplenishScaledZhenyuan(handleOpt.get(), amount, clampToMax);
  }

  public static OptionalDouble tryReplenishScaledZhenyuan(Player player, double amount) {
    return tryReplenishScaledZhenyuan(player, amount, true);
  }

  public static OptionalDouble tryReplenishScaledZhenyuan(
      LivingEntity entity, double amount, boolean clampToMax) {
    if (entity == null || amount <= 0.0) {
      return OptionalDouble.empty();
    }
    Optional<ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(entity);
    if (handleOpt.isEmpty()) {
      return OptionalDouble.empty();
    }
    return tryReplenishScaledZhenyuan(handleOpt.get(), amount, clampToMax);
  }

  public static OptionalDouble tryReplenishScaledZhenyuan(LivingEntity entity, double amount) {
    return tryReplenishScaledZhenyuan(entity, amount, true);
  }

  /** Convenience wrapper retaining legacy behaviour. */
  public static void replenishScaledZhenyuan(Player player, double amount) {
    tryReplenishScaledZhenyuan(player, amount, true);
  }

  public static OptionalDouble tryConsumeScaledZhenyuan(ResourceHandle handle, double baseCost) {
    if (handle == null || baseCost <= 0.0) {
      return OptionalDouble.empty();
    }
    return handle.consumeScaledZhenyuan(baseCost);
  }

  /**
   * 按“设计转/阶段 + 单位用量（units）”消费真元：
   * baseCost = ZhenyuanBaseCosts.baseForUnits(designZhuanshu, designJieduan, units)
   * → handle.consumeScaledZhenyuan(baseCost)。
   */
  public static OptionalDouble tryConsumeScaledZhenyuan(
      ResourceHandle handle, int designZhuanshu, int designJieduan, double units) {
    if (handle == null) {
      return OptionalDouble.empty();
    }
    if (!(units > 0.0) || !Double.isFinite(units)) {
      return OptionalDouble.empty();
    }
    double base = ZhenyuanBaseCosts.baseForUnits(designZhuanshu, designJieduan, units);
    return tryConsumeScaledZhenyuan(handle, base);
  }

  public static OptionalDouble tryConsumeScaledZhenyuan(Player player, double baseCost) {
    if (player == null || baseCost <= 0.0) {
      return OptionalDouble.empty();
    }
    Optional<ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
    if (handleOpt.isEmpty()) {
      return OptionalDouble.empty();
    }
    return tryConsumeScaledZhenyuan(handleOpt.get(), baseCost);
  }

  /** see {@link #tryConsumeScaledZhenyuan(ResourceHandle, int, int, double)} */
  public static OptionalDouble tryConsumeScaledZhenyuan(
      Player player, int designZhuanshu, int designJieduan, double units) {
    if (player == null) {
      return OptionalDouble.empty();
    }
    Optional<ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
    if (handleOpt.isEmpty()) {
      return OptionalDouble.empty();
    }
    return tryConsumeScaledZhenyuan(handleOpt.get(), designZhuanshu, designJieduan, units);
  }

  public static OptionalDouble tryConsumeScaledZhenyuan(LivingEntity entity, double baseCost) {
    if (entity == null || baseCost <= 0.0) {
      return OptionalDouble.empty();
    }
    Optional<ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(entity);
    if (handleOpt.isEmpty()) {
      return OptionalDouble.empty();
    }
    return tryConsumeScaledZhenyuan(handleOpt.get(), baseCost);
  }

  /** see {@link #tryConsumeScaledZhenyuan(ResourceHandle, int, int, double)} */
  public static OptionalDouble tryConsumeScaledZhenyuan(
      LivingEntity entity, int designZhuanshu, int designJieduan, double units) {
    if (entity == null) {
      return OptionalDouble.empty();
    }
    Optional<ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(entity);
    if (handleOpt.isEmpty()) {
      return OptionalDouble.empty();
    }
    return tryConsumeScaledZhenyuan(handleOpt.get(), designZhuanshu, designJieduan, units);
  }

  /** 按推荐分级（Tier）消费真元。 */
  public static OptionalDouble tryConsumeTieredZhenyuan(
      ResourceHandle handle, int designZhuanshu, int designJieduan, Tier tier) {
    if (handle == null || tier == null) {
      return OptionalDouble.empty();
    }
    double base = ZhenyuanBaseCosts.baseForTier(designZhuanshu, designJieduan, tier);
    return tryConsumeScaledZhenyuan(handle, base);
  }

  /** see {@link #tryConsumeTieredZhenyuan(ResourceHandle, int, int, Tier)} */
  public static OptionalDouble tryConsumeTieredZhenyuan(
      Player player, int designZhuanshu, int designJieduan, Tier tier) {
    if (player == null || tier == null) {
      return OptionalDouble.empty();
    }
    Optional<ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
    if (handleOpt.isEmpty()) {
      return OptionalDouble.empty();
    }
    return tryConsumeTieredZhenyuan(handleOpt.get(), designZhuanshu, designJieduan, tier);
  }

  /** see {@link #tryConsumeTieredZhenyuan(ResourceHandle, int, int, Tier)} */
  public static OptionalDouble tryConsumeTieredZhenyuan(
      LivingEntity entity, int designZhuanshu, int designJieduan, Tier tier) {
    if (entity == null || tier == null) {
      return OptionalDouble.empty();
    }
    Optional<ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(entity);
    if (handleOpt.isEmpty()) {
      return OptionalDouble.empty();
    }
    return tryConsumeTieredZhenyuan(handleOpt.get(), designZhuanshu, designJieduan, tier);
  }

  public static OptionalDouble tryConsumeScaledJingli(ResourceHandle handle, double baseCost) {
    if (handle == null || baseCost <= 0.0) {
      return OptionalDouble.empty();
    }
    return handle.consumeScaledJingli(baseCost);
  }

  public static OptionalDouble tryConsumeScaledJingli(Player player, double baseCost) {
    if (player == null || baseCost <= 0.0) {
      return OptionalDouble.empty();
    }
    Optional<ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
    if (handleOpt.isEmpty()) {
      return OptionalDouble.empty();
    }
    return tryConsumeScaledJingli(handleOpt.get(), baseCost);
  }

  public static OptionalDouble tryConsumeScaledJingli(LivingEntity entity, double baseCost) {
    if (entity == null || baseCost <= 0.0) {
      return OptionalDouble.empty();
    }
    Optional<ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(entity);
    if (handleOpt.isEmpty()) {
      return OptionalDouble.empty();
    }
    return tryConsumeScaledJingli(handleOpt.get(), baseCost);
  }

  public static OptionalDouble tryAdjustZhenyuan(
      ResourceHandle handle, double amount, boolean clampToMax) {
    if (handle == null || amount == 0.0) {
      return OptionalDouble.empty();
    }
    return handle.adjustZhenyuan(amount, clampToMax);
  }

  public static OptionalDouble tryAdjustZhenyuan(Player player, double amount, boolean clampToMax) {
    if (player == null || amount == 0.0) {
      return OptionalDouble.empty();
    }
    Optional<ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
    if (handleOpt.isEmpty()) {
      return OptionalDouble.empty();
    }
    return tryAdjustZhenyuan(handleOpt.get(), amount, clampToMax);
  }

  public static OptionalDouble trySetJingli(ResourceHandle handle, double value) {
    if (handle == null) {
      return OptionalDouble.empty();
    }
    return handle.setJingli(value);
  }

  public static OptionalDouble trySetJingli(Player player, double value) {
    if (player == null) {
      return OptionalDouble.empty();
    }
    Optional<ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
    if (handleOpt.isEmpty()) {
      return OptionalDouble.empty();
    }
    return trySetJingli(handleOpt.get(), value);
  }

  public static OptionalDouble tryAdjustDouble(
      ResourceHandle handle, String field, double amount, boolean clampToMax, String maxField) {
    if (handle == null || field == null) {
      return OptionalDouble.empty();
    }
    return handle.adjustDouble(field, amount, clampToMax, maxField);
  }

  public static OptionalDouble tryAdjustDouble(
      Player player, String field, double amount, boolean clampToMax, String maxField) {
    if (player == null || field == null) {
      return OptionalDouble.empty();
    }
    Optional<ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
    if (handleOpt.isEmpty()) {
      return OptionalDouble.empty();
    }
    return tryAdjustDouble(handleOpt.get(), field, amount, clampToMax, maxField);
  }
}
