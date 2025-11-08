package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.behavior.active;

import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.item.common.cost.ResourceCost;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.behavior.organ.JianSuoGuState;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.calculator.JianSuoCalc;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.runtime.jian_suo.JianSuoRuntime;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.tuning.JianSuoGuTuning;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 剑梭蛊主动技能实现：剑梭突进。
 *
 * <p>核心逻辑：
 * <ol>
 *   <li>决定突进方向（玩家：视线方向；NPC：目标或朝向）</li>
 *   <li>检查并消耗资源（真元/精力/念头）</li>
 *   <li>基于道痕计算突进距离与伤害</li>
 *   <li>执行突进与路径伤害（碰撞预测、友方过滤、去重）</li>
 *   <li>启动冷却</li>
 * </ol>
 */
public final class JianSuoGuActive {

  private static final Logger LOGGER = LoggerFactory.getLogger(JianSuoGuActive.class);

  private JianSuoGuActive() {}

  /**
   * 激活剑梭蛊能力。
   *
   * @param player 玩家
   * @param cc 玩家的胸腔实例
   * @param organ 剑梭蛊器官物品
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
        cooldown.entry(JianSuoGuState.KEY_READY_TICK).withDefault(0L);
    if (now < readyEntry.getReadyTick()) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("[JianSuoGuActive] On cooldown, remaining: {} ticks", readyEntry.getReadyTick() - now);
      }
      return false;
    }

    // 2. 读取道痕（从资源桥）
    Optional<GuzhenrenResourceBridge.ResourceHandle> handleOpt = ResourceOps.openHandle(player);
    if (handleOpt.isEmpty()) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("[JianSuoGuActive] No resource handle available");
      }
      return false;
    }

    GuzhenrenResourceBridge.ResourceHandle handle = handleOpt.get();
    double daohen = handle.getDouble("jiandao:daohen_jiandao").orElse(0.0);

    // 3. 决定方向
    Vec3 dir = determineDashDirection(player);
    if (dir.lengthSqr() < 0.01) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("[JianSuoGuActive] Invalid direction vector");
      }
      return false;
    }

    // 4. 计算参数
    double dashDist = JianSuoCalc.dashDistance(daohen);
    double velocity = player.getDeltaMovement().length();
    double damage = JianSuoCalc.pathDamage(daohen, velocity);
    int cdTicks = JianSuoCalc.activeCooldown(daohen);

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(
          "[JianSuoGuActive] daohen={}, dashDist={}, damage={}, cd={}",
          daohen, dashDist, damage, cdTicks);
    }

    // 5. 消耗资源
    ResourceCost cost = new ResourceCost(
        JianSuoGuTuning.BASE_COST_ZHENYUAN,
        JianSuoGuTuning.BASE_COST_JINGLI,
        JianSuoGuTuning.BASE_COST_NIANTOU
    );

    if (!ResourceOps.payCost(player, cost, "剑梭蛊")) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("[JianSuoGuActive] Insufficient resources");
      }
      return false;
    }

    // 6. 执行突进与路径伤害
    double actualDist = JianSuoRuntime.tryDashAndDamage(
        player,
        dir,
        dashDist,
        damage,
        JianSuoGuTuning.RAY_WIDTH,
        JianSuoGuTuning.MAX_DASH_STEPS,
        JianSuoGuTuning.HIT_ONCE_DEDUP_TICKS
    );

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("[JianSuoGuActive] Dashed {} blocks", actualDist);
    }

    // 7. 启动冷却
    readyEntry.setReadyAt(now + cdTicks);

    return true;
  }

  /**
   * 决定突进方向。
   *
   * <p>规则：
   * <ul>
   *   <li>玩家：视线方向的水平分量</li>
   *   <li>非玩家且有目标且距离近：朝向目标</li>
   *   <li>否则：当前朝向的水平分量</li>
   * </ul>
   *
   * @param entity 实体
   * @return 突进方向（归一化）
   */
  private static Vec3 determineDashDirection(LivingEntity entity) {
    // 玩家优先取视线方向
    if (entity instanceof ServerPlayer) {
      Vec3 lookAngle = entity.getLookAngle();
      Vec3 horizontal = new Vec3(lookAngle.x, 0, lookAngle.z);
      return horizontal.lengthSqr() > 0.01 ? horizontal.normalize() : lookAngle.normalize();
    }

    // NPC：优先朝向目标
    LivingEntity target = entity.getTarget();
    if (target != null && entity.distanceTo(target) <= JianSuoGuTuning.NPC_GOAL_LOCK_MAXDIST) {
      Vec3 toTarget = target.position().subtract(entity.position());
      Vec3 horizontal = new Vec3(toTarget.x, 0, toTarget.z);
      return horizontal.lengthSqr() > 0.01 ? horizontal.normalize() : toTarget.normalize();
    }

    // 否则取当前朝向
    Vec3 lookAngle = entity.getLookAngle();
    Vec3 horizontal = new Vec3(lookAngle.x, 0, lookAngle.z);
    return horizontal.lengthSqr() > 0.01 ? horizontal.normalize() : lookAngle.normalize();
  }
}
