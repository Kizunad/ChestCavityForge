package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.swarm;

import java.util.*;
import javax.annotation.Nullable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.FlyingSwordEntity;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.AIMode;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.behavior.SeparationBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.tuning.QingLianSwarmTuning;

/**
 * 青莲剑群集群AI管理器
 *
 * <p>负责协调2-32把青莲剑的集群行为，实现：
 * <ul>
 *   <li>莲花护卫阵 - 所有剑形成莲花瓣状环绕玩家</li>
 *   <li>螺旋攻击 - 剑依次螺旋飞向目标，形成连续打击</li>
 *   <li>轮流防御 - 部分剑护卫、部分剑攻击，定期轮换</li>
 *   <li>合击 - 多把剑同时从不同角度攻击同一目标</li>
 * </ul>
 */
public class QingLianSwordSwarm {

  /** 青莲剑群UUID（对应Domain的UUID） */
  private final UUID swarmId;

  /** 剑群主人 */
  private final LivingEntity owner;

  /** 剑群成员列表（按生成顺序） */
  private final List<FlyingSwordEntity> swords;

  private final int swarmGroupId;

  /** 当前集群行为模式 */
  private SwarmBehaviorMode currentMode;

  /** 集群目标（敌对实体） */
  @Nullable private LivingEntity swarmTarget;

  /** 集群计时器（用于协调时序） */
  private int swarmTick;

  /** 攻击轮次（用于轮流攻击） */
  private int attackWave;

  // —— 集群中心（鸟群式控制） ——
  private Vec3 swarmCenter = Vec3.ZERO; // 平滑后的中心
  private double centerOrbitAngle = 0.0; // 攻击模式：中心绕目标旋转角

  // —— 个体代理（每把剑的独立状态机） ——
  private enum AgentPhase { FORMATION, DEPART, ATTACK, RETURN }

  private static final class SwordAgent {
    final UUID id;
    final FlyingSwordEntity sword;
    AgentPhase phase = AgentPhase.FORMATION;
    int phaseTick = 0;
    long lastLaunchTick = -1000L;
    // 运行态缓存：最近一次"离队"的参照角度，提升观感
    double cachedSlotAngle = 0.0;

    SwordAgent(FlyingSwordEntity sword) {
      this.id = sword.getUUID();
      this.sword = sword;
    }
  }

  private final Map<UUID, SwordAgent> agents = new HashMap<>();

  // 目标选择的滞后与范围（避免边界抖动）
  private static final double ACQUIRE_RANGE = 32.0; // 新目标获取半径
  private static final double RETAIN_RANGE = 38.0;  // 保持锁定的半径（>获取半径，形成滞后）
  // IDLE 最大移动速度（绝对值，避免高属性时过快）
  private static final double IDLE_MAX_SPEED = 0.10;

  public QingLianSwordSwarm(UUID swarmId, LivingEntity owner) {
    this.swarmId = swarmId;
    this.owner = owner;
    this.swords = new ArrayList<>();
    this.currentMode = SwarmBehaviorMode.LOTUS_GUARD;
    this.swarmTick = 0;
    this.attackWave = 0;
    this.swarmGroupId = FlyingSwordEntity.SWARM_GROUP_ID;
  }

  // ==================== 剑群成员管理 ====================

  /** 添加飞剑到集群 */
  public void addSword(FlyingSwordEntity sword) {
    if (!swords.contains(sword)) {
      swords.add(sword);
      // 注册个体代理
      agents.put(sword.getUUID(), new SwordAgent(sword));
      sword.setGroupId(swarmGroupId);
    }
  }

  /** 移除飞剑（剑被召回或销毁时） */
  public void removeSword(FlyingSwordEntity sword) {
    swords.remove(sword);
    agents.remove(sword.getUUID());
    sword.setGroupId(0);
  }

  /** 获取剑群大小 */
  public int getSwarmSize() {
    return swords.size();
  }

  /** 清理已销毁的剑 */
  private void cleanupDeadSwords() {
    swords.removeIf(sword -> {
      boolean removed = !sword.isAlive() || sword.isRemoved();
      if (removed) {
        agents.remove(sword.getUUID());
      }
      return removed;
    });
  }

  // ==================== 集群AI主循环 ====================

  /**
   * 每tick更新集群行为
   *
   * <p>这是集群AI的核心方法，协调所有飞剑的行动
   */
  public void tick() {
    cleanupDeadSwords();

    if (swords.isEmpty()) {
      return;
    }

    swarmTick++;

    // 每20 ticks检查一次目标有效性
    if (swarmTick % 20 == 0) {
      updateSwarmTarget();
    }

    // 根据当前模式执行集群行为
    switch (currentMode) {
      case LOTUS_GUARD -> tickLotusGuard();
      case SPIRAL_ATTACK -> tickSpiralAttack();
      case ROTATING_DEFENSE -> tickRotatingDefense();
      case CONVERGING_STRIKE -> tickConvergingStrike();
    }
  }

  // ==================== 目标管理 ====================

  /** 更新集群目标（选择最近的敌对实体） */
  private void updateSwarmTarget() {
    // 如果当前目标仍然有效且在范围内，保持锁定
    if (swarmTarget != null
        && swarmTarget.isAlive()
        && owner.distanceTo(swarmTarget) < QingLianSwarmTuning.SWARM_TARGET_RETAIN_RANGE) {
      return;
    }

    // 使用第一把剑作为搜索代表（集群共享目标）
    if (swords.isEmpty()) {
      swarmTarget = null;
      return;
    }

    FlyingSwordEntity representativeSword = swords.get(0);
    Vec3 searchCenter = owner.position();

    // 使用 TargetFinder 搜索敌对目标（优先敌方飞剑），限定获取半径
    LivingEntity newTarget =
        net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.behavior
            .TargetFinder.findNearestHostileForGuard(
                representativeSword, searchCenter, QingLianSwarmTuning.SWARM_TARGET_ACQUIRE_RANGE);
    boolean targetChanged = (newTarget != swarmTarget);
    swarmTarget = newTarget;

    // 将目标同步到每把飞剑（碰撞攻击依赖飞剑自身target）
    if (targetChanged) {
      if (swarmTarget != null) {
        for (FlyingSwordEntity s : swords) {
          s.setTargetEntity(swarmTarget);
        }
      } else {
        for (FlyingSwordEntity s : swords) {
          s.setTargetEntity(null);
        }
      }
    }

    // 目标改变时，切换行为模式
    if (swarmTarget != null && currentMode == SwarmBehaviorMode.LOTUS_GUARD) {
      // 发现敌人，切换到攻击模式
      switchMode(SwarmBehaviorMode.SPIRAL_ATTACK);
    } else if (swarmTarget == null && currentMode != SwarmBehaviorMode.LOTUS_GUARD) {
      // 没有敌人，回到护卫模式
      switchMode(SwarmBehaviorMode.LOTUS_GUARD);
    }
  }

  /** 切换集群行为模式 */
  public void switchMode(SwarmBehaviorMode newMode) {
    if (currentMode != newMode) {
      currentMode = newMode;
      attackWave = 0; // 重置攻击波次
      net.tigereye.chestcavity.ChestCavity.LOGGER.info(
          "[QingLianSwarm] Mode switched to: {}", newMode);
      // 模式切换时，复位个体代理
      if (newMode == SwarmBehaviorMode.LOTUS_GUARD) {
        for (SwordAgent a : agents.values()) {
          a.phase = AgentPhase.FORMATION;
          a.phaseTick = 0;
        }
        // 清空中心绕转角，避免突兀跳变
        centerOrbitAngle = 0.0;
        // 清空个体目标
        for (FlyingSwordEntity s : swords) {
          s.setTargetEntity(null);
          // 回到护卫时统一切回 SWARM，由集群接管
          if (s.getAIMode() != AIMode.SWARM) {
            s.setAIMode(AIMode.SWARM);
          }
        }
      }
    }
  }

  // ==================== 集群行为模式实现 ====================

  /**
   * 莲花护卫阵 - 所有剑形成莲花瓣状环绕玩家
   *
   * <p>特点：
   * <ul>
   *   <li>剑均匀分布在圆周上</li>
   *   <li>形成3层立体莲花（内、中、外）</li>
   *   <li>缓慢旋转，营造仙侠氛围</li>
   * </ul>
   */
  private void tickLotusGuard() {
    // 中心锚定主人，平滑跟随
    Vec3 anchor = owner.position().add(0, 1.5, 0);
    if (swarmCenter == Vec3.ZERO) {
      swarmCenter = anchor;
    } else {
      double alpha = QingLianSwarmTuning.SWARM_CENTER_FOLLOW_SMOOTH;
      swarmCenter = swarmCenter.add(anchor.subtract(swarmCenter).scale(alpha));
    }

    int swordCount = swords.size();
    double formationRadius = computeFormationRadius(swordCount);
    // 需求：去除编队槽位自转，保持相对位置稳定
    double globalRotation = 0.0; // swarmTick * QingLianSwarmTuning.SWARM_GLOBAL_ROTATION_IDLE_SPEED;

    // 槽位稳定排序
    List<FlyingSwordEntity> ordered = new ArrayList<>(swords);
    ordered.sort(Comparator.comparing(FlyingSwordEntity::getUUID));

    for (int idx = 0; idx < ordered.size(); idx++) {
      FlyingSwordEntity sword = ordered.get(idx);
      SwordAgent agent = agents.get(sword.getUUID());
      if (agent == null) continue;

      // 去除槽位自转：不叠加 globalRotation
      double baseAngle = (2.0 * Math.PI / Math.max(1, swordCount)) * idx;
      agent.cachedSlotAngle = baseAngle;
      int layer = idx % 3;
      double radiusMult = 0.9 + (layer - 1) * 0.1;
      double currentRadius = formationRadius * Math.max(0.8, radiusMult);
      double heightOffset = (layer - 1) * QingLianSwarmTuning.SWARM_FORMATION_LAYER_HEIGHT;
      Vec3 slot =
          swarmCenter.add(
              Math.cos(baseAngle) * currentRadius,
              heightOffset,
              Math.sin(baseAngle) * currentRadius);

      // IDLE：全部编队，清空目标
      agent.phase = AgentPhase.FORMATION;
      agent.phaseTick = Math.max(0, agent.phaseTick - 1);
      sword.setTargetEntity(null);

      Vec3 desiredVelocity =
          arrive(
              sword,
              slot,
              QingLianSwarmTuning.SWARM_IDLE_MAX_SPEED,
              QingLianSwarmTuning.SWARM_FORMATION_ARRIVE_RADIUS,
              QingLianSwarmTuning.SWARM_FORMATION_STOP_RADIUS);
      sword.applySteeringVelocity(desiredVelocity);
    }
  }

  /**
   * 螺旋攻击 - 剑依次螺旋飞向目标
   *
   * <p>特点：
   * <ul>
   *   <li>剑按顺序依次攻击，形成连续打击</li>
   *   <li>螺旋轨迹，增加观赏性</li>
   *   <li>攻击后退回护卫位置</li>
   * </ul>
   */
  private void tickSpiralAttack() {
    if (swarmTarget == null || !swarmTarget.isAlive()) {
      switchMode(SwarmBehaviorMode.LOTUS_GUARD);
      tickLotusGuard();
      return;
    }

    // 中心固定在目标上空10格（按配置），去除中心绕行
    final double HEIGHT_OFFSET = QingLianSwarmTuning.SWARM_HEIGHT_OFFSET; // 默认=10.0
    Vec3 targetAnchor = swarmTarget.position();
    int swordCount = swords.size();
    double formationRadius = computeFormationRadius(swordCount);

    // 直接锚定到目标上方 HEIGHT_OFFSET，高度 = 目标Y + HEIGHT_OFFSET
    Vec3 desiredCenter = targetAnchor.add(0, HEIGHT_OFFSET, 0);
    if (swarmCenter == Vec3.ZERO) swarmCenter = desiredCenter;
    double cAlpha = 0.25; // 轻微平滑，避免突变
    swarmCenter = swarmCenter.add(desiredCenter.subtract(swarmCenter).scale(cAlpha));

    // 槽位与排序
    // 去除攻击阶段槽位自转
    double globalRotation = 0.0; // swarmTick * QingLianSwarmTuning.SWARM_GLOBAL_ROTATION_ATTACK_SPEED;
    List<FlyingSwordEntity> ordered = new ArrayList<>(swords);
    ordered.sort(Comparator.comparing(FlyingSwordEntity::getUUID));

    // 调度：周期性派出一把剑离队
    // 离队频率：0.1s ≈ 2 tick（20 TPS）
    int dispatchInterval = QingLianSwarmTuning.SWARM_DISPATCH_INTERVAL_TICKS;
    if (swarmTick % dispatchInterval == 0) {
      for (FlyingSwordEntity s : ordered) {
        SwordAgent a = agents.get(s.getUUID());
        if (a == null) continue;
        // 保证单体之间最小触发间隔≈0.1s，避免同一把剑被过度调度
        if (a.phase == AgentPhase.FORMATION
            && (swarmTick - a.lastLaunchTick) >= QingLianSwarmTuning.SWARM_MIN_LAUNCH_INTERVAL_TICKS) {
          a.phase = AgentPhase.DEPART;
          a.phaseTick = 0;
          a.lastLaunchTick = swarmTick;
          s.setTargetEntity(swarmTarget);
          break;
        }
      }
    }

    for (int idx = 0; idx < ordered.size(); idx++) {
      FlyingSwordEntity sword = ordered.get(idx);
      SwordAgent agent = agents.get(sword.getUUID());
      if (agent == null) continue;

      // 去除槽位自转：不叠加 globalRotation
      double baseAngle = (2.0 * Math.PI / Math.max(1, swordCount)) * idx;
      agent.cachedSlotAngle = baseAngle;
      int layer = idx % 3;
      double layerRadius = formationRadius * (0.9 + (layer - 1) * 0.1);
      double heightOffset = (layer - 1) * QingLianSwarmTuning.SWARM_FORMATION_LAYER_HEIGHT;
      Vec3 slot =
          swarmCenter.add(
              Math.cos(baseAngle) * Math.max(0.8, layerRadius),
              heightOffset,
              Math.sin(baseAngle) * Math.max(0.8, layerRadius));

      switch (agent.phase) {
        case FORMATION -> {
          sword.setTargetEntity(null);
          Vec3 v = arrive(sword, slot, sword.getSwordAttributes().speedBase, 2.5, 0.15);
          sword.applySteeringVelocity(v);
          // 固定朝向：让每把剑“看向”当前目标（若存在）
          if (swarmTarget != null && swarmTarget.isAlive()) {
            Vec3 lookDir = swarmTarget.position().add(0, swarmTarget.getBbHeight() * 0.5, 0)
                .subtract(sword.position());
            if (lookDir.lengthSqr() > 1.0e-6) {
              double yaw = Math.toDegrees(Math.atan2(lookDir.x, lookDir.z));
              double horiz = Math.sqrt(lookDir.x * lookDir.x + lookDir.z * lookDir.z);
              double pitch = Math.toDegrees(Math.atan2(-lookDir.y, horiz));
              sword.setYRot((float) yaw);
              sword.setXRot((float) pitch);
            }
          }
          agent.phaseTick = Math.max(0, agent.phaseTick - 1);
        }
        case DEPART -> {
          Vec3 radial = new Vec3(Math.cos(baseAngle), 0, Math.sin(baseAngle));
          double departDist =
              Math.max(
                  QingLianSwarmTuning.SWARM_DEPART_MIN_DIST,
                  formationRadius + QingLianSwarmTuning.SWARM_DEPART_EXTRA_DIST);
          Vec3 departPoint = swarmCenter.add(radial.scale(departDist)).add(0, heightOffset, 0);
          Vec3 v =
              arrive(
                  sword,
                  departPoint,
                  sword.getSwordAttributes().speedMax,
                  QingLianSwarmTuning.SWARM_DEPART_ARRIVE_RADIUS,
                  QingLianSwarmTuning.SWARM_DEPART_STOP_RADIUS);
          sword.applySteeringVelocity(v);

          agent.phaseTick++;
          double dist = sword.position().distanceTo(departPoint);
          if (dist < QingLianSwarmTuning.SWARM_DEPART_REACH_EPS
              || agent.phaseTick > QingLianSwarmTuning.SWARM_DEPART_TIMEOUT_TICKS) {
            agent.phase = AgentPhase.ATTACK;
            agent.phaseTick = 0;
            // 离队进入自主AI：切到 HUNT，并指向当前集群目标
            sword.setTargetEntity(swarmTarget);
            sword.setAIMode(AIMode.HUNT);
          }
        }
        case ATTACK -> {
          // 由单剑AI（HUNT）驱动，集群仅监控时机
          if (sword.getAIMode() != AIMode.HUNT) {
            sword.setAIMode(AIMode.HUNT);
            sword.setTargetEntity(swarmTarget);
          }

          agent.phaseTick++;
          double d = swarmTarget != null ? sword.distanceTo(swarmTarget) : Double.MAX_VALUE;
          // 去除“攻击超时回归”：仅命中或目标失效时进入归队
          if (d < QingLianSwarmTuning.SWARM_ATTACK_HIT_RANGE
              || swarmTarget == null
              || !swarmTarget.isAlive()) {
            agent.phase = AgentPhase.RETURN;
            agent.phaseTick = 0;
            // 收队：切回 SWARM，清空目标，交回集群控制
            sword.setTargetEntity(null);
            sword.setAIMode(AIMode.SWARM);
          }
        }
        case RETURN -> {
          Vec3 v =
              arrive(
                  sword,
                  slot,
                  sword.getSwordAttributes().speedMax * 0.9,
                  QingLianSwarmTuning.SWARM_RETURN_ARRIVE_RADIUS,
                  QingLianSwarmTuning.SWARM_RETURN_STOP_RADIUS);
          sword.applySteeringVelocity(v);
          agent.phaseTick++;
          if (sword.position().distanceTo(slot) < QingLianSwarmTuning.SWARM_RETURN_REACH_EPS
              || agent.phaseTick > QingLianSwarmTuning.SWARM_RETURN_TIMEOUT_TICKS) {
            agent.phase = AgentPhase.FORMATION;
            agent.phaseTick = 0;
          }
        }
      }
    }
  }

  // 依据队伍规模计算编队半径
  private double computeFormationRadius(int count) {
    if (count <= 0) return 2.0;
    // 2把≈3.0格，线性压缩到32把≈1.6格
    double t = Math.min(1.0, count / 32.0);
    return 3.0 - 1.4 * t;
  }

  /**
   * 轮流防御 - 部分剑护卫、部分剑攻击
   *
   * <p>特点：
   * <ul>
   *   <li>50%剑环绕主人，50%剑追击敌人</li>
   *   <li>每60 ticks轮换角色</li>
   *   <li>平衡防守与进攻</li>
   * </ul>
   */
  private void tickRotatingDefense() {
    if (swarmTarget == null) {
      tickLotusGuard();
      return;
    }

    Vec3 targetPos = swarmTarget.position().add(0, swarmTarget.getBbHeight() / 2, 0);
    Vec3 ownerPos = owner.position().add(0, 1.5, 0);

    // 每60 ticks轮换
    int rotationCycle = 60;
    int currentWave = (swarmTick / rotationCycle) % 2;

    for (int i = 0; i < swords.size(); i++) {
      FlyingSwordEntity sword = swords.get(i);

      // 奇偶分组，轮流攻击/防守
      boolean isAttacking = (i % 2) == currentWave;

      if (isAttacking) {
        // 攻击组：基于 arrive 的追击（平滑逼近）
        Vec3 desiredVelocity =
            arrive(sword, targetPos, /*maxSpeed*/ sword.getSwordAttributes().speedMax,
                /*arriveRadius*/ 2.0, /*stopRadius*/ 0.15);
        sword.applySteeringVelocity(desiredVelocity);
      } else {
        // 防守组：环绕主人
        double angle = (2.0 * Math.PI / (swords.size() / 2.0)) * (i / 2);
        double radius = 2.0;
        Vec3 guardPos =
            ownerPos.add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);

        Vec3 desiredVelocity =
            arrive(sword, guardPos, /*maxSpeed*/ sword.getSwordAttributes().speedBase,
                /*arriveRadius*/ 2.5, /*stopRadius*/ 0.15);
        desiredVelocity = SeparationBehavior.applySeparation(sword, desiredVelocity);
        sword.applySteeringVelocity(desiredVelocity);
      }
    }
  }

  /**
   * 合击 - 多把剑同时从不同角度攻击
   *
   * <p>特点：
   * <ul>
   *   <li>所有剑同时从不同方向飞向目标</li>
   *   <li>形成球形包围，无死角打击</li>
   *   <li>攻击后散开再次聚集</li>
   * </ul>
   */
  private void tickConvergingStrike() {
    if (swarmTarget == null) {
      tickLotusGuard();
      return;
    }

    Vec3 targetPos = swarmTarget.position().add(0, swarmTarget.getBbHeight() / 2, 0);

    // 合击周期：40 ticks聚集 -> 20 ticks散开 -> 重复
    int convergeCycle = 60;
    int phase = swarmTick % convergeCycle;

    for (int i = 0; i < swords.size(); i++) {
      FlyingSwordEntity sword = swords.get(i);

      // 计算剑在球面上的分布（斐波那契球面分布）
      double goldenRatio = (1.0 + Math.sqrt(5.0)) / 2.0;
      double theta = 2.0 * Math.PI * i / goldenRatio;
      double phi = Math.acos(1.0 - 2.0 * (i + 0.5) / swords.size());

      double x = Math.cos(theta) * Math.sin(phi);
      double y = Math.cos(phi);
      double z = Math.sin(theta) * Math.sin(phi);
      Vec3 direction = new Vec3(x, y, z);

      if (phase < 40) {
        // 聚集阶段：基于 arrive 逼近
        Vec3 desiredVelocity =
            arrive(sword, targetPos, /*maxSpeed*/ sword.getSwordAttributes().speedMax,
                /*arriveRadius*/ 2.0, /*stopRadius*/ 0.15);
        sword.applySteeringVelocity(desiredVelocity);
      } else {
        // 散开阶段：沿球面方向散开
        double scatterRadius = 4.0;
        Vec3 scatterPos = targetPos.add(direction.scale(scatterRadius));
        Vec3 desiredVelocity =
            arrive(sword, scatterPos, /*maxSpeed*/ sword.getSwordAttributes().speedMax,
                /*arriveRadius*/ 3.0, /*stopRadius*/ 0.15);
        sword.applySteeringVelocity(desiredVelocity);
      }
    }
  }

  // ==================== 简化的平滑“到达/减速” ====================
  /**
   * 统一的到达/减速速度计算：
   * - 距离小于 stopRadius 则停下（去抖动）
   * - 距离在 [stopRadius, arriveRadius] 内按比例减速
   * - 距离大于 arriveRadius 以 maxSpeed 追击
   */
  private Vec3 arrive(
      FlyingSwordEntity sword, Vec3 targetPos, double maxSpeed, double arriveRadius,
      double stopRadius) {
    Vec3 toTarget = targetPos.subtract(sword.position());
    double dist = toTarget.length();
    if (dist < stopRadius) {
      return Vec3.ZERO;
    }

    double t = dist >= arriveRadius ? 1.0 : Math.max(0.0, (dist - stopRadius) / Math.max(1e-6, (arriveRadius - stopRadius)));
    // 在 base 与 max 之间插值，近处更慢，远处更快；并对 base 与结果施加不超过 maxSpeed 的硬上限
    double base = Math.min(sword.getSwordAttributes().speedBase, maxSpeed);
    double desiredSpeed = base + (maxSpeed - base) * t;
    desiredSpeed = Math.min(desiredSpeed, maxSpeed);
    return toTarget.normalize().scale(desiredSpeed);
  }

  // ==================== Getters ====================

  public UUID getSwarmId() {
    return swarmId;
  }

  public LivingEntity getOwner() {
    return owner;
  }

  public List<FlyingSwordEntity> getSwords() {
    return Collections.unmodifiableList(swords);
  }

  public SwarmBehaviorMode getCurrentMode() {
    return currentMode;
  }

  @Nullable
  public LivingEntity getSwarmTarget() {
    return swarmTarget;
  }

  /**
   * 外部指令：将集群切换为攻击指定目标。
   *
   * <p>会立即设置内部目标并切到攻击模式，同时同步每把飞剑的个体目标，
   * 以便碰撞攻击逻辑立刻生效。
   */
  public boolean commandAttack(@Nullable LivingEntity target) {
    if (target == null || !target.isAlive()) {
      return false;
    }
    this.swarmTarget = target;
    for (FlyingSwordEntity s : swords) {
      s.setTargetEntity(target);
    }
    switchMode(SwarmBehaviorMode.SPIRAL_ATTACK);
    return true;
  }
}
