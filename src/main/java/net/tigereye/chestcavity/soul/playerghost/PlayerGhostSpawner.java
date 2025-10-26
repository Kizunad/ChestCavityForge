package net.tigereye.chestcavity.soul.playerghost;

import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.config.CCGameRules;
import net.tigereye.chestcavity.registration.CCEntities;

/**
 * 玩家幽灵定期刷新器
 *
 * <p>职责：
 * - 定期检查（100-600 ticks）是否应该刷新玩家幽灵
 * - 每次检查有 1% 几率触发刷新
 * - 在随机在线玩家周围 24 格范围内刷新
 * - 遵守 SPAWN_FUN_ENTITIES gamerule
 */
public final class PlayerGhostSpawner {

  private static final int MIN_CHECK_INTERVAL_TICKS = 100;
  private static final int MAX_CHECK_INTERVAL_TICKS = 600;
  private static final double SPAWN_CHANCE = 0.01; // 1% 几率
  private static final int MAX_ATTEMPTS = 10;
  private static final double HORIZONTAL_RADIUS = 24.0;
  private static final Random RANDOM = new Random();

  private static int cooldown = MIN_CHECK_INTERVAL_TICKS;

  private PlayerGhostSpawner() {}

  /**
   * 服务器 Tick 事件处理
   *
   * @param event Tick 事件
   */
  public static void onServerTick(ServerTickEvent.Post event) {
    // 检查 gamerule 是否允许刷新
    if (!event.getServer().getGameRules().getBoolean(CCGameRules.SPAWN_FUN_ENTITIES)) {
      return;
    }

    // 检查服务器是否准备好
    if (!event.getServer().isReady()) {
      return;
    }

    // 冷却倒计时
    if (cooldown > 0) {
      cooldown--;
      return;
    }

    // 检查是否有死亡记录
    PlayerGhostWorldData data = PlayerGhostWorldData.get(event.getServer());
    if (!data.hasArchives()) {
      cooldown = MAX_CHECK_INTERVAL_TICKS;
      return;
    }

    // 1% 几率触发刷新
    if (RANDOM.nextDouble() >= SPAWN_CHANCE) {
      cooldown = MIN_CHECK_INTERVAL_TICKS;
      return;
    }

    // 尝试刷新幽灵
    boolean spawned = false;
    for (ServerLevel level : event.getServer().getAllLevels()) {
      if (trySpawn(level, data)) {
        spawned = true;
        break;
      }
    }

    // 设置下次检查的冷却时间
    cooldown =
        spawned
            ? MAX_CHECK_INTERVAL_TICKS
            : MIN_CHECK_INTERVAL_TICKS + RANDOM.nextInt(200);
  }

  /**
   * 尝试在指定世界刷新幽灵
   *
   * @param level 世界
   * @param data 死亡记录数据
   * @return 是否成功刷新
   */
  private static boolean trySpawn(ServerLevel level, PlayerGhostWorldData data) {
    try {
      List<ServerPlayer> players = level.players();
      if (players.isEmpty()) {
        return false;
      }

      // 随机获取一个死亡记录
      PlayerGhostArchive archive = data.getRandom();
      if (archive == null) {
        return false;
      }

      // 尝试多次找到合适的刷新位置
      for (int i = 0; i < MAX_ATTEMPTS; i++) {
        try {
          // 随机选择一个玩家作为锚点
          ServerPlayer anchor = players.get(RANDOM.nextInt(players.size()));
          Vec3 pos = anchor.position();

          // 在玩家周围随机偏移
          double offsetX = (RANDOM.nextDouble() * 2 - 1) * HORIZONTAL_RADIUS;
          double offsetZ = (RANDOM.nextDouble() * 2 - 1) * HORIZONTAL_RADIUS;
          int x = (int) Math.floor(pos.x + offsetX);
          int z = (int) Math.floor(pos.z + offsetZ);
          int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);

          BlockPos spawnPos = new BlockPos(x, y, z);

          // 检查地面是否坚固
          if (!level.getBlockState(spawnPos.below()).isSolid()) {
            continue;
          }

          // 创建幽灵实体
          PlayerGhostEntity ghost =
              PlayerGhostEntity.createFromArchive(CCEntities.PLAYER_GHOST.get(), level, archive);

          // 设置位置
          ghost.moveTo(x + 0.5, y, z + 0.5, RANDOM.nextFloat() * 360f, 0f);

          // 添加到世界
          boolean addedToWorld = level.tryAddFreshEntityWithPassengers(ghost);

          if (addedToWorld) {
            ChestCavity.LOGGER.info(
                "[PlayerGhost] 刷新玩家幽灵: {}的魂魄 于坐标 ({}, {}, {}) 维度 {}",
                archive.getPlayerName(),
                x,
                y,
                z,
                level.dimension().location());
            return true;
          } else {
            ChestCavity.LOGGER.warn(
                "[PlayerGhost] 刷新尝试 #{} 失败 - 无法添加到世界: {}",
                i + 1,
                archive.getPlayerName());
          }
        } catch (Exception e) {
          ChestCavity.LOGGER.warn(
              "[PlayerGhost] 刷新尝试 #{} 失败 - 玩家: {}",
              i + 1,
              archive.getPlayerName(),
              e);
          // 继续下一次尝试
        }
      }

      return false;
    } catch (Exception e) {
      ChestCavity.LOGGER.warn("[PlayerGhost] 刷新幽灵时出错", e);
      return false;
    }
  }
}
