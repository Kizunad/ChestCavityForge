package net.tigereye.chestcavity.engine.fx;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

/**
 * FX 门控工具类：提供玩家半径检查、区块加载检查等门控判定功能。
 *
 * <p>Stage 4 核心工具类。用于判定 FX 是否应该执行（门控条件）。
 */
public final class FxGatingUtils {

  private FxGatingUtils() {
    // 工具类，禁止实例化
  }

  /**
   * 检查指定位置是否在任意玩家的半径范围内。
   *
   * @param level 服务器世界
   * @param pos 位置坐标
   * @param radius 半径（方块单位）
   * @return 如果至少有一个玩家在范围内则返回 true
   */
  public static boolean isWithinPlayerRadius(ServerLevel level, Vec3 pos, double radius) {
    if (level == null || pos == null) {
      return false;
    }

    double radiusSq = radius * radius;
    for (ServerPlayer player : level.players()) {
      if (player.distanceToSqr(pos) <= radiusSq) {
        return true;
      }
    }
    return false;
  }

  /**
   * 检查指定位置是否在任意玩家的半径范围内（使用 BlockPos）。
   *
   * @param level 服务器世界
   * @param pos 位置坐标
   * @param radius 半径（方块单位）
   * @return 如果至少有一个玩家在范围内则返回 true
   */
  public static boolean isWithinPlayerRadius(ServerLevel level, BlockPos pos, double radius) {
    if (pos == null) {
      return false;
    }
    return isWithinPlayerRadius(level, Vec3.atCenterOf(pos), radius);
  }

  /**
   * 检查指定实体位置是否在任意玩家的半径范围内。
   *
   * @param entity 实体
   * @param radius 半径（方块单位）
   * @return 如果至少有一个玩家在范围内则返回 true
   */
  public static boolean isWithinPlayerRadius(Entity entity, double radius) {
    if (entity == null || !(entity.level() instanceof ServerLevel serverLevel)) {
      return false;
    }
    return isWithinPlayerRadius(serverLevel, entity.position(), radius);
  }

  /**
   * 检查指定区块位置是否已加载。
   *
   * @param level 服务器世界
   * @param chunkPos 区块坐标
   * @return 如果区块已加载则返回 true
   */
  public static boolean isChunkLoaded(ServerLevel level, ChunkPos chunkPos) {
    if (level == null || chunkPos == null) {
      return false;
    }
    return level.hasChunk(chunkPos.x, chunkPos.z);
  }

  /**
   * 检查指定方块位置所在的区块是否已加载。
   *
   * @param level 服务器世界
   * @param pos 方块坐标
   * @return 如果区块已加载则返回 true
   */
  public static boolean isChunkLoaded(ServerLevel level, BlockPos pos) {
    if (level == null || pos == null) {
      return false;
    }
    return level.isLoaded(pos);
  }

  /**
   * 检查指定世界坐标所在的区块是否已加载。
   *
   * @param level 服务器世界
   * @param pos 世界坐标
   * @return 如果区块已加载则返回 true
   */
  public static boolean isChunkLoaded(ServerLevel level, Vec3 pos) {
    if (pos == null) {
      return false;
    }
    return isChunkLoaded(level, BlockPos.containing(pos));
  }

  /**
   * 检查指定实体位置所在的区块是否已加载。
   *
   * @param entity 实体
   * @return 如果区块已加载则返回 true
   */
  public static boolean isChunkLoaded(Entity entity) {
    if (entity == null || !(entity.level() instanceof ServerLevel serverLevel)) {
      return false;
    }
    return isChunkLoaded(serverLevel, entity.position());
  }

  /**
   * 检查指定实体是否有效（未移除且存活）。
   *
   * @param entity 实体
   * @return 如果实体有效则返回 true
   */
  public static boolean isEntityValid(Entity entity) {
    if (entity == null) {
      return false;
    }
    return !entity.isRemoved() && entity.isAlive();
  }

  /**
   * 综合门控检查：检查实体是否有效、区块是否加载、是否在玩家半径内。
   *
   * @param entity 实体
   * @param playerRadius 玩家半径（方块单位），如果 <= 0 则不检查玩家半径
   * @return 如果所有门控条件均满足则返回 true
   */
  public static boolean checkGating(Entity entity, double playerRadius) {
    if (!isEntityValid(entity)) {
      return false;
    }

    if (!(entity.level() instanceof ServerLevel serverLevel)) {
      return false;
    }

    if (!isChunkLoaded(entity)) {
      return false;
    }

    if (playerRadius > 0 && !isWithinPlayerRadius(entity, playerRadius)) {
      return false;
    }

    return true;
  }

  /**
   * 综合门控检查：检查位置的区块是否加载、是否在玩家半径内。
   *
   * @param level 服务器世界
   * @param pos 位置坐标
   * @param playerRadius 玩家半径（方块单位），如果 <= 0 则不检查玩家半径
   * @return 如果所有门控条件均满足则返回 true
   */
  public static boolean checkGating(ServerLevel level, Vec3 pos, double playerRadius) {
    if (level == null || pos == null) {
      return false;
    }

    if (!isChunkLoaded(level, pos)) {
      return false;
    }

    if (playerRadius > 0 && !isWithinPlayerRadius(level, pos, playerRadius)) {
      return false;
    }

    return true;
  }
}
