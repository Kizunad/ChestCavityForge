package net.tigereye.chestcavity.soul.playerghost;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * 玩家死亡记录世界存档
 *
 * <p>职责：
 * - 存储所有玩家死亡记录到世界存档（Overworld）
 * - 提供添加、查询、随机获取死亡记录的功能
 * - 自动持久化到磁盘
 */
public final class PlayerGhostWorldData extends SavedData {

  private static final String DATA_NAME = "chestcavity_player_ghost_archive";
  private static final Random RANDOM = new Random();

  private final Map<UUID, PlayerGhostArchive> archives = new HashMap<>();

  /**
   * 获取世界存档实例
   *
   * @param server 服务器实例
   * @return 玩家死亡记录存档
   */
  public static PlayerGhostWorldData get(MinecraftServer server) {
    ServerLevel overworld = server.getLevel(Level.OVERWORLD);
    if (overworld == null) {
      java.util.Iterator<ServerLevel> it = server.getAllLevels().iterator();
      if (!it.hasNext()) {
        return new PlayerGhostWorldData();
      }
      overworld = it.next();
    }
    return overworld
        .getDataStorage()
        .computeIfAbsent(
            new SavedData.Factory<>(
                PlayerGhostWorldData::new, PlayerGhostWorldData::load),
            DATA_NAME);
  }

  private PlayerGhostWorldData() {}

  /**
   * 从 NBT 加载数据
   *
   * @param tag NBT 标签
   * @param provider 注册中心提供者
   * @return 加载后的数据实例
   */
  private static PlayerGhostWorldData load(CompoundTag tag, HolderLookup.Provider provider) {
    PlayerGhostWorldData data = new PlayerGhostWorldData();
    if (tag == null) {
      return data;
    }

    CompoundTag archivesTag = tag.getCompound("archives");
    for (String key : archivesTag.getAllKeys()) {
      try {
        UUID archiveId = UUID.fromString(key);
        CompoundTag archiveTag = archivesTag.getCompound(key);
        PlayerGhostArchive archive = PlayerGhostArchive.fromNbt(archiveTag, provider);
        if (archive != null) {
          data.archives.put(archiveId, archive);
        }
      } catch (IllegalArgumentException e) {
        // 忽略无效的 UUID 键
      }
    }

    return data;
  }

  @Override
  public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
    CompoundTag archivesTag = new CompoundTag();
    for (Map.Entry<UUID, PlayerGhostArchive> entry : archives.entrySet()) {
      archivesTag.put(entry.getKey().toString(), entry.getValue().toNbt(provider));
    }
    tag.put("archives", archivesTag);
    return tag;
  }

  /**
   * 添加新的死亡记录
   *
   * @param archive 玩家死亡记录
   */
  public void add(PlayerGhostArchive archive) {
    if (archive == null) {
      return;
    }
    // 使用随机 UUID 作为存档键，允许同一玩家有多个死亡记录
    UUID archiveId = UUID.randomUUID();
    archives.put(archiveId, archive);
    setDirty();
  }

  /**
   * 获取所有死亡记录
   *
   * @return 所有死亡记录列表（不可修改）
   */
  public List<PlayerGhostArchive> getAll() {
    return new ArrayList<>(archives.values());
  }

  /**
   * 随机获取一个死亡记录用于刷新
   *
   * @return 随机选择的死亡记录，如果没有记录则返回 null
   */
  public PlayerGhostArchive getRandom() {
    if (archives.isEmpty()) {
      return null;
    }
    List<PlayerGhostArchive> list = new ArrayList<>(archives.values());
    return list.get(RANDOM.nextInt(list.size()));
  }

  /**
   * 获取存档数量
   *
   * @return 死亡记录数量
   */
  public int size() {
    return archives.size();
  }

  /**
   * 检查是否有存档
   *
   * @return 如果有至少一个死亡记录则返回 true
   */
  public boolean hasArchives() {
    return !archives.isEmpty();
  }
}
