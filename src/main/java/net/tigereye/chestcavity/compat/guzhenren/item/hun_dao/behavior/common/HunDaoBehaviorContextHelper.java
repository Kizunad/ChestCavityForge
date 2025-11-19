package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.behavior.common;

import java.util.Locale;
import java.util.Optional;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.runtime.HunDaoRuntimeContext;
import net.tigereye.chestcavity.interfaces.ChestCavityEntity;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/**
 * Shared utilities for Hun Dao behavior modules.
 *
 * <p>提供统一的：
 *
 * <ul>
 *   <li>运行时上下文拉取（{@link HunDaoRuntimeContext}）
 *   <li>日志前缀与格式化
 *   <li>校验工具（玩家、胸腔、服务端检查）
 * </ul>
 *
 * <p>Phase 3 重构后，所有魂道行为类应通过此 helper 访问上下文，而非直接引用 {@code HunDaoOpsAdapter.INSTANCE}。
 */
public final class HunDaoBehaviorContextHelper {

  private static final Logger LOGGER = LogUtils.getLogger();
  private static final String LOG_PREFIX = "[compat/guzhenren][hun_dao][behavior]";

  private HunDaoBehaviorContextHelper() {}

  /**
   * Retrieves the Hun Dao runtime context for the given player.
   *
   * @param player 玩家实体
   * @return 魂道运行时上下文
   * @throws IllegalArgumentException 如果 player 为 null
   */
  public static HunDaoRuntimeContext getContext(Player player) {
    if (player == null) {
      throw new IllegalArgumentException("Player cannot be null");
    }
    return HunDaoRuntimeContext.get(player);
  }

  /**
   * Retrieves the Hun Dao runtime context with Optional safety.
   *
   * @param player 玩家实体
   * @return Optional 包装的魂道运行时上下文
   */
  public static Optional<HunDaoRuntimeContext> getContextSafe(Player player) {
    if (player == null) {
      return Optional.empty();
    }
    try {
      return Optional.of(HunDaoRuntimeContext.get(player));
    } catch (Exception e) {
      LOGGER.warn(
          "{} failed to get runtime context for player {}",
          LOG_PREFIX,
          player.getScoreboardName(),
          e);
      return Optional.empty();
    }
  }

  /**
   * Resolves the chest cavity instance from a living entity.
   *
   * @param entity 实体
   * @return Optional 包装的胸腔实例
   */
  public static Optional<ChestCavityInstance> getChestCavity(LivingEntity entity) {
    if (entity == null) {
      return Optional.empty();
    }
    return ChestCavityEntity.of(entity).map(ChestCavityEntity::getChestCavityInstance);
  }

  /**
   * Checks whether the entity is a server-side player.
   *
   * @param entity 实体
   * @return true 如果是服务端玩家
   */
  public static boolean isServerPlayer(LivingEntity entity) {
    return entity instanceof Player && !entity.level().isClientSide();
  }

  /**
   * Checks whether the entity currently exists on the client.
   *
   * @param entity 实体
   * @return true 如果在客户端
   */
  public static boolean isClientSide(LivingEntity entity) {
    return entity != null && entity.level().isClientSide();
  }

  /**
   * Formats a double value to two decimals for logging.
   *
   * @param value 数值
   * @return 格式化的字符串
   */
  public static String format(double value) {
    return String.format(Locale.ROOT, "%.2f", value);
  }

  /**
   * Formats a double value with the specified precision.
   *
   * @param value 数值
   * @param precision 小数位数
   * @return 格式化的字符串
   */
  public static String format(double value, int precision) {
    return String.format(Locale.ROOT, "%." + precision + "f", value);
  }

  /**
   * Provides a log-friendly description of the player.
   *
   * @param player 玩家
   * @return 玩家名称或 {@code "<unknown>"}
   */
  public static String describePlayer(Player player) {
    return player == null ? "<unknown>" : player.getScoreboardName();
  }

  /**
   * Provides a log-friendly description of the entity.
   *
   * @param entity 实体
   * @return 实体名称或 {@code "<null>"}
   */
  public static String describeEntity(LivingEntity entity) {
    if (entity == null) {
      return "<null>";
    }
    if (entity instanceof Player player) {
      return describePlayer(player);
    }
    return entity.getName().getString() + " (" + entity.getType().toShortString() + ")";
  }

  /**
   * Returns the shared log prefix for Hun Dao behaviors.
   *
   * @return 日志前缀
   */
  public static String logPrefix() {
    return LOG_PREFIX;
  }

  /**
   * Returns the log prefix with a sub-module suffix.
   *
   * @param module 子模块名称（如 "xiao_hun_gu", "da_hun_gu"）
   * @return 带子模块的日志前缀
   */
  public static String logPrefix(String module) {
    return LOG_PREFIX + "[" + module + "]";
  }

  /**
   * Logs a debug message that references a player.
   *
   * @param module 模块名称
   * @param player 玩家
   * @param message 消息模板
   * @param args 参数
   */
  public static void debugLog(String module, Player player, String message, Object... args) {
    if (LOGGER.isDebugEnabled()) {
      String prefix = logPrefix(module);
      String playerDesc = describePlayer(player);
      Object[] newArgs = new Object[args.length + 2];
      newArgs[0] = prefix;
      newArgs[1] = playerDesc;
      System.arraycopy(args, 0, newArgs, 2, args.length);
      LOGGER.debug("{} player={} " + message, newArgs);
    }
  }

  /**
   * Logs a debug message without player context.
   *
   * @param module 模块名称
   * @param message 消息模板
   * @param args 参数
   */
  public static void debugLog(String module, String message, Object... args) {
    if (LOGGER.isDebugEnabled()) {
      String prefix = logPrefix(module);
      Object[] newArgs = new Object[args.length + 1];
      newArgs[0] = prefix;
      System.arraycopy(args, 0, newArgs, 1, args.length);
      LOGGER.debug("{} " + message, newArgs);
    }
  }

  /**
   * Logs a warning with the shared prefix.
   *
   * @param module 模块名称
   * @param message 消息模板
   * @param args 参数
   */
  public static void warnLog(String module, String message, Object... args) {
    String prefix = logPrefix(module);
    Object[] newArgs = new Object[args.length + 1];
    newArgs[0] = prefix;
    System.arraycopy(args, 0, newArgs, 1, args.length);
    LOGGER.warn("{} " + message, newArgs);
  }

  /**
   * Logs an error message with throwable support.
   *
   * @param module 模块名称
   * @param message 消息模板
   * @param throwable 异常
   * @param args 参数
   */
  public static void errorLog(String module, String message, Throwable throwable, Object... args) {
    String prefix = logPrefix(module);
    Object[] newArgs = new Object[args.length + 1];
    newArgs[0] = prefix;
    System.arraycopy(args, 0, newArgs, 1, args.length);
    LOGGER.error("{} " + message, newArgs, throwable);
  }

  /**
   * Checks whether the value is near zero.
   *
   * @param value 数值
   * @param epsilon 误差范围
   * @return true 如果数值的绝对值小于 epsilon
   */
  public static boolean isNearZero(double value, double epsilon) {
    return Math.abs(value) < epsilon;
  }

  /**
   * Checks whether the value is near zero using the default epsilon.
   *
   * @param value 数值
   * @return true 如果数值接近零
   */
  public static boolean isNearZero(double value) {
    return isNearZero(value, 1.0E-4);
  }

  /**
   * Returns the max value while ignoring NaN inputs.
   *
   * @param a 第一个数值
   * @param b 第二个数值
   * @return 两者中的较大值，如果有 NaN 则返回另一个值
   */
  public static double max(double a, double b) {
    if (Double.isNaN(a)) {
      return b;
    }
    if (Double.isNaN(b)) {
      return a;
    }
    return Math.max(a, b);
  }

  /**
   * Returns the min value while ignoring NaN inputs.
   *
   * @param a 第一个数值
   * @param b 第二个数值
   * @return 两者中的较小值，如果有 NaN 则返回另一个值
   */
  public static double min(double a, double b) {
    if (Double.isNaN(a)) {
      return b;
    }
    if (Double.isNaN(b)) {
      return a;
    }
    return Math.min(a, b);
  }

  /**
   * Clamps the value within the provided range.
   *
   * @param value 数值
   * @param min 最小值
   * @param max 最大值
   * @return 限制后的数值
   */
  public static double clamp(double value, double min, double max) {
    return Math.max(min, Math.min(max, value));
  }
}
