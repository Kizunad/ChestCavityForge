package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.swarm;

import java.util.*;
import javax.annotation.Nullable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.FlyingSwordEntity;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.behavior.SeparationBehavior;

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
  private final Player owner;

  /** 剑群成员列表（按生成顺序） */
  private final List<FlyingSwordEntity> swords;

  /** 当前集群行为模式 */
  private SwarmBehaviorMode currentMode;

  /** 集群目标（敌对实体） */
  @Nullable private LivingEntity swarmTarget;

  /** 集群计时器（用于协调时序） */
  private int swarmTick;

  /** 攻击轮次（用于轮流攻击） */
  private int attackWave;

  public QingLianSwordSwarm(UUID swarmId, Player owner) {
    this.swarmId = swarmId;
    this.owner = owner;
    this.swords = new ArrayList<>();
    this.currentMode = SwarmBehaviorMode.LOTUS_GUARD;
    this.swarmTick = 0;
    this.attackWave = 0;
  }

  // ==================== 剑群成员管理 ====================

  /** 添加飞剑到集群 */
  public void addSword(FlyingSwordEntity sword) {
    if (!swords.contains(sword)) {
      swords.add(sword);
    }
  }

  /** 移除飞剑（剑被召回或销毁时） */
  public void removeSword(FlyingSwordEntity sword) {
    swords.remove(sword);
  }

  /** 获取剑群大小 */
  public int getSwarmSize() {
    return swords.size();
  }

  /** 清理已销毁的剑 */
  private void cleanupDeadSwords() {
    swords.removeIf(sword -> !sword.isAlive() || sword.isRemoved());
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
        && owner.distanceTo(swarmTarget) < 32.0) {
      return;
    }

    // 使用第一把剑作为搜索代表（集群共享目标）
    if (swords.isEmpty()) {
      swarmTarget = null;
      return;
    }

    FlyingSwordEntity representativeSword = swords.get(0);
    Vec3 searchCenter = owner.position();

    // 使用 TargetFinder 搜索敌对目标（优先敌方飞剑）
    swarmTarget =
        net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.behavior
            .TargetFinder.findNearestHostileForGuard(representativeSword, searchCenter, 32.0);

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
    Vec3 ownerPos = owner.position().add(0, 1.5, 0); // 玩家腰部高度
    int swordCount = swords.size();

    // 根据剑数量动态调整半径（剑越多，半径越小）
    double baseRadius = 3.0 - (swordCount / 32.0) * 1.5; // 3.0格 -> 1.5格

    // 极慢旋转（每秒约5度，减少抖动）
    double rotationSpeed = 0.003; // 弧度/tick (原来0.01，现在1/3速度)
    double globalRotation = swarmTick * rotationSpeed;

    for (int i = 0; i < swords.size(); i++) {
      FlyingSwordEntity sword = swords.get(i);

      // 计算剑在阵型中的位置
      double angle = (2.0 * Math.PI / swordCount) * i + globalRotation;

      // 3层莲花：内层(80%)、中层(100%)、外层(120%)
      int layer = i % 3;
      double radiusMult = 0.8 + layer * 0.2;
      double currentRadius = baseRadius * radiusMult;

      // 高度差：形成立体莲花（底层、中层、顶层）
      double heightOffset = (layer - 1) * 0.8; // -0.8, 0, +0.8

      // 计算目标位置
      double offsetX = Math.cos(angle) * currentRadius;
      double offsetZ = Math.sin(angle) * currentRadius;
      Vec3 targetPos = ownerPos.add(offsetX, heightOffset, offsetZ);

      // 计算速度向量（带平滑追踪）
      Vec3 toTarget = targetPos.subtract(sword.position());
      double distance = toTarget.length();

      // 平滑速度计算：距离越近速度越小，避免震荡
      // 使用平方根函数而非线性，让减速更平滑
      double speedFactor = Math.min(Math.sqrt(distance) * 0.2, 0.6); // 限制最大速度为0.6

      // 距离很近时停止移动，避免微小抖动
      if (distance < 0.15) {
        speedFactor = 0;
      }

      Vec3 desiredVelocity =
          toTarget.normalize().scale(sword.getSwordAttributes().speedBase * speedFactor);

      // 应用分离力，避免剑重叠（但不要在莲花阵中应用分离力，会破坏阵型）
      // desiredVelocity = SeparationBehavior.applySeparation(sword, desiredVelocity);

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
      // 没有目标，回到护卫模式
      switchMode(SwarmBehaviorMode.LOTUS_GUARD);
      tickLotusGuard();
      return;
    }

    Vec3 targetPos = swarmTarget.position().add(0, swarmTarget.getBbHeight() / 2, 0);
    Vec3 ownerPos = owner.position().add(0, 1.5, 0);

    // 每15 ticks派出一波剑（减少频率，更有序）
    int waveCycle = 15;
    int swordsPerWave = Math.max(1, swords.size() / 6); // 6波攻击（更少波次）

    for (int i = 0; i < swords.size(); i++) {
      FlyingSwordEntity sword = swords.get(i);

      // 计算这把剑属于哪一波
      int swordWave = i / swordsPerWave;
      int wavePhase = (swarmTick / waveCycle) % (swords.size() / swordsPerWave + 1);

      if (swordWave == wavePhase) {
        // 这把剑处于攻击波次
        Vec3 toTarget = targetPos.subtract(sword.position());
        double distance = toTarget.length();

        // 减小螺旋半径和速度，更平滑
        double spiralRadius = 0.5; // 原来0.8，现在更小
        double spiralSpeed = 0.15; // 原来0.3，现在慢一半
        double spiralAngle = swarmTick * spiralSpeed + i * 0.5;

        Vec3 forward = toTarget.normalize();
        Vec3 right = new Vec3(-forward.z, 0, forward.x).normalize();
        Vec3 up = forward.cross(right).normalize();

        Vec3 spiralOffset =
            right.scale(Math.cos(spiralAngle) * spiralRadius)
                .add(up.scale(Math.sin(spiralAngle) * spiralRadius));

        // 降低最大速度，更平滑
        Vec3 desiredVelocity =
            forward
                .scale(sword.getSwordAttributes().speedMax * 1.0) // 原来1.5，现在1.0
                .add(spiralOffset.scale(0.3)); // 原来0.5，现在0.3

        sword.applySteeringVelocity(desiredVelocity);
      } else {
        // 其他剑保持护卫位置
        double angle = (2.0 * Math.PI / swords.size()) * i;
        double radius = 2.5;
        Vec3 guardPos =
            ownerPos.add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);

        Vec3 toGuard = guardPos.subtract(sword.position());
        double distance = toGuard.length();

        // 平滑追踪护卫位置
        double speedFactor = Math.min(Math.sqrt(distance) * 0.25, 0.5);
        if (distance < 0.15) {
          speedFactor = 0;
        }

        Vec3 desiredVelocity =
            toGuard.normalize().scale(sword.getSwordAttributes().speedBase * speedFactor);

        sword.applySteeringVelocity(desiredVelocity);
      }
    }
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
        // 攻击组：直接追击目标
        Vec3 toTarget = targetPos.subtract(sword.position());
        Vec3 desiredVelocity =
            toTarget.normalize().scale(sword.getSwordAttributes().speedMax * 1.2);

        sword.applySteeringVelocity(desiredVelocity);
      } else {
        // 防守组：环绕主人
        double angle = (2.0 * Math.PI / (swords.size() / 2.0)) * (i / 2);
        double radius = 2.0;
        Vec3 guardPos =
            ownerPos.add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);

        Vec3 toGuard = guardPos.subtract(sword.position());
        Vec3 desiredVelocity =
            toGuard.normalize().scale(sword.getSwordAttributes().speedBase * 0.8);

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
        // 聚集阶段：从各个方向飞向目标
        Vec3 toTarget = targetPos.subtract(sword.position());
        Vec3 desiredVelocity =
            toTarget.normalize().scale(sword.getSwordAttributes().speedMax * 1.5);

        sword.applySteeringVelocity(desiredVelocity);
      } else {
        // 散开阶段：沿球面方向散开
        double scatterRadius = 4.0;
        Vec3 scatterPos = targetPos.add(direction.scale(scatterRadius));
        Vec3 toScatter = scatterPos.subtract(sword.position());

        Vec3 desiredVelocity =
            toScatter.normalize().scale(sword.getSwordAttributes().speedMax * 1.0);

        sword.applySteeringVelocity(desiredVelocity);
      }
    }
  }

  // ==================== Getters ====================

  public UUID getSwarmId() {
    return swarmId;
  }

  public Player getOwner() {
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
}
