package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.entity;

import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.tuning.HunDaoRuntimeTuning;
import net.tigereye.chestcavity.registration.CCEntities;

/** Handles spawning the world-unique Hun Dao soul avatar boss. */
public final class HunDaoSoulAvatarWorldBossSpawner {

  private HunDaoSoulAvatarWorldBossSpawner() {}

  public static void handlePlayerLogin(ServerPlayer player) {
    if (player == null || player.getServer() == null) {
      return;
    }
    HunDaoSoulAvatarWorldBossData data = HunDaoSoulAvatarWorldBossData.get(player.getServer());
    if (data.isSpawned()) {
      return;
    }
    ServerLevel level = player.getServer().getLevel(Level.OVERWORLD);
    if (level == null) {
      return;
    }
    if (trySpawn(level, player)) {
      data.markSpawned();
    }
  }

  private static boolean trySpawn(ServerLevel level, ServerPlayer reference) {
    Vec3 position = computeSpawnPosition(level, reference);
    if (position == null) {
      return false;
    }
    EntityType<HunDaoSoulAvatarWorldBossEntity> type = CCEntities.HUN_DAO_SOUL_AVATAR_BOSS.get();
    HunDaoSoulAvatarWorldBossEntity entity = type.create(level);
    if (entity == null) {
      return false;
    }
    entity.moveTo(position.x, position.y, position.z, level.random.nextFloat() * 360.0F, 0.0F);
    entity.setTarget(reference);
    boolean added = level.addFreshEntity(entity);
    if (added) {
      ChestCavity.LOGGER.info(
          "[hun_dao] Spawned world boss at ({}, {}, {})", position.x, position.y, position.z);
    }
    return added;
  }

  @Nullable
  private static Vec3 computeSpawnPosition(ServerLevel level, ServerPlayer reference) {
    double min = HunDaoRuntimeTuning.SoulAvatarWorldBoss.MIN_SPAWN_DISTANCE;
    double extra =
        level.random.nextDouble() * HunDaoRuntimeTuning.SoulAvatarWorldBoss.EXTRA_SPAWN_DISTANCE;
    double distance = min + extra;
    double angle = level.random.nextDouble() * Math.PI * 2.0D;
    double x = reference.getX() + Math.cos(angle) * distance;
    double z = reference.getZ() + Math.sin(angle) * distance;
    int blockX = Mth.floor(x);
    int blockZ = Mth.floor(z);
    level.getChunk(blockX >> 4, blockZ >> 4);
    int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, blockX, blockZ);
    if (y <= level.getMinBuildHeight()) {
      return null;
    }
    return new Vec3(x + 0.5D, y, z + 0.5D);
  }
}
