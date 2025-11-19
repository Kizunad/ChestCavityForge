package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.calculator;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerPlayer;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.tuning.HunDaoRuntimeTuning;

/**
 * Lightweight cache for Hun Dao dao-hen effective values.
 *
 * <p>Phase 9 only requires a simple "compile-time verified" placeholder, so the cache simply
 * memorizes the last computed value per player with a short TTL to avoid hammering the resource
 * bridge every tick. Later phases can extend the entry payload with the raw dao-hen/liupai
 * snapshot similar to Jian Dao.
 */
final class HunDaoDaohenCache {

  private static final Map<UUID, Entry> CACHE = new ConcurrentHashMap<>();

  private HunDaoDaohenCache() {}

  static double getEffective(ServerPlayer player, long now, Supplier<Double> loader) {
    UUID uuid = player.getUUID();
    Entry entry = CACHE.get(uuid);
    if (entry != null && now - entry.updatedAt < HunDaoRuntimeTuning.DaoHenCache.TTL_TICKS) {
      return entry.value;
    }

    double computed = loader.get();
    CACHE.put(uuid, new Entry(computed, now));
    return computed;
  }

  static void invalidate(ServerPlayer player) {
    CACHE.remove(player.getUUID());
  }

  static void clearAll() {
    CACHE.clear();
  }

  private static final class Entry {
    final double value;
    final long updatedAt;

    Entry(double value, long updatedAt) {
      this.value = value;
      this.updatedAt = updatedAt;
    }
  }
}
