package net.tigereye.chestcavity.soul.entity;

import java.lang.ref.WeakReference;
import java.util.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.tigereye.chestcavity.soul.entity.data.SoulClanWorldData;

public final class SoulClanManager {
  private static final Map<ResourceKey<Level>, Map<UUID, WeakReference<SoulClanEntity>>> LIVE =
      new HashMap<>();

  private SoulClanManager() {}

  static void onTickRegister(SoulClanEntity e) {
    if (!(e.level() instanceof ServerLevel sl)) return;
    Map<UUID, WeakReference<SoulClanEntity>> m =
        LIVE.computeIfAbsent(sl.dimension(), k -> new HashMap<>());
    m.put(e.getUUID(), new WeakReference<>(e));

    if (e.getVariant() == SoulClanEntity.Variant.ELDER) {
      findAnotherElder(sl, e)
          .ifPresent(
              other -> {
                e.setTarget(other);
                other.setTarget(e);
              });
    }
    m.entrySet()
        .removeIf(
            ent -> {
              SoulClanEntity s = ent.getValue().get();
              return s == null || !s.isAlive();
            });
  }

  public static Optional<SoulClanEntity> findElder(Level level) {
    if (!(level instanceof ServerLevel sl)) return Optional.empty();
    UUID id = SoulClanWorldData.get(sl).getElderId();
    Map<UUID, WeakReference<SoulClanEntity>> m = LIVE.get(sl.dimension());
    if (id != null && m != null) {
      SoulClanEntity exact = Optional.ofNullable(m.get(id)).map(WeakReference::get).orElse(null);
      if (exact != null && exact.isAlive()) return Optional.of(exact);
    }
    if (m != null) {
      for (WeakReference<SoulClanEntity> r : m.values()) {
        SoulClanEntity s = r.get();
        if (s != null && s.isAlive() && s.getVariant() == SoulClanEntity.Variant.ELDER)
          return Optional.of(s);
      }
    }
    return Optional.empty();
  }

  public static void tryElectElder(ServerLevel level, SoulClanEntity candidate) {
    Optional<SoulClanEntity> cur = findElder(level);
    if (cur.isEmpty() || !cur.get().isAlive()) {
      SoulClanWorldData.get(level).setElderId(candidate.getUUID());
    } else {
      SoulClanEntity other = cur.get();
      candidate.setTarget(other);
      other.setTarget(candidate);
    }
  }

  public static void onElderDead(ServerLevel level, UUID elderId) {
    if (Objects.equals(SoulClanWorldData.get(level).getElderId(), elderId)) {
      SoulClanWorldData.get(level).setElderId(null);
    }
  }

  private static Optional<SoulClanEntity> findAnotherElder(ServerLevel sl, SoulClanEntity self) {
    Map<UUID, WeakReference<SoulClanEntity>> m = LIVE.get(sl.dimension());
    if (m == null) return Optional.empty();
    for (WeakReference<SoulClanEntity> r : m.values()) {
      SoulClanEntity s = r.get();
      if (s != null && s != self && s.isAlive() && s.getVariant() == SoulClanEntity.Variant.ELDER)
        return Optional.of(s);
    }
    return Optional.empty();
  }

  public static int getAreaCap(ServerLevel level, int fallback) {
    return Math.max(1, SoulClanWorldData.get(level).getAreaCapOr(fallback));
  }
}
