package net.tigereye.chestcavity.skill.effects;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * 每次技能触发（cast）的临时状态聚合器：记录 pre 阶段返回的句柄，供 post 阶段按结果清理或安排过期。
 */
final class SkillTempState {

  private SkillTempState() {}

  private static final Map<UUID, AtomicLong> CAST_COUNTERS = new ConcurrentHashMap<>();
  private static final Map<UUID, Map<ResourceLocation, PendingCast>> PENDING =
      new ConcurrentHashMap<>();

  static long beginCast(ServerPlayer player, ResourceLocation skillId) {
    UUID id = player.getUUID();
    long castId = CAST_COUNTERS.computeIfAbsent(id, k -> new AtomicLong()).incrementAndGet();
    PENDING
        .computeIfAbsent(id, k -> new ConcurrentHashMap<>())
        .compute(
            skillId,
            (k, existing) -> {
              if (existing != null) {
                existing.revertAll();
              }
              return new PendingCast(castId);
            });
    return castId;
  }

  static void recordHandle(ServerPlayer player, ResourceLocation skillId, AppliedHandle handle) {
    if (handle == null) return;
    Map<ResourceLocation, PendingCast> bySkill = PENDING.get(player.getUUID());
    if (bySkill == null) return;
    PendingCast pending = bySkill.get(skillId);
    if (pending == null) return;
    pending.add(handle);
  }

  static PendingCast endCast(ServerPlayer player, ResourceLocation skillId) {
    Map<ResourceLocation, PendingCast> bySkill = PENDING.get(player.getUUID());
    if (bySkill == null) return PendingCast.empty();
    PendingCast pending = bySkill.remove(skillId);
    if (bySkill.isEmpty()) {
      PENDING.remove(player.getUUID());
    }
    return pending == null ? PendingCast.empty() : pending;
  }

  static void cleanup(ServerPlayer player) {
    Map<ResourceLocation, PendingCast> bySkill = PENDING.remove(player.getUUID());
    if (bySkill == null) return;
    for (PendingCast cast : bySkill.values()) {
      cast.revertAll();
    }
  }

  static final class PendingCast {
    private final long castId;
    private final List<AppliedHandle> handles = new ArrayList<>();

    private final Map<String, Double> metadata = new ConcurrentHashMap<>();

    PendingCast(long castId) {
      this.castId = castId;
    }

    static PendingCast empty() {
      return new PendingCast(0L);
    }

    void add(AppliedHandle handle) {
      if (handle != null) {
        handles.add(handle);
      }
    }

    long castId() {
      return castId;
    }

    List<AppliedHandle> handles() {
      return List.copyOf(handles);
    }

    void revertAll() {
      for (AppliedHandle h : handles) {
        try {
          h.revert();
        } catch (Throwable ignored) {
        }
      }
      handles.clear();
    }

    void putMetadata(String key, double value) {
      if (key != null && Double.isFinite(value)) {
        metadata.put(key, value);
      }
    }

    java.util.OptionalDouble consumeMetadata(String key) {
      if (key == null) return java.util.OptionalDouble.empty();
      Double removed = metadata.remove(key);
      return removed == null ? java.util.OptionalDouble.empty() : java.util.OptionalDouble.of(removed);
    }
  }

  static void putMetadata(
      ServerPlayer player, ResourceLocation skillId, String key, double value) {
    if (player == null || skillId == null) {
      return;
    }
    Map<ResourceLocation, PendingCast> bySkill = PENDING.get(player.getUUID());
    if (bySkill == null) {
      return;
    }
    PendingCast cast = bySkill.get(skillId);
    if (cast == null) {
      return;
    }
    cast.putMetadata(key, value);
  }

  static java.util.OptionalDouble consumeMetadata(
      ServerPlayer player, ResourceLocation skillId, String key) {
    if (player == null || skillId == null) {
      return java.util.OptionalDouble.empty();
    }
    Map<ResourceLocation, PendingCast> bySkill = PENDING.get(player.getUUID());
    if (bySkill == null) {
      return java.util.OptionalDouble.empty();
    }
    PendingCast cast = bySkill.get(skillId);
    if (cast == null) {
      return java.util.OptionalDouble.empty();
    }
    return cast.consumeMetadata(key);
  }
}
