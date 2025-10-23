package net.tigereye.chestcavity.soul.entity;

import java.util.*;
import net.minecraft.server.level.ServerLevel;
import net.tigereye.chestcavity.soul.entity.data.SoulClanWorldData;

public final class SoulClanManager {

  private static final Map<UUID, SoulClanEntity> LIVE = new WeakHashMap<>();

  private SoulClanManager() {}

  static void onTickRegister(SoulClanEntity e) {
    LIVE.put(e.getUUID(), e);
    if (e.getVariant() == SoulClanEntity.Variant.ELDER) {
      findAnotherElder(e)
          .ifPresent(
              other -> {
                e.setTarget(other);
                other.setTarget(e);
              });
    }
  }

  public static Optional<SoulClanEntity> findElder(net.minecraft.world.level.Level level) {
    if (level.isClientSide) return Optional.empty();
    UUID id = SoulClanWorldData.get((ServerLevel) level).getElderId();
    SoulClanEntity byData = id == null ? null : LIVE.get(id);
    if (byData != null && byData.isAlive()) return Optional.of(byData);
    return LIVE.values().stream()
        .filter(m -> m.isAlive() && m.getVariant() == SoulClanEntity.Variant.ELDER)
        .findFirst();
  }

  public static void tryElectElder(ServerLevel level, SoulClanEntity candidate) {
    Optional<SoulClanEntity> current = findElder(level);
    if (current.isEmpty() || !current.get().isAlive()) {
      SoulClanWorldData data = SoulClanWorldData.get(level);
      data.setElderId(candidate.getUUID());
    } else {
      SoulClanEntity other = current.get();
      candidate.setTarget(other);
      other.setTarget(candidate);
    }
  }

  public static void onElderDead(ServerLevel level, UUID elderId) {
    SoulClanWorldData data = SoulClanWorldData.get(level);
    if (Objects.equals(data.getElderId(), elderId)) data.setElderId(null);
  }

  private static Optional<SoulClanEntity> findAnotherElder(SoulClanEntity self) {
    return LIVE.values().stream()
        .filter(e -> e != self && e.isAlive() && e.getVariant() == SoulClanEntity.Variant.ELDER)
        .findFirst();
  }

  public static int getAreaCap(ServerLevel level, int fallback) {
    return Math.max(1, SoulClanWorldData.get(level).getAreaCapOr(fallback));
  }
}
