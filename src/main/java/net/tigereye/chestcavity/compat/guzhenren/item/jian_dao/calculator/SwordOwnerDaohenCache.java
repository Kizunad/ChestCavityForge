package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.calculator;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerPlayer;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;

/**
 * 飞剑主人道痕缓存管理器。
 *
 * <p>提供玩家级别的"有效剑道道痕"缓存，TTL = 20 tick（1 秒）：
 * <ul>
 *   <li>缓存包含：lastD（道痕）、lastL（流派经验）、lastEffective（有效值）、updatedAt（更新时间）</li>
 *   <li>如果 now - updatedAt >= 20：自动触发重算</li>
 *   <li>如果 D/L 与上次一致（|Δ|≤1e-6）：跳过重算，复用 lastEffective</li>
 *   <li>失效接口：在 JME/主动券变化后调用 invalidate(player) 触发下一帧刷新</li>
 * </ul>
 *
 * <p>线程安全：使用 {@link ConcurrentHashMap} 支持并发访问。
 */
public final class SwordOwnerDaohenCache {

  private SwordOwnerDaohenCache() {}

  /** 缓存 TTL（tick）：20 tick = 1 秒 */
  private static final int CACHE_TTL = 20;

  /** 道痕/流派经验变化阈值：|Δ|≤EPSILON 视为未变化 */
  private static final double EPSILON = 1e-6;

  /** 缓存条目 */
  private static class Entry {
    double lastD;          // 上次道痕值
    double lastL;          // 上次流派经验值
    double lastEffective;  // 上次有效值
    long updatedAt;        // 上次更新时间（游戏刻）
    int gen;               // 代数（用于失效）

    Entry(double d, double l, double effective, long updatedAt) {
      this.lastD = d;
      this.lastL = l;
      this.lastEffective = effective;
      this.updatedAt = updatedAt;
      this.gen = 0;
    }
  }

  /** 玩家 UUID -> 缓存条目 */
  private static final Map<UUID, Entry> CACHE = new ConcurrentHashMap<>();

  /**
   * 获取有效剑道道痕值（带缓存）。
   *
   * <p>规则：
   * <ul>
   *   <li>缓存命中且 now - updatedAt < TTL 且 D/L 未变化：返回缓存值</li>
   *   <li>否则：调用 loader 重新计算并更新缓存</li>
   * </ul>
   *
   * @param player 玩家
   * @param now 当前游戏刻
   * @param loader 有效值计算器（懒加载）
   * @return 有效剑道道痕值
   */
  public static double getEffective(ServerPlayer player, long now, Supplier<Double> loader) {
    UUID uuid = player.getUUID();

    // 读取当前道痕与流派经验
    var handle = ResourceOps.openHandle(player).orElse(null);
    if (handle == null) {
      return 0.0;
    }

    double currentD = handle.read("daohen_jiandao").orElse(0.0);
    double currentL = handle.read("liupai_jiandao").orElse(0.0);

    // 检查缓存
    Entry entry = CACHE.get(uuid);
    if (entry != null) {
      long age = now - entry.updatedAt;

      // 缓存未过期 且 D/L 未变化：返回缓存值
      if (age < CACHE_TTL
          && Math.abs(currentD - entry.lastD) <= EPSILON
          && Math.abs(currentL - entry.lastL) <= EPSILON) {
        return entry.lastEffective;
      }
    }

    // 缓存失效或过期：重新计算
    double effective = loader.get();

    // 更新缓存
    if (entry == null) {
      entry = new Entry(currentD, currentL, effective, now);
      CACHE.put(uuid, entry);
    } else {
      entry.lastD = currentD;
      entry.lastL = currentL;
      entry.lastEffective = effective;
      entry.updatedAt = now;
    }

    return effective;
  }

  /**
   * 使指定玩家的缓存失效（下一次调用将重新计算）。
   *
   * <p>用于 JME/主动券变化后触发立即刷新。
   *
   * @param player 玩家
   */
  public static void invalidate(ServerPlayer player) {
    Entry entry = CACHE.get(player.getUUID());
    if (entry != null) {
      // 强制过期：将 updatedAt 设为远古时间
      entry.updatedAt = 0L;
      entry.gen++;
    }
  }

  /**
   * 清空所有缓存（用于调试或重载）。
   */
  public static void clearAll() {
    CACHE.clear();
  }
}
