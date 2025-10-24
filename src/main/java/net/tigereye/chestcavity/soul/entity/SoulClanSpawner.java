package net.tigereye.chestcavity.soul.entity;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.tigereye.chestcavity.registration.CCEntities;

/**
 * SoulClan 专用的被动刷怪管理器。
 *
 * <p>核心职责：
 *
 * <ul>
 *   <li>定期扫描世界以确保有且仅有一个长老存活；
 *   <li>维持长老周围的部族人数，避免灵魂族群因意外灭绝；
 *   <li>将所有生成逻辑限制在服务端，防止客户端重复执行。
 * </ul>
 */
public final class SoulClanSpawner {

  /** 每次尝试刷怪之间的冷却时长（单位：tick，默认 20 秒）。 */
  private static final int TICK_COOLDOWN = 20 * 20;

  /** 以长老为中心希望维持的族群规模（包含长老自身）。 */
  private static final int TARGET_POP_AROUND_ELDER = 8;

  /** 统计现有成员时使用的半径范围（曼哈顿意义上的 24 格近似）。 */
  private static final int SCAN_RADIUS = 24;

  /** 单次尝试在长老附近刷新的随机坐标次数上限。 */
  private static final int SPAWN_TRIES = 6;

  /** 剩余的冷却 tick；为 0 时允许再次尝试刷新族人。 */
  private static int cooldown;

  private static boolean registered;

  private SoulClanSpawner() {
    // 工具类不需要实例化。
  }

  /** 注册 SoulClan 刷怪逻辑到 NeoForge 事件总线，确保监听世界 tick。 */
  public static void init() {
    if (registered) {
      return;
    }
    NeoForge.EVENT_BUS.addListener(SoulClanSpawner::onLevelTick);
    registered = true;
  }

  /**
   * 每个服务器世界 tick 的收尾阶段都会触发该方法，用于执行保活逻辑。
   *
   * @param event Forge 在世界 tick 后触发的事件
   */
  public static void onLevelTick(LevelTickEvent.Post event) {
    Level level = event.getLevel();
    if (!(level instanceof ServerLevel serverLevel)) {
      // 仅在服务端运行，客户端 world tick 不执行任何逻辑。
      return;
    }
    if (!serverLevel
        .getGameRules()
        .getBoolean(net.tigereye.chestcavity.config.CCGameRules.SPAWN_FUN_ENTITIES)) {
      return;
    }
    if (cooldown > 0) {
      cooldown--;
      return;
    }

    // 1. 优先保证至少存在一名长老；缺失时在世界出生点直接生成。
    Optional<SoulClanEntity> elder = SoulClanManager.findElder(serverLevel);
    if (elder.isEmpty()) {
      if (spawnOne(serverLevel, serverLevel.getSharedSpawnPos(), SoulClanEntity.Variant.ELDER)) {
        cooldown = TICK_COOLDOWN;
      }
      return;
    }

    SoulClanEntity leader = elder.get();
    // 2. 统计长老附近的族人数量，仅统计仍然存活的实体。
    int currentPopulation =
        serverLevel
            .getEntitiesOfClass(
                SoulClanEntity.class, leader.getBoundingBox().inflate(SCAN_RADIUS), Entity::isAlive)
            .size();

    if (currentPopulation >= TARGET_POP_AROUND_ELDER) {
      // 长老周围人数充足，无需额外刷怪。
      return;
    }

    // 3. 按照概率选择要生成的变体，默认以守卫为主，兼顾商人与备用长老。
    SoulClanEntity.Variant variant = pickChildVariant(serverLevel.getRandom());
    if (spawnNear(serverLevel, leader.blockPosition(), variant)) {
      cooldown = TICK_COOLDOWN;
    }
  }

  /**
   * 在给定中心附近尝试多次生成 SoulClan 成员。
   *
   * @param level 当前服务器世界
   * @param center 随机偏移所基于的中心位置
   * @param variant 计划生成的变体类型
   * @return 至少一次生成成功时返回 true
   */
  private static boolean spawnNear(
      ServerLevel level, BlockPos center, SoulClanEntity.Variant variant) {
    RandomSource random = level.random;
    for (int attempt = 0; attempt < SPAWN_TRIES; attempt++) {
      int dx = random.nextInt(9) - 4; // [-4, 4]
      int dz = random.nextInt(9) - 4; // [-4, 4]
      BlockPos candidate =
          level.getHeightmapPos(
              Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, center.offset(dx, 0, dz));
      if (spawnOne(level, candidate, variant)) {
        return true;
      }
    }
    return false;
  }

  /**
   * 在指定位置生成一个 SoulClan 实体。
   *
   * @param level 当前服务器世界
   * @param position 生成坐标
   * @param variant 生成实体的变体
   * @return 若实体成功加入世界则返回 true
   */
  private static boolean spawnOne(
      ServerLevel level, BlockPos position, SoulClanEntity.Variant variant) {
    SoulClanEntity entity = CCEntities.SOUL_CLAN.get().create(level);
    if (entity == null) {
      return false;
    }
    entity.setVariant(variant);
    entity.moveTo(
        position.getX() + 0.5,
        position.getY(),
        position.getZ() + 0.5,
        level.random.nextFloat() * 360.0F,
        0.0F);
    return level.addFreshEntity(entity);
  }

  /**
   * 根据预设概率权重选择需要生成的族人类型。
   *
   * <p>当前权重设置：
   *
   * <ul>
   *   <li>守卫：60%
   *   <li>商人：35%
   *   <li>长老：5%
   * </ul>
   *
   * @param random 世界随机源
   * @return 按权重选出的变体
   */
  private static SoulClanEntity.Variant pickChildVariant(RandomSource random) {
    int roll = random.nextInt(100);
    if (roll < 60) {
      return SoulClanEntity.Variant.GUARD;
    }
    if (roll < 95) {
      return SoulClanEntity.Variant.TRADER;
    }
    return SoulClanEntity.Variant.ELDER;
  }
}
