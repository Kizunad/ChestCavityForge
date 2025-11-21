package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.entity;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.tigereye.chestcavity.ChestCavity;

/** Tracks whether the unique Hun Dao soul avatar boss has spawned in a world. */
public final class HunDaoSoulAvatarWorldBossData extends SavedData {

  private static final String DATA_NAME = ChestCavity.MODID + ":hun_dao_world_boss";

  private boolean spawned;

  public static HunDaoSoulAvatarWorldBossData get(MinecraftServer server) {
    ServerLevel overworld = server.getLevel(Level.OVERWORLD);
    if (overworld == null) {
      java.util.Iterator<ServerLevel> iterator = server.getAllLevels().iterator();
      if (!iterator.hasNext()) {
        return new HunDaoSoulAvatarWorldBossData();
      }
      overworld = iterator.next();
    }
    return overworld
        .getDataStorage()
        .computeIfAbsent(
            new SavedData.Factory<>(
                HunDaoSoulAvatarWorldBossData::new, HunDaoSoulAvatarWorldBossData::load),
            DATA_NAME);
  }

  private HunDaoSoulAvatarWorldBossData() {}

  private static HunDaoSoulAvatarWorldBossData load(
      CompoundTag tag, HolderLookup.Provider provider) {
    HunDaoSoulAvatarWorldBossData data = new HunDaoSoulAvatarWorldBossData();
    data.spawned = tag.getBoolean("Spawned");
    return data;
  }

  @Override
  public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
    tag.putBoolean("Spawned", spawned);
    return tag;
  }

  public boolean isSpawned() {
    return spawned;
  }

  public void markSpawned() {
    if (!spawned) {
      spawned = true;
      setDirty();
    }
  }
}
