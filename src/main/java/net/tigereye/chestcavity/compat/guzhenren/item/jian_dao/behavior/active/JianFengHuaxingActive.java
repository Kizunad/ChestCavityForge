package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.behavior.active;

import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.FlyingSwordEntity;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.FlyingSwordSpawner;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.FlyingSwordType;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.AIMode;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.item.common.cost.ResourceCost;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.fx.JianFengGuFx;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.tuning.JianFengGuTuning;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 剑锋蛊主动技能实现：锋芒化形。
 *
 * <p>核心逻辑：
 * <ol>
 *   <li>检查冷却状态</li>
 *   <li>根据器官转数（四转/五转）确定资源消耗和生成数量</li>
 *   <li>消耗真元/精力</li>
 *   <li>在身后生成飞剑，设置为随侍/环绕模式</li>
 *   <li>记录飞剑ID到器官状态</li>
 *   <li>设置主动态持续时间</li>
 *   <li>启动冷却</li>
 *   <li>播放激活特效</li>
 * </ol>
 */
public final class JianFengHuaxingActive {

  private static final Logger LOGGER = LoggerFactory.getLogger(JianFengHuaxingActive.class);

  private JianFengHuaxingActive() {}

  /**
   * 激活剑锋蛊能力。
   *
   * @param player 玩家
   * @param cc 玩家的胸腔实例
   * @param organ 剑锋蛊器官物品
   * @param state 器官状态
   * @param cooldown 冷却管理器
   * @param now 当前游戏时间（tick）
   * @return 是否成功激活
   */
  public static boolean activate(
      ServerPlayer player,
      ChestCavityInstance cc,
      ItemStack organ,
      OrganState state,
      MultiCooldown cooldown,
      long now) {

    ServerLevel level = player.serverLevel();

    // 1. 检查冷却
    MultiCooldown.Entry readyEntry =
        cooldown.entry("ready_tick").withDefault(0L);
    if (now < readyEntry.getReadyTick()) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(
            "[JianFengHuaxingActive] On cooldown, remaining: {} ticks",
            readyEntry.getReadyTick() - now);
      }
      return false;
    }

    // 2. 确定转数（四转/五转）
    ResourceLocation organId = BuiltInRegistries.ITEM.getKey(organ.getItem());
    boolean isFiveTurn = organId != null && organId.getPath().equals(JianFengGuTuning.ORGAN_ID_FIVE);
    int swordCount = isFiveTurn ? JianFengGuTuning.SPAWN_COUNT_FIVE : JianFengGuTuning.SPAWN_COUNT_FOUR;
    double zhenyuanCost = isFiveTurn ? JianFengGuTuning.COST_ZHENYUAN_FIVE : JianFengGuTuning.COST_ZHENYUAN_FOUR;

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(
          "[JianFengHuaxingActive] Organ: {}, isFiveTurn: {}, swordCount: {}, zhenyuanCost: {}",
          organId,
          isFiveTurn,
          swordCount,
          zhenyuanCost);
    }

    // 3. 读取道痕（用于后续计算）
    Optional<GuzhenrenResourceBridge.ResourceHandle> handleOpt = ResourceOps.openHandle(player);
    if (handleOpt.isEmpty()) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("[JianFengHuaxingActive] No resource handle available");
      }
      return false;
    }

    GuzhenrenResourceBridge.ResourceHandle handle = handleOpt.get();
    double daohen = handle.read("daohen_jiandao").orElse(0.0);

    // 4. 消耗资源
    ResourceCost cost =
        new ResourceCost(
            zhenyuanCost,
            JianFengGuTuning.COST_JINGLI,
            0.0, // hunpo
            0.0, // niantou
            0, // hunger
            0.0f // health
        );

    if (!ResourceOps.payCost(player, cost, "剑锋蛊")) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("[JianFengHuaxingActive] Insufficient resources");
      }
      return false;
    }

    // 5. 生成飞剑（在身后）
    Vec3 playerPos = player.position();
    Vec3 lookAngle = player.getLookAngle();
    // 计算身后位置：相对玩家朝向的反方向
    Vec3 backwardVec = lookAngle.scale(-1.0).normalize();
    Vec3 spawnPos = playerPos.add(
        backwardVec.x * JianFengGuTuning.SPAWN_OFFSET_Z,
        JianFengGuTuning.SPAWN_OFFSET_Y,
        backwardVec.z * JianFengGuTuning.SPAWN_OFFSET_Z
    );

    // 生成多把飞剑
    java.util.List<Integer> swordIds = new java.util.ArrayList<>();
    for (int i = 0; i < swordCount; i++) {
      // 稍微分散生成位置（环绕分布）
      double angle = (Math.PI * 2.0 * i) / swordCount;
      Vec3 offset = new Vec3(Math.cos(angle) * 0.5, 0, Math.sin(angle) * 0.5);
      Vec3 finalSpawnPos = spawnPos.add(offset);

      FlyingSwordEntity sword =
          FlyingSwordSpawner.spawn(
              level,
              player,
              finalSpawnPos,
              null, // 方向由飞剑AI控制
              organ, // 使用器官作为源物品
              FlyingSwordType.DEFAULT);

      if (sword != null) {
        // 设置为环绕/随侍模式（ORBIT = 环绕）
        sword.setAIMode(AIMode.ORBIT);
        swordIds.add(sword.getId());

        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("[JianFengHuaxingActive] Spawned sword {} at {}", sword.getId(), finalSpawnPos);
        }
      }
    }

    // 6. 记录飞剑ID到器官状态（用于后续协同突击）
    net.minecraft.nbt.ListTag swordIdList = new net.minecraft.nbt.ListTag();
    for (Integer id : swordIds) {
      net.minecraft.nbt.IntTag tag = net.minecraft.nbt.IntTag.valueOf(id);
      swordIdList.add(tag);
    }
    state.setList("spawned_sword_ids", swordIdList);

    // 7. 设置主动态持续时间
    long activeUntil = now + JianFengGuTuning.ACTIVE_BASE_DURATION_TICKS;
    state.setLong("active_until", activeUntil);

    // 8. 重置协同计数
    state.setInt("coop_count", 0);
    state.setLong("last_coop_tick", 0L);

    // 9. 启动冷却
    long readyAt = now + JianFengGuTuning.COOLDOWN_TICKS;
    readyEntry.setReadyAt(readyAt);

    // 10. 播放激活特效
    JianFengGuFx.playActivate(player);

    // 11. 发送冷却提示
    ResourceLocation abilityId = ResourceLocation.fromNamespaceAndPath(
        JianFengGuTuning.MOD_ID,
        JianFengGuTuning.ABILITY_ID
    );
    JianFengGuFx.scheduleCooldownToast(player, abilityId, readyAt, now);

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(
          "[JianFengHuaxingActive] Activated! Spawned {} swords, active until {}, cooldown until {}",
          swordIds.size(),
          activeUntil,
          readyAt);
    }

    return true;
  }
}
