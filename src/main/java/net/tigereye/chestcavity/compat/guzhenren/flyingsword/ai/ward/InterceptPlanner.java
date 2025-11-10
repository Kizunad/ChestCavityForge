package net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.ward;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.FlyingSwordEntity;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.integration.ward.WardTuning;
import org.jetbrains.annotations.Nullable;

/**
 * 拦截规划算法（纯函数式，无状态）
 *
 * <p>职责：
 *
 * <ul>
 *   <li>从威胁信息推导出拦截点 P* 与预计命中时刻
 *   <li>验证是否在时间窗口 [windowMin, windowMax] 内
 *   <li>不包含具体的寻路或避让逻辑（那部分由运动系统处理）
 * </ul>
 *
 * <h3>算法概览</h3>
 *
 * <ol>
 *   <li><b>投射物威胁</b>：根据位置、速度、重力预测与目标 AABB 的相交点
 *   <li><b>近战威胁</b>：构造攻击线段，取与目标 AABB 最近的点
 *   <li><b>拦截点偏移</b>：在预测命中点前方 0.3m，确保有效拦截
 *   <li><b>时间窗口验证</b>：只有在 [0.1, 1.0] 秒内可到达的威胁才会返回有效查询
 * </ol>
 *
 * <h3>设计原则</h3>
 *
 * <ul>
 *   <li>所有方法为静态方法，无状态
 *   <li>返回 null 表示无法拦截（超出时间窗或无解）
 *   <li>算法应高效，避免复杂迭代（最多 20 次）
 * </ul>
 */
public final class InterceptPlanner {

  /** 私有构造函数，防止实例化 */
  private InterceptPlanner() {
    throw new AssertionError("InterceptPlanner should not be instantiated");
  }

  // ====== 主规划方法 ======

  /**
   * 生成拦截查询
   *
   * <p>算法流程：
   *
   * <ol>
   *   <li>判断威胁类型（投射物 vs 近战）
   *   <li>调用相应的预测方法获得命中点 I：
   *       <ul>
   *         <li>投射物：{@link #predictProjectileHitPoint}
   *         <li>近战：{@link #predictMeleeHitPoint}
   *       </ul>
   *   <li>从命中点推导拦截点 P*：
   *       <pre>P* = I - 0.3 * normalize(velocity)</pre>
   *   <li>计算预计命中时刻 tImpact（秒）
   *   <li>验证是否在时间窗内（{@link WardTuning#windowMin()} ~ {@link WardTuning#windowMax()}）
   *   <li>返回 {@link InterceptQuery} 或 null
   * </ol>
   *
   * <h3>骨架阶段实现</h3>
   *
   * 此方法在 A 阶段仅返回 null（骨架），具体实现在 B 阶段完成。
   *
   * @param threat 威胁信息
   * @param owner 护幕主人（玩家）
   * @param tuning 参数接口
   * @return 拦截查询，或 null 若无法在窗口内拦截
   */
  public static @Nullable InterceptQuery plan(
      IncomingThreat threat, Player owner, WardTuning tuning) {
    if (threat == null || owner == null || tuning == null) {
      return null;
    }

    // 适配到 TargetView，复用无实体依赖的实现
    TargetView view = new TargetView() {
      @Override
      public Vec3 position() {
        return owner.position();
      }

      @Override
      public AABB getBoundingBox() {
        return owner.getBoundingBox();
      }

      @Override
      public double getBbHeight() {
        return owner.getBbHeight();
      }
    };

    return plan(threat, view, tuning);
  }

  /**
   * 无实体依赖版本，便于测试。
   */
  public static @Nullable InterceptQuery plan(
      IncomingThreat threat, TargetView owner, WardTuning tuning) {
    if (threat == null || owner == null || tuning == null) {
      return null;
    }

    // 根据投射物是否存在竖直速度，选择是否考虑重力
    final double GRAVITY = (threat.velocity() != null && Math.abs(threat.velocity().y) > 1e-6) ? 20.0 : 0.0;
    final double MELEE_REACH = 3.0; // 近战攻击范围

    Vec3 hitPoint = null;
    double tImpact = 0.0;

    // 步骤1: 判断威胁类型并预测命中点
    if (threat.isProjectile()) {
      // 投射物威胁：预测轨迹与目标AABB的相交点
      hitPoint = predictProjectileHitPoint(threat.position(), threat.velocity(), owner, GRAVITY);

      if (hitPoint != null) {
        // 计算投射物到达命中点的时间
        double distance = threat.position().distanceTo(hitPoint);
        double speedPerTick = threat.velocity().length(); // blocks/tick
        double speedPerSecond = speedPerTick * 20.0; // 转换为 m/s
        if (speedPerSecond > 0.02) { // 0.001 * 20 = 0.02
          tImpact = distance / speedPerSecond; // 秒
          if (distance < 1e-6) {
            // 初始即在AABB内，按最小时间窗处理
            tImpact = Math.max(tuning.windowMin(), tImpact);
          }
        } else {
          // 速度太慢或为0，无法拦截
          return null;
        }
      }

    } else if (threat.isMelee()) {
      // 近战威胁：预测攻击线段与目标的相交点
      if (threat.attacker() != null) {
        hitPoint = predictMeleeHitPoint(threat.attacker(), owner, MELEE_REACH);
      } else {
        // 测试场景：使用几何数据（attacker 为空时）
        Vec3 attackerEye = threat.position();
        hitPoint = predictMeleeHitPoint(attackerEye, owner, MELEE_REACH);
      }

      if (hitPoint != null) {
        // 近战的到达时间基于攻击者移动速度
        // 简化处理：假设攻击者瞬间到达（或使用攻击者当前速度）
        Vec3 attackerVel = threat.velocity();
        double attackerSpeed = threat.speed() * 20.0; // threat.speed 为 blocks/tick

        if (attackerSpeed <= 2.0) { // 慢速/静止，采用固定快速响应
          // 攻击者静止或速度很慢，假设0.2秒内攻击
          tImpact = 0.2;
        } else {
          Vec3 attackerPos = threat.attacker() != null ? threat.attacker().position() : threat.position();
          double distance = attackerPos.distanceTo(hitPoint);
          tImpact = distance / attackerSpeed;
        }
      }
    }

    // 步骤2: 如果无法预测命中点，返回null
    if (hitPoint == null) {
      return null;
    }

    // 步骤3: 从命中点推导拦截点P*
    // P* = I - offset * normalize(velocity_direction)
    Vec3 interceptPoint;
    if (threat.isProjectile() && threat.velocity() != null) {
      Vec3 direction = threat.velocity().normalize();
      interceptPoint = hitPoint.subtract(direction.scale(0.3)); // 提前0.3m
    } else {
      // 近战：拦截点就是命中点
      interceptPoint = hitPoint;
    }

    // 步骤4: 验证时间窗口
    double windowMin = tuning.windowMin();
    double windowMax = tuning.windowMax();

    if (tImpact < windowMin || tImpact > windowMax) {
      // 超出时间窗，无法拦截
      return null;
    }

    // 步骤5: 返回拦截查询
    return new InterceptQuery(interceptPoint, tImpact, threat);
  }

  // ====== 辅助方法 ======

  /**
   * 计算飞剑到达拦截点所需的时间
   *
   * <p>公式：
   *
   * <pre>
   * tReach = max(reaction_delay, distance / vMax)
   * </pre>
   *
   * 其中：
   *
   * <ul>
   *   <li>distance = 飞剑到 P* 的距离（米）
   *   <li>vMax = 飞剑最大速度（米/秒）
   *   <li>reaction_delay = 反应延迟（秒）
   * </ul>
   *
   * <h3>骨架阶段实现</h3>
   *
   * 此方法在 A 阶段返回简单计算，B 阶段完善公式。
   *
   * @param sword 飞剑实体
   * @param pStar 拦截点
   * @param tuning 参数接口
   * @return 所需时间（秒）
   */
  public static double timeToReach(FlyingSwordEntity sword, Vec3 pStar, WardTuning tuning) {
    if (sword == null || pStar == null || tuning == null) {
      return Double.MAX_VALUE;
    }

    // 检查主人是否存在，避免空指针异常
    var owner = sword.getOwner();
    if (owner == null) {
      return Double.MAX_VALUE;
    }

    double distance = sword.position().distanceTo(pStar);
    double vMax = tuning.vMax(owner.getUUID());
    double reaction = tuning.reactionDelay(owner.getUUID());

    // 防止除零
    if (vMax <= 0.0) {
      return Double.MAX_VALUE;
    }

    double tByDistance = distance / vMax;
    return Math.max(reaction, tByDistance);
  }

  /**
   * 无实体依赖版本：由测试直接提供 {@code swordPos} 与 {@code ownerId}。
   */
  public static double timeToReach(Vec3 swordPos, Vec3 pStar, java.util.UUID ownerId,
      WardTuning tuning) {
    if (swordPos == null || pStar == null || ownerId == null || tuning == null) {
      return Double.MAX_VALUE;
    }
    double distance = swordPos.distanceTo(pStar);
    double vMax = tuning.vMax(ownerId);
    double reaction = tuning.reactionDelay(ownerId);
    if (vMax <= 0.0) {
      return Double.MAX_VALUE;
    }
    double tByDistance = distance / vMax;
    return Math.max(reaction, tByDistance);
  }

  // ====== 预测方法（骨架阶段仅签名） ======

  /**
   * 预测投射物与目标 AABB 的相交点
   *
   * <p>算法伪代码：
   *
   * <pre>
   * 迭代 t 从 0 到 1.0s，步长 0.05s：
   *   预测位置 = projPos + projVel * t + gravity * t²
   *   if 预测位置与 target.getBoundingBox() 相交：
   *     return 预测位置
   * return null（无相交）
   * </pre>
   *
   * <h3>骨架阶段实现</h3>
   *
   * 此方法在 A 阶段返回 null，B 阶段实现完整逻辑。
   *
   * @param projPos 投射物当前位置
   * @param projVel 投射物速度向量
   * @param target 目标（玩家）
   * @param gravity 重力加速度（米/秒²，通常为 9.8 或 Minecraft 的重力值）
   * @return 预测的命中点，或 null 若无相交
   */
  private static @Nullable Vec3 predictProjectileHitPoint(
      Vec3 projPos, Vec3 projVel, TargetView target, double gravity) {
    if (projPos == null || projVel == null || target == null) {
      return null;
    }

    AABB targetBox = target.getBoundingBox();

    // 单位转换：projVel 是 blocks/tick，需要转换为 m/s
    // 1 tick = 1/20 秒，所以 v(m/s) = v(blocks/tick) * 20
    Vec3 velPerSecond = projVel.scale(20.0);

    // Minecraft gravity: 0.05 blocks/tick² = 0.05 * 20² = 20 m/s²
    Vec3 gravityVec = new Vec3(0, -gravity, 0);

    // 迭代预测：从 0 到 1.0s，步长 0.05s（共20次迭代）
    double maxTime = 1.0; // 秒
    double stepSize = 0.01; // 秒（更细粒度，提升相交检测精度）

    for (double t = 0.0; t <= maxTime; t += stepSize) {
      // 二次轨迹公式：P(t) = P₀ + v₀*t + 0.5*g*t²
      // 现在 velPerSecond 和 gravity 都是 m/s 单位
      Vec3 predPos = projPos.add(velPerSecond.scale(t)).add(gravityVec.scale(0.5 * t * t));

      // 检测是否与目标AABB相交
      if (isPointInAABB(predPos, targetBox)) {
        return predPos;
      }

      // 也检查从上一点到当前点的线段是否穿过AABB（更精确）
      if (t > 0) {
        double prevT = t - stepSize;
        Vec3 prevPos =
            projPos.add(velPerSecond.scale(prevT)).add(gravityVec.scale(0.5 * prevT * prevT));

        // 使用精确的线段-AABB 相交检测（slab 算法）
        Vec3 hit = segmentAabbIntersection(prevPos, predPos, targetBox);
        if (hit != null) {
          return hit;
        }
      }
    }

    return null; // 无相交
  }

  /**
   * 预测近战攻击线段与目标的相交点
   *
   * <p>算法伪代码：
   *
   * <pre>
   * 构造线段：attacker.getEyePosition() → target.position()
   * 计算线段到 target.getBoundingBox() 的最近点 I
   * return I
   * </pre>
   *
   * <h3>骨架阶段实现</h3>
   *
   * 此方法在 A 阶段返回 null，B 阶段实现完整逻辑。
   *
   * @param attacker 攻击者实体
   * @param target 目标（玩家）
   * @param reach 攻击范围（米，通常为 3.0）
   * @return 预测的命中点，或 null 若超出范围
   */
  private static @Nullable Vec3 predictMeleeHitPoint(Entity attacker, TargetView target, double reach) {
    if (attacker == null || target == null) {
      return null;
    }

    // 构造攻击线段：从攻击者的眼睛位置到目标的中心位置
    Vec3 attackerEye = attacker.getEyePosition();
    Vec3 targetCenter = target.position().add(0, target.getBbHeight() / 2.0, 0);

    // 检查距离是否在攻击范围内
    double distance = attackerEye.distanceTo(targetCenter);
    if (distance > reach) {
      return null; // 超出攻击范围
    }

    // 计算线段到目标AABB的最近点
    AABB targetBox = target.getBoundingBox();

    // 先检查攻击者是否已经在AABB内（近距离攻击）
    if (isPointInAABB(attackerEye, targetBox)) {
      return attackerEye;
    }

    // 计算从攻击者指向目标的方向
    Vec3 direction = targetCenter.subtract(attackerEye).normalize();
    Vec3 attackPoint = attackerEye.add(direction.scale(reach));

    // 找到线段与AABB的最近点
    Vec3 closestOnBox = closestPointOnAABB(targetCenter, targetBox);

    // 如果目标中心点不在盒子内，则使用线段到盒子的最近点
    if (!isPointInAABB(targetCenter, targetBox)) {
      // 计算线段到AABB各个面的最近点
      closestOnBox = closestPointOnSegmentToAABB(attackerEye, attackPoint, targetBox);
    }

    return closestOnBox;
  }

  /**
   * 预测近战攻击线段与目标的相交点（几何数据版本）。
   */
  private static @Nullable Vec3 predictMeleeHitPoint(Vec3 attackerEye, TargetView target, double reach) {
    if (attackerEye == null || target == null) {
      return null;
    }

    Vec3 targetCenter = target.position().add(0, target.getBbHeight() / 2.0, 0);

    double distance = attackerEye.distanceTo(targetCenter);
    if (distance > reach) {
      return null;
    }

    AABB targetBox = target.getBoundingBox();
    if (isPointInAABB(attackerEye, targetBox)) {
      return attackerEye;
    }

    Vec3 direction = targetCenter.subtract(attackerEye).normalize();
    Vec3 attackPoint = attackerEye.add(direction.scale(reach));
    Vec3 boxCenter = new Vec3((targetBox.minX + targetBox.maxX) / 2.0,
        (targetBox.minY + targetBox.maxY) / 2.0,
        (targetBox.minZ + targetBox.maxZ) / 2.0);

    Vec3 closestOnSegment = closestPointOnSegment(attackerEye, attackPoint, boxCenter);
    return closestPointOnAABB(closestOnSegment, targetBox);
  }

  // ====== 几何工具方法（骨架阶段仅签名） ======

  /**
   * 计算点到 AABB 的最近点
   *
   * <p>用于判定投射物或线段是否与目标碰撞
   *
   * @param point 查询点
   * @param box 边界盒
   * @return 边界盒上距离查询点最近的点
   */
  private static Vec3 closestPointOnAABB(Vec3 point, AABB box) {
    double x = Math.max(box.minX, Math.min(point.x, box.maxX));
    double y = Math.max(box.minY, Math.min(point.y, box.maxY));
    double z = Math.max(box.minZ, Math.min(point.z, box.maxZ));
    return new Vec3(x, y, z);
  }

  /**
   * 计算线段到点的最近点
   *
   * <p>用于近战攻击线段的最近点计算
   *
   * @param lineStart 线段起点
   * @param lineEnd 线段终点
   * @param point 查询点
   * @return 线段上距离查询点最近的点
   */
  private static Vec3 closestPointOnSegment(Vec3 lineStart, Vec3 lineEnd, Vec3 point) {
    Vec3 segment = lineEnd.subtract(lineStart);
    Vec3 toPoint = point.subtract(lineStart);

    double segmentLengthSq = segment.lengthSqr();
    if (segmentLengthSq < 1e-6) {
      // 线段退化为点
      return lineStart;
    }

    double t = toPoint.dot(segment) / segmentLengthSq;
    t = Math.max(0.0, Math.min(1.0, t)); // 限制在 [0, 1]

    return lineStart.add(segment.scale(t));
  }

  /**
   * 计算线段到AABB的最近点
   *
   * <p>用于近战攻击线段与目标盒子的碰撞检测
   *
   * @param lineStart 线段起点
   * @param lineEnd 线段终点
   * @param box 边界盒
   * @return AABB上距离线段最近的点
   */
  private static Vec3 closestPointOnSegmentToAABB(Vec3 lineStart, Vec3 lineEnd, AABB box) {
    // 获取AABB中心点
    Vec3 boxCenter =
        new Vec3(
            (box.minX + box.maxX) / 2.0, (box.minY + box.maxY) / 2.0, (box.minZ + box.maxZ) / 2.0);

    // 计算线段到盒子中心的最近点
    Vec3 closestOnSegment = closestPointOnSegment(lineStart, lineEnd, boxCenter);

    // 计算该点到AABB的最近点
    return closestPointOnAABB(closestOnSegment, box);
  }

  /**
   * 线段与 AABB 的相交检测（slab 算法）。
   * 返回进入交点（若命中），否则返回 null。
   */
  private static @Nullable Vec3 segmentAabbIntersection(Vec3 p0, Vec3 p1, AABB box) {
    Vec3 d = p1.subtract(p0);
    double tMin = 0.0;
    double tMax = 1.0;

    double[] p0Arr = {p0.x, p0.y, p0.z};
    double[] dArr = {d.x, d.y, d.z};
    double[] minArr = {box.minX, box.minY, box.minZ};
    double[] maxArr = {box.maxX, box.maxY, box.maxZ};

    for (int i = 0; i < 3; i++) {
      double pi = p0Arr[i];
      double di = dArr[i];
      double min = minArr[i];
      double max = maxArr[i];

      if (Math.abs(di) < 1e-12) {
        // 线段在该轴平行，若起点在盒子外则无交点
        if (pi < min || pi > max) {
          return null;
        }
      } else {
        double invD = 1.0 / di;
        double t1 = (min - pi) * invD;
        double t2 = (max - pi) * invD;
        if (t1 > t2) {
          double tmp = t1; t1 = t2; t2 = tmp;
        }
        tMin = Math.max(tMin, t1);
        tMax = Math.min(tMax, t2);
        if (tMin > tMax) {
          return null;
        }
      }
    }

    Vec3 hit = p0.add(d.scale(tMin));
    return hit;
  }

  /**
   * 检测点是否在 AABB 内
   *
   * @param point 查询点
   * @param box 边界盒
   * @return 如果点在盒内返回 true
   */
  private static boolean isPointInAABB(Vec3 point, AABB box) {
    return point.x >= box.minX
        && point.x <= box.maxX
        && point.y >= box.minY
        && point.y <= box.maxY
        && point.z >= box.minZ
        && point.z <= box.maxZ;
  }
}
