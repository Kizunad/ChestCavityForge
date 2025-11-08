package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.runtime;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.tuning.JianmaiTuning;

/**
 * 剑脉蛊 NBT 读写工具类。
 *
 * <p>负责管理玩家 PersistentData 下的剑脉效率（JME）相关数据。
 *
 * <p>命名空间：{@code chestcavity.jianmai}
 *
 * <p>数据结构：
 * <pre>
 * {
 *   "JME": 0.0,                  // 当前剑脉效率值
 *   "JME_Tick": 0,               // 上次刷新时间（游戏刻）
 *   "JME_Radius": 12.0,          // 扫描半径
 *   "JME_DistAlpha": 1.25,       // 距离权重指数
 *   "JME_MaxCap": 2.5,           // JME 上限
 *   "JME_Amp": {                 // 临时增幅数据
 *     "ampK": 0.10,              // 道痕增幅系数
 *     "mult": 1.0,               // 当前倍率
 *     "expireGameTime": 0,       // 过期时间（游戏刻）
 *     "graceTicks": 40           // 宽限期（tick）
 *   }
 * }
 * </pre>
 */
public final class JianmaiNBT {

  private JianmaiNBT() {}

  private static final String NAMESPACE = "chestcavity.jianmai";

  // ========== 顶层字段 ==========
  private static final String K_JME = "JME";
  private static final String K_JME_TICK = "JME_Tick";
  private static final String K_JME_RADIUS = "JME_Radius";
  private static final String K_JME_DIST_ALPHA = "JME_DistAlpha";
  private static final String K_JME_MAX_CAP = "JME_MaxCap";
  private static final String K_JME_AMP = "JME_Amp";

  // ========== JME_Amp 子字段 ==========
  private static final String K_AMP_K = "ampK";
  private static final String K_AMP_MULT_PASSIVE = "multPassive"; // 被动 JME 倍率
  private static final String K_AMP_MULT_ACTIVE = "multActive";   // 主动技能倍率
  private static final String K_AMP_EXPIRE = "expireGameTime";     // 被动过期时间
  private static final String K_AMP_ACTIVE_EXPIRE = "activeExpireGameTime"; // 主动过期时间
  private static final String K_AMP_GRACE = "graceTicks";

  // ========== 读取方法 ==========

  /**
   * 读取玩家的剑脉效率（JME）值。
   *
   * @param player 玩家
   * @return JME 值，默认 0.0
   */
  public static double readJME(ServerPlayer player) {
    CompoundTag data = player.getPersistentData().getCompound(NAMESPACE);
    return data.getDouble(K_JME);
  }

  /**
   * 读取上次刷新时间（游戏刻）。
   *
   * @param player 玩家
   * @return 上次刷新时间，默认 0
   */
  public static long readLastTick(ServerPlayer player) {
    CompoundTag data = player.getPersistentData().getCompound(NAMESPACE);
    return data.getLong(K_JME_TICK);
  }

  /**
   * 读取扫描半径。
   *
   * @param player 玩家
   * @return 扫描半径，默认使用 Tuning 参数
   */
  public static double readRadius(ServerPlayer player) {
    CompoundTag data = player.getPersistentData().getCompound(NAMESPACE);
    return data.contains(K_JME_RADIUS) ? data.getDouble(K_JME_RADIUS) : JianmaiTuning.JME_RADIUS;
  }

  /**
   * 读取距离权重指数。
   *
   * @param player 玩家
   * @return 距离权重指数，默认使用 Tuning 参数
   */
  public static double readDistAlpha(ServerPlayer player) {
    CompoundTag data = player.getPersistentData().getCompound(NAMESPACE);
    return data.contains(K_JME_DIST_ALPHA)
        ? data.getDouble(K_JME_DIST_ALPHA)
        : JianmaiTuning.JME_DIST_ALPHA;
  }

  /**
   * 读取 JME 上限。
   *
   * @param player 玩家
   * @return JME 上限，默认使用 Tuning 参数
   */
  public static double readMaxCap(ServerPlayer player) {
    CompoundTag data = player.getPersistentData().getCompound(NAMESPACE);
    return data.contains(K_JME_MAX_CAP)
        ? data.getDouble(K_JME_MAX_CAP)
        : JianmaiTuning.JME_MAX_CAP;
  }

  /**
   * 读取临时增幅数据。
   *
   * @param player 玩家
   * @return 增幅数据（CompoundTag），可能为空
   */
  public static CompoundTag readAmp(ServerPlayer player) {
    CompoundTag data = player.getPersistentData().getCompound(NAMESPACE);
    return data.getCompound(K_JME_AMP);
  }

  // ========== 写入方法 ==========

  /**
   * 写入剑脉效率（JME）值。
   *
   * @param player 玩家
   * @param jme JME 值
   */
  public static void writeJME(ServerPlayer player, double jme) {
    CompoundTag root = player.getPersistentData();
    CompoundTag data = root.getCompound(NAMESPACE);
    data.putDouble(K_JME, jme);
    root.put(NAMESPACE, data);
  }

  /**
   * 写入上次刷新时间。
   *
   * @param player 玩家
   * @param tick 游戏刻
   */
  public static void writeLastTick(ServerPlayer player, long tick) {
    CompoundTag root = player.getPersistentData();
    CompoundTag data = root.getCompound(NAMESPACE);
    data.putLong(K_JME_TICK, tick);
    root.put(NAMESPACE, data);
  }

  /**
   * 写入扫描半径。
   *
   * @param player 玩家
   * @param radius 半径
   */
  public static void writeRadius(ServerPlayer player, double radius) {
    CompoundTag root = player.getPersistentData();
    CompoundTag data = root.getCompound(NAMESPACE);
    data.putDouble(K_JME_RADIUS, radius);
    root.put(NAMESPACE, data);
  }

  /**
   * 写入距离权重指数。
   *
   * @param player 玩家
   * @param alpha 指数
   */
  public static void writeDistAlpha(ServerPlayer player, double alpha) {
    CompoundTag root = player.getPersistentData();
    CompoundTag data = root.getCompound(NAMESPACE);
    data.putDouble(K_JME_DIST_ALPHA, alpha);
    root.put(NAMESPACE, data);
  }

  /**
   * 写入 JME 上限。
   *
   * @param player 玩家
   * @param cap 上限
   */
  public static void writeMaxCap(ServerPlayer player, double cap) {
    CompoundTag root = player.getPersistentData();
    CompoundTag data = root.getCompound(NAMESPACE);
    data.putDouble(K_JME_MAX_CAP, cap);
    root.put(NAMESPACE, data);
  }

  /**
   * 写入临时增幅数据。
   *
   * @param player 玩家
   * @param ampK 道痕增幅系数
   * @param multPassive 被动倍率
   * @param multActive 主动倍率
   * @param expireGameTime 被动过期时间
   * @param activeExpireGameTime 主动过期时间
   * @param graceTicks 宽限期
   */
  public static void writeAmp(
      ServerPlayer player,
      double ampK,
      double multPassive,
      double multActive,
      long expireGameTime,
      long activeExpireGameTime,
      int graceTicks) {
    CompoundTag root = player.getPersistentData();
    CompoundTag data = root.getCompound(NAMESPACE);

    CompoundTag amp = new CompoundTag();
    amp.putDouble(K_AMP_K, ampK);
    amp.putDouble(K_AMP_MULT_PASSIVE, multPassive);
    amp.putDouble(K_AMP_MULT_ACTIVE, multActive);
    amp.putLong(K_AMP_EXPIRE, expireGameTime);
    amp.putLong(K_AMP_ACTIVE_EXPIRE, activeExpireGameTime);
    amp.putInt(K_AMP_GRACE, graceTicks);

    data.put(K_JME_AMP, amp);
    root.put(NAMESPACE, data);
  }

  /**
   * 读取增幅倍率系数（ampK）。
   *
   * @param ampData 增幅数据（CompoundTag）
   * @return ampK，默认为 Tuning 参数
   */
  public static double getAmpK(CompoundTag ampData) {
    return ampData.contains(K_AMP_K) ? ampData.getDouble(K_AMP_K) : JianmaiTuning.DAO_JME_K;
  }

  /**
   * 读取被动倍率（multPassive）。
   *
   * @param ampData 增幅数据（CompoundTag）
   * @return multPassive，默认 1.0
   */
  public static double getMultPassive(CompoundTag ampData) {
    return ampData.contains(K_AMP_MULT_PASSIVE) ? ampData.getDouble(K_AMP_MULT_PASSIVE) : 1.0;
  }

  /**
   * 读取主动倍率（multActive）。
   *
   * @param ampData 增幅数据（CompoundTag）
   * @return multActive，默认 1.0
   */
  public static double getMultActive(CompoundTag ampData) {
    return ampData.contains(K_AMP_MULT_ACTIVE) ? ampData.getDouble(K_AMP_MULT_ACTIVE) : 1.0;
  }

  /**
   * 读取被动过期时间（expireGameTime）。
   *
   * @param ampData 增幅数据（CompoundTag）
   * @return 被动过期时间，默认 0
   */
  public static long getExpire(CompoundTag ampData) {
    return ampData.getLong(K_AMP_EXPIRE);
  }

  /**
   * 读取主动过期时间（activeExpireGameTime）。
   *
   * @param ampData 增幅数据（CompoundTag）
   * @return 主动过期时间，默认 0
   */
  public static long getActiveExpire(CompoundTag ampData) {
    return ampData.getLong(K_AMP_ACTIVE_EXPIRE);
  }

  /**
   * 读取宽限期（graceTicks）。
   *
   * @param ampData 增幅数据（CompoundTag）
   * @return 宽限期，默认为 Tuning 参数
   */
  public static int getGrace(CompoundTag ampData) {
    return ampData.contains(K_AMP_GRACE)
        ? ampData.getInt(K_AMP_GRACE)
        : JianmaiTuning.AMP_GRACE_TICKS;
  }

  // ========== 清理方法 ==========

  /**
   * 清空所有剑脉蛊数据（卸载时使用）。
   *
   * @param player 玩家
   */
  public static void clearAll(ServerPlayer player) {
    player.getPersistentData().remove(NAMESPACE);
  }

  /**
   * 清空临时增幅数据。
   *
   * @param player 玩家
   */
  public static void clearAmp(ServerPlayer player) {
    CompoundTag root = player.getPersistentData();
    CompoundTag data = root.getCompound(NAMESPACE);
    data.remove(K_JME_AMP);
    root.put(NAMESPACE, data);
  }
}
