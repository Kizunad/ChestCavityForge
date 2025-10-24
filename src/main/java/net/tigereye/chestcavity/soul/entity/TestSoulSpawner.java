package net.tigereye.chestcavity.soul.entity;

import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.tigereye.chestcavity.registration.CCEntities;

/** 简易 TestSoul 自动生成器：确保在世界加载后至少存在一只 Test 生物。 */
public final class TestSoulSpawner {

  private static final int CHECK_INTERVAL_TICKS = 600;
  private static final int MAX_ATTEMPTS = 6;
  private static final double HORIZONTAL_RADIUS = 24.0;
  private static final Random RANDOM = new Random();

  private static int cooldown = 20;

  private TestSoulSpawner() {}

  public static void onServerTick(ServerTickEvent.Post event) {
    if (!event.getServer().getGameRules().getBoolean(net.tigereye.chestcavity.config.CCGameRules.SPAWN_FUN_ENTITIES)) {
        return;
    }
    // Guard clause: only run when the server is ready
    if (!event.getServer().isReady()) {
      return;
    }

    if (cooldown > 0) {
      cooldown--;
      return;
    }
    boolean spawned = false;
    for (ServerLevel level : event.getServer().getAllLevels()) {
      if (!TestSoulManager.canSpawn(level)) {
        continue;
      }
      if (trySpawn(level)) {
        spawned = true;
        break;
      }
    }
    cooldown = spawned ? CHECK_INTERVAL_TICKS : 40;
  }

  private static boolean trySpawn(ServerLevel level) {
    List<ServerPlayer> players = level.players();
    if (players.isEmpty()) {
      return false;
    }
    EntityType<TestSoulEntity> type = CCEntities.TEST_SOUL.get();
    for (int i = 0; i < MAX_ATTEMPTS; i++) {
      ServerPlayer anchor = players.get(RANDOM.nextInt(players.size()));
      Vec3 pos = anchor.position();
      double offsetX = (RANDOM.nextDouble() * 2 - 1) * HORIZONTAL_RADIUS;
      double offsetZ = (RANDOM.nextDouble() * 2 - 1) * HORIZONTAL_RADIUS;
      int x = (int) Math.floor(pos.x + offsetX);
      int z = (int) Math.floor(pos.z + offsetZ);
      int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
      BlockPos spawnPos = new BlockPos(x, y, z);
      if (!level.getBlockState(spawnPos.below()).isSolid()) {
        continue;
      }
      TestSoulEntity entity = type.create(level);
      if (entity == null) {
        return false;
      }
      entity.moveTo(x + 0.5, y, z + 0.5, RANDOM.nextFloat() * 360f, 0f);
      if (!level.noCollision(entity)) {
        continue;
      }
      if (level.addFreshEntity(entity)) {
        TestSoulManager.register(entity);
        return true;
      }
    }
    return false;
  }
}
