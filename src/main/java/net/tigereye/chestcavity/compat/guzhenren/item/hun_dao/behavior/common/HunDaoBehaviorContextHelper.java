package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.behavior.common;

import com.mojang.logging.LogUtils;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.runtime.HunDaoRuntimeContext;
import net.tigereye.chestcavity.interfaces.ChestCavityEntity;
import org.slf4j.Logger;

/**
 * 魂道行为层共享工具类。
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
   * 从玩家实体获取魂道运行时上下文。
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
   * 从玩家实体安全地获取魂道运行时上下文。
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
   * 从实体获取胸腔实例。
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
   * 检查实体是否为玩家且在服务端。
   *
   * @param entity 实体
   * @return true 如果是服务端玩家
   */
  public static boolean isServerPlayer(LivingEntity entity) {
    return entity instanceof Player && !entity.level().isClientSide();
  }

  /**
   * 检查实体是否在客户端。
   *
   * @param entity 实体
   * @return true 如果在客户端
   */
  public static boolean isClientSide(LivingEntity entity) {
    return entity != null && entity.level().isClientSide();
  }

  /**
   * 格式化浮点数为两位小数（日志用）。
   *
   * @param value 数值
   * @return 格式化的字符串
   */
  public static String format(double value) {
    return String.format(Locale.ROOT, "%.2f", value);
  }

  /**
   * 格式化浮点数为指定位数小数（日志用）。
   *
   * @param value 数值
   * @param precision 小数位数
   * @return 格式化的字符串
   */
  public static String format(double value, int precision) {
    return String.format(Locale.ROOT, "%." + precision + "f", value);
  }

  /**
   * 获取玩家描述（记日志用）。
   *
   * @param player 玩家
   * @return 玩家名称或 "&lt;unknown&gt;"
   */
  public static String describePlayer(Player player) {
    return player == null ? "<unknown>" : player.getScoreboardName();
  }

  /**
   * 获取实体描述（记日志用）。
   *
   * @param entity 实体
   * @return 实体名称
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
   * 获取统一的日志前缀。
   *
   * @return 日志前缀
   */
  public static String logPrefix() {
    return LOG_PREFIX;
  }

  /**
   * 获取带子模块名称的日志前缀。
   *
   * @param module 子模块名称（如 "xiao_hun_gu", "da_hun_gu"）
   * @return 带子模块的日志前缀
   */
  public static String logPrefix(String module) {
    return LOG_PREFIX + "[" + module + "]";
  }

  /**
   * 调试日志：记录玩家操作。
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
   * 调试日志：简单消息。
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
   * 警告日志。
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
   * 错误日志。
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
   * 检查数值是否接近零（用于浮点数比较）。
   *
   * @param value 数值
   * @param epsilon 误差范围
   * @return true 如果数值的绝对值小于 epsilon
   */
  public static boolean isNearZero(double value, double epsilon) {
    return Math.abs(value) < epsilon;
  }

  /**
   * 检查数值是否接近零（使用默认 epsilon 1.0E-4）。
   *
   * @param value 数值
   * @return true 如果数值接近零
   */
  public static boolean isNearZero(double value) {
    return isNearZero(value, 1.0E-4);
  }

  /**
   * 安全的数值最大值（忽略 NaN）。
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
   * 安全的数值最小值（忽略 NaN）。
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
   * 将数值限制在指定范围内。
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
