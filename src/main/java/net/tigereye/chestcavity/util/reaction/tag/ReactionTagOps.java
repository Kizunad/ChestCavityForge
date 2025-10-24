package net.tigereye.chestcavity.util.reaction.tag;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

/**
 * 反应 Tag 的附着与判定工具： - 维护“实体 -> (tagId -> expireTick)”映射； - 提供 add/has/clear API； - 对外暴露 {@link
 * #purge(long)} 以便在服务端 tick 清理。
 *
 * <p>说明： - 此处的 Tag 为轻量运行时标记（如“油涂层”、“火衣免疫窗口”），不是数据包 Tag； - 选择用 ResourceLocation 直表述，便于与现有代码平滑过渡。
 */
public final class ReactionTagOps {
  private ReactionTagOps() {}

  private static final Map<UUID, Map<ResourceLocation, Long>> TAGS = new HashMap<>();
  private static final Map<UUID, Map<ResourceLocation, Integer>> STACKS = new HashMap<>();

  public static void add(LivingEntity entity, ResourceLocation tagId, int durationTicks) {
    if (entity == null || tagId == null || durationTicks <= 0) return;
    long expire = entity.level().getGameTime() + durationTicks;
    TAGS.computeIfAbsent(entity.getUUID(), k -> new HashMap<>()).put(tagId, expire);
  }

  /**
   * 为运行时标签增加“堆叠值”，并刷新持续时间。 不同于 {@link #add(LivingEntity, ResourceLocation, int)}，此方法会在内部维护一个计数器。
   */
  public static void addStacked(
      LivingEntity entity, ResourceLocation tagId, int stacksDelta, int durationTicks) {
    if (entity == null || tagId == null || durationTicks <= 0) return;
    long expire = entity.level().getGameTime() + durationTicks;
    TAGS.computeIfAbsent(entity.getUUID(), k -> new HashMap<>()).put(tagId, expire);
    Map<ResourceLocation, Integer> map =
        STACKS.computeIfAbsent(entity.getUUID(), k -> new HashMap<>());
    int cur = Math.max(0, map.getOrDefault(tagId, 0));
    int next = Math.max(0, cur + stacksDelta);
    map.put(tagId, next);
  }

  /** 当前标签堆叠值（若未附着则为0）。 */
  public static int count(LivingEntity entity, ResourceLocation tagId) {
    if (entity == null || tagId == null) return 0;
    Map<ResourceLocation, Integer> map = STACKS.get(entity.getUUID());
    if (map == null) return 0;
    return Math.max(0, map.getOrDefault(tagId, 0));
  }

  public static boolean has(LivingEntity entity, ResourceLocation tagId) {
    if (entity == null || tagId == null) return false;
    Map<ResourceLocation, Long> map = TAGS.get(entity.getUUID());
    if (map == null) return false;
    Long exp = map.get(tagId);
    return exp != null && exp > entity.level().getGameTime();
  }

  /** Remaining ticks before the tag expires; zero if absent or already expired. */
  public static int remainingTicks(LivingEntity entity, ResourceLocation tagId) {
    if (entity == null || tagId == null) return 0;
    Map<ResourceLocation, Long> map = TAGS.get(entity.getUUID());
    if (map == null) return 0;
    Long expire = map.get(tagId);
    if (expire == null) return 0;
    long now = entity.level().getGameTime();
    long remaining = expire - now;
    if (remaining <= 0L) {
      return 0;
    }
    return remaining > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) remaining;
  }

  public static void clear(LivingEntity entity, ResourceLocation tagId) {
    if (entity == null || tagId == null) return;
    Map<ResourceLocation, Long> map = TAGS.get(entity.getUUID());
    if (map != null) {
      map.remove(tagId);
      if (map.isEmpty()) TAGS.remove(entity.getUUID());
    }
    Map<ResourceLocation, Integer> s = STACKS.get(entity.getUUID());
    if (s != null) {
      s.remove(tagId);
      if (s.isEmpty()) STACKS.remove(entity.getUUID());
    }
  }

  public static void purge(long nowServerTick) {
    if (TAGS.isEmpty()) return;
    Iterator<Map.Entry<UUID, Map<ResourceLocation, Long>>> it = TAGS.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry<UUID, Map<ResourceLocation, Long>> e = it.next();
      Map<ResourceLocation, Long> inner = e.getValue();
      if (inner == null || inner.isEmpty()) {
        it.remove();
        continue;
      }
      inner.entrySet().removeIf(en -> en.getValue() <= nowServerTick);
      if (inner.isEmpty()) it.remove();
      // 同步清理已过期标签的堆叠
      Map<ResourceLocation, Integer> s = STACKS.get(e.getKey());
      if (s != null) {
        Iterator<Map.Entry<ResourceLocation, Integer>> sit = s.entrySet().iterator();
        while (sit.hasNext()) {
          Map.Entry<ResourceLocation, Integer> se = sit.next();
          Long exp = inner.get(se.getKey());
          if (exp == null || exp <= nowServerTick) {
            sit.remove();
          }
        }
        if (s.isEmpty()) STACKS.remove(e.getKey());
      }
    }
  }
}
