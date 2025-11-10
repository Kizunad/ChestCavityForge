package net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.ward;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 障碍物避让系统
 *
 * <p>为护幕飞剑提供基本的避障能力,防止卡在方块中。
 *
 * <h3>避障策略</h3>
 *
 * <ul>
 *   <li><b>射线检测</b>: 检测前方路径是否有方块阻挡
 *   <li><b>简单绕路</b>: 检测到障碍时,尝试向上、向旁边偏移
 *   <li><b>最小干预</b>: 只在必要时调整路径,保持原有运动逻辑
 * </ul>
 *
 * <h3>使用方式</h3>
 *
 * <pre>
 * Vec3 desiredVel = pathfinder.compute(...);
 * Vec3 safeVel = ObstacleAvoidance.adjustForObstacles(ctx, desiredVel);
 * entity.setDeltaMovement(safeVel);
 * </pre>
 */
public final class ObstacleAvoidance {

  private static final Logger LOGGER = LoggerFactory.getLogger(ObstacleAvoidance.class);

  // 避障参数
  private static final double LOOK_AHEAD_DISTANCE = 2.0; // 前瞻距离(m)
  private static final double AVOIDANCE_UP_OFFSET = 1.0; // 向上绕行偏移(m)
  private static final double AVOIDANCE_SIDE_OFFSET = 0.8; // 侧向绕行偏移(m)
  private static final double MIN_SPEED_FOR_AVOIDANCE = 0.05; // 最小避障速度阈值(m/tick)

  private ObstacleAvoidance() {
    throw new AssertionError("ObstacleAvoidance should not be instantiated");
  }

  /**
   * 调整速度向量以避开障碍物
   *
   * <p>此方法检测前方是否有障碍,如果有,则修改速度方向以绕过障碍。
   *
   * @param ctx 寻路上下文
   * @param desiredVel 原始期望速度向量(m/tick)
   * @return 调整后的安全速度向量(m/tick)
   */
  public static Vec3 adjustForObstacles(PathfindingContext ctx, Vec3 desiredVel) {
    if (desiredVel.length() < MIN_SPEED_FOR_AVOIDANCE) {
      // 速度太慢,不需要避障
      return desiredVel;
    }

    Level world = ctx.world();
    Vec3 currentPos = ctx.currentPos();

    // 计算前瞻位置
    Vec3 direction = desiredVel.normalize();
    double lookAheadDist = Math.min(LOOK_AHEAD_DISTANCE, desiredVel.length() * 20.0);
    Vec3 lookAheadPos = currentPos.add(direction.scale(lookAheadDist));

    // 射线检测:检查路径上是否有方块
    BlockHitResult hitResult = raycastBlocks(world, currentPos, lookAheadPos);

    if (hitResult.getType() == HitResult.Type.MISS) {
      // 前方无障碍,保持原速度
      return desiredVel;
    }

    // 检测到障碍,尝试绕路
    Vec3 avoidanceVel = computeAvoidanceVelocity(ctx, desiredVel, hitResult);

    return avoidanceVel;
  }

  /**
   * 执行方块射线检测
   *
   * @param world 世界对象
   * @param from 起点
   * @param to 终点
   * @return 射线检测结果
   */
  private static BlockHitResult raycastBlocks(Level world, Vec3 from, Vec3 to) {
    // 使用 CollisionContext.empty() 而不是 null
    ClipContext clipContext =
        new ClipContext(
            from,
            to,
            ClipContext.Block.COLLIDER, // 检测碰撞箱
            ClipContext.Fluid.NONE, // 忽略流体
            net.minecraft.world.phys.shapes.CollisionContext.empty() // 空碰撞上下文
            );

    return world.clip(clipContext);
  }

  /**
   * 计算避障速度向量
   *
   * <p>策略:
   *
   * <ol>
   *   <li>尝试向上绕行(飞剑可以飞,向上是最简单的)
   *   <li>如果向上也有障碍,尝试侧向绕行
   *   <li>如果都不行,减速并保持原方向
   * </ol>
   *
   * @param ctx 寻路上下文
   * @param desiredVel 原始期望速度
   * @param obstacleHit 障碍碰撞结果
   * @return 避障速度向量(m/tick)
   */
  private static Vec3 computeAvoidanceVelocity(
      PathfindingContext ctx, Vec3 desiredVel, BlockHitResult obstacleHit) {
    Vec3 currentPos = ctx.currentPos();
    Level world = ctx.world();
    Vec3 hitNormal = Vec3.atLowerCornerOf(obstacleHit.getDirection().getNormal());

    // === 策略1: 向上绕行 ===
    Vec3 upAvoidance = desiredVel.add(0, AVOIDANCE_UP_OFFSET / 20.0, 0);
    Vec3 upTestPos = currentPos.add(upAvoidance.scale(20.0));

    BlockHitResult upHit = raycastBlocks(world, currentPos, upTestPos);
    if (upHit.getType() == HitResult.Type.MISS) {
      // 向上绕行可行
      return upAvoidance.normalize().scale(desiredVel.length());
    }

    // === 策略2: 侧向绕行 ===
    // 计算垂直于前进方向和法向量的侧向
    Vec3 forward = desiredVel.normalize();
    Vec3 sideDir = forward.cross(new Vec3(0, 1, 0)).normalize();

    if (sideDir.lengthSqr() < 0.01) {
      // 退化情况:前进方向是竖直的
      sideDir = new Vec3(1, 0, 0);
    }

    // 尝试左侧
    Vec3 leftAvoidance = forward.add(sideDir.scale(AVOIDANCE_SIDE_OFFSET)).normalize();
    Vec3 leftTestPos = currentPos.add(leftAvoidance.scale(LOOK_AHEAD_DISTANCE));

    BlockHitResult leftHit = raycastBlocks(world, currentPos, leftTestPos);
    if (leftHit.getType() == HitResult.Type.MISS) {
      return leftAvoidance.scale(desiredVel.length());
    }

    // 尝试右侧
    Vec3 rightAvoidance = forward.add(sideDir.scale(-AVOIDANCE_SIDE_OFFSET)).normalize();
    Vec3 rightTestPos = currentPos.add(rightAvoidance.scale(LOOK_AHEAD_DISTANCE));

    BlockHitResult rightHit = raycastBlocks(world, currentPos, rightTestPos);
    if (rightHit.getType() == HitResult.Type.MISS) {
      return rightAvoidance.scale(desiredVel.length());
    }

    // === 策略3: 都不行,沿障碍表面滑动 ===
    // 计算速度在障碍法向量上的投影,并移除该分量
    Vec3 slideVel = desiredVel.subtract(hitNormal.scale(desiredVel.dot(hitNormal)));

    if (slideVel.lengthSqr() > 0.01) {
      return slideVel.normalize().scale(desiredVel.length() * 0.5);
    }

    // === 最后手段: 减速 ===
    return desiredVel.scale(0.3);
  }

  /**
   * 检查位置是否在固体方块内
   *
   * @param world 世界对象
   * @param pos 位置
   * @return 若在固体方块内返回 true
   */
  public static boolean isInsideSolidBlock(Level world, Vec3 pos) {
    BlockPos blockPos = BlockPos.containing(pos);
    return !world.getBlockState(blockPos).isAir()
        && world.getBlockState(blockPos).isSolidRender(world, blockPos);
  }

  /**
   * 如果实体卡在方块中,计算逃逸速度
   *
   * <p>用于紧急情况:飞剑因某种原因卡在方块里,需要快速脱困。
   *
   * @param ctx 寻路上下文
   * @return 逃逸速度向量,如果未卡住则返回 Vec3.ZERO
   */
  public static Vec3 computeEscapeVelocity(PathfindingContext ctx) {
    Vec3 currentPos = ctx.currentPos();
    Level world = ctx.world();

    if (!isInsideSolidBlock(world, currentPos)) {
      return Vec3.ZERO; // 未卡住
    }

    // 卡住了,尝试向上逃逸
    LOGGER.warn(
        "Sword {} is stuck inside block at {}, escaping upward", ctx.sword().getId(), currentPos);

    return new Vec3(0, 0.3 / 20.0, 0); // 向上 0.3 m/s
  }
}
