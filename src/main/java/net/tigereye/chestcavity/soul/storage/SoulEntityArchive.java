package net.tigereye.chestcavity.soul.storage;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

/** 通用灵魂实体存档：用于保存不隶属于玩家阵营的“敌对”或特殊实体的快照。 */
public final class SoulEntityArchive extends SavedData {

  private static final String DATA_NAME = "chestcavity_soul_entity_archive";

  private final Map<UUID, CompoundTag> entries = new HashMap<>();

  public static SoulEntityArchive get(MinecraftServer server) {
    ServerLevel overworld = server.getLevel(Level.OVERWORLD);
    if (overworld == null) {
      java.util.Iterator<ServerLevel> it = server.getAllLevels().iterator();
      if (!it.hasNext()) {
        return new SoulEntityArchive();
      }
      overworld = it.next();
    }
    return overworld
        .getDataStorage()
        .computeIfAbsent(
            new SavedData.Factory<>(SoulEntityArchive::new, SoulEntityArchive::load), DATA_NAME);
  }

  private SoulEntityArchive() {}

  private static SoulEntityArchive load(CompoundTag tag, HolderLookup.Provider provider) {
    SoulEntityArchive archive = new SoulEntityArchive();
    if (tag == null) {
      return archive;
    }
    for (String key : tag.getAllKeys()) {
      try {
        UUID id = UUID.fromString(key);
        archive.entries.put(id, tag.getCompound(key).copy());
      } catch (IllegalArgumentException ignored) {
      }
    }
    return archive;
  }

  @Override
  public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
    for (Map.Entry<UUID, CompoundTag> entry : entries.entrySet()) {
      tag.put(entry.getKey().toString(), entry.getValue().copy());
    }
    return tag;
  }

  public void put(UUID id, CompoundTag data) {
    if (id == null || data == null) {
      return;
    }
    entries.put(id, data.copy());
    setDirty();
  }

  public Optional<CompoundTag> peek(UUID id) {
    CompoundTag tag = entries.get(id);
    return tag == null ? Optional.empty() : Optional.of(tag.copy());
  }

  public Optional<CompoundTag> consume(UUID id) {
    CompoundTag removed = entries.remove(id);
    if (removed == null) {
      return Optional.empty();
    }
    setDirty();
    return Optional.of(removed.copy());
  }

  public void remove(UUID id) {
    if (entries.remove(id) != null) {
      setDirty();
    }
  }

  public Map<UUID, CompoundTag> snapshot() {
    Map<UUID, CompoundTag> copy = new HashMap<>();
    entries.forEach((id, tag) -> copy.put(id, tag.copy()));
    return Collections.unmodifiableMap(copy);
  }
}
