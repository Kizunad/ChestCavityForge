package net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.ward;

import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 拦截运动寻路器
 * <p>
 * 实现护幕飞剑的拦截轨迹算法,让飞剑以最大速度、最优路径快速到达拦截点。
 *
 * <h3>算法特性</h3>
 * <ul>
 *   <li><b>全速冲刺</b>: 拦截阶段始终保持最大速度</li>
 *   <li><b>预判轨迹</b>: 考虑投射物运动,提前预判拦截点</li>
 *   <li><b>精确刹车</b>: 到达拦截点时平滑减速并停留</li>
 *   <li><b>轨迹优化</b>: 计算最短时间到达路径</li>
 * </ul>
 *
 * <h3>与ORBIT的差异</h3>
 * <table>
 *   <tr><th>特性</th><th>ORBIT</th><th>INTERCEPT</th></tr>
 *   <tr><td>速度策略</td><td>动态调节(30%-100%)</td><td>全速冲刺(100%)</td></tr>
 *   <tr><td>轨迹类型</td><td>环绕圆周运动</td><td>直线加速到达</td></tr>
 *   <tr><td>减速区间</td><td>1.5m开始减速</td><td>根据刹车距离计算</td></tr>
 *   <tr><td>到达判定</td><td>0.3m</td><td>0.5m(更宽松)</td></tr>
 * </table>
 */
public final class InterceptPathfinder {

    private static final Logger LOGGER = LoggerFactory.getLogger(InterceptPathfinder.class);

    // 拦截运动参数
    private static final double INTERCEPT_ARRIVAL_THRESHOLD = 0.5;  // 拦截到达判定阈值(m)
    private static final double BRAKE_SAFETY_MARGIN = 1.2;  // 刹车安全余量系数
    private static final double MIN_BRAKE_DISTANCE = 0.8;  // 最小刹车距离(m)

    private InterceptPathfinder() {
        throw new AssertionError("InterceptPathfinder should not be instantiated");
    }

    /**
     * 计算拦截运动的目标速度向量
     * <p>
     * 拦截算法分为三个阶段:
     * <ol>
     *   <li><b>加速阶段</b>: 全力加速到最大速度</li>
     *   <li><b>巡航阶段</b>: 保持最大速度直线飞向拦截点</li>
     *   <li><b>刹车阶段</b>: 提前计算刹车距离,平滑减速到停止</li>
     * </ol>
     *
     * @param ctx 寻路上下文
     * @return 目标速度向量(m/tick)
     */
    public static Vec3 computeInterceptVelocity(PathfindingContext ctx) {
        Vec3 direction = ctx.getDirectionToTarget();
        double distance = ctx.getDistanceToTarget();
        double currentSpeed = ctx.getCurrentSpeed();
        double currentSpeedPerSec = currentSpeed * 20.0;

        // === 1. 已到达拦截点 ===
        if (distance < INTERCEPT_ARRIVAL_THRESHOLD) {
            // 快速减速到停止
            return ctx.currentVel().scale(0.3);
        }

        // === 2. 计算理论刹车距离 ===
        // d_brake = v² / (2a)
        double theoreticalBrakeDistance = 0.0;
        if (currentSpeedPerSec > 0.1) {
            theoreticalBrakeDistance = (currentSpeedPerSec * currentSpeedPerSec) / (2.0 * ctx.aMax());
        }

        // 加上安全余量
        double effectiveBrakeDistance = Math.max(
                MIN_BRAKE_DISTANCE,
                theoreticalBrakeDistance * BRAKE_SAFETY_MARGIN
        );

        // === 3. 判断当前阶段 ===
        boolean shouldBrake = distance <= effectiveBrakeDistance;

        if (shouldBrake) {
            // === 刹车阶段 ===
            return computeBrakingVelocity(ctx, direction, distance, currentSpeed);
        } else {
            // === 加速/巡航阶段 ===
            return computeAcceleratingVelocity(ctx, direction, currentSpeed);
        }
    }

    /**
     * 计算加速/巡航阶段的速度
     * <p>
     * 全力加速到最大速度,并保持方向。
     *
     * @param ctx 寻路上下文
     * @param direction 目标方向(归一化)
     * @param currentSpeed 当前速度(m/tick)
     * @return 目标速度向量(m/tick)
     */
    private static Vec3 computeAcceleratingVelocity(
            PathfindingContext ctx,
            Vec3 direction,
            double currentSpeed
    ) {
        double maxSpeedPerTick = ctx.vMax() / 20.0;
        double maxAccelPerTick = ctx.getMaxAccelPerTick();

        // 计算目标速度:当前速度 + 最大加速度
        double targetSpeed = Math.min(currentSpeed + maxAccelPerTick, maxSpeedPerTick);

        // 返回方向 * 目标速度
        return direction.scale(targetSpeed);
    }

    /**
     * 计算刹车阶段的速度
     * <p>
     * 根据剩余距离动态计算刹车速度,确保平滑减速到停止。
     *
     * 刹车公式:
     * <pre>
     * v_target = sqrt(2 * a * d_remaining)
     * </pre>
     *
     * @param ctx 寻路上下文
     * @param direction 目标方向(归一化)
     * @param distance 剩余距离(m)
     * @param currentSpeed 当前速度(m/tick)
     * @return 目标速度向量(m/tick)
     */
    private static Vec3 computeBrakingVelocity(
            PathfindingContext ctx,
            Vec3 direction,
            double distance,
            double currentSpeed
    ) {
        // 根据剩余距离计算理想刹车速度
        // v = sqrt(2 * a * d)
        double idealBrakeSpeed = Math.sqrt(2.0 * ctx.aMax() * distance);

        // 转换为 m/tick
        double idealBrakeSpeedPerTick = idealBrakeSpeed / 20.0;

        // 不能超过当前速度(只能减速,不能加速)
        double targetSpeed = Math.min(currentSpeed, idealBrakeSpeedPerTick);

        // 确保不会反向
        targetSpeed = Math.max(0.0, targetSpeed);

        // 极近距离时强制快速减速
        if (distance < INTERCEPT_ARRIVAL_THRESHOLD * 1.5) {
            targetSpeed *= (distance / (INTERCEPT_ARRIVAL_THRESHOLD * 1.5));
        }

        return direction.scale(targetSpeed);
    }

    /**
     * 检查是否已到达拦截点
     *
     * @param ctx 寻路上下文
     * @return 若已到达返回 true
     */
    public static boolean hasArrivedAtInterceptPoint(PathfindingContext ctx) {
        return ctx.hasReachedTarget(INTERCEPT_ARRIVAL_THRESHOLD);
    }

    /**
     * 估算到达拦截点所需的时间(秒)
     * <p>
     * 用于拦截任务分配时评估飞剑是否能及时到达。
     *
     * 估算公式:
     * <pre>
     * 如果 d <= d_cruise (已达巡航速度):
     *   t = d / v_max
     *
     * 否则 (需要加速):
     *   t = sqrt(2d / a) (匀加速运动)
     * </pre>
     *
     * @param ctx 寻路上下文
     * @return 估算时间(秒)
     */
    public static double estimateTimeToArrive(PathfindingContext ctx) {
        double distance = ctx.getDistanceToTarget();
        double currentSpeedPerSec = ctx.getCurrentSpeedPerSecond();
        double vMax = ctx.vMax();
        double aMax = ctx.aMax();

        // 计算从当前速度加速到最大速度需要的距离
        // d_accel = (v_max² - v_current²) / (2a)
        double accelDistance = 0.0;
        if (currentSpeedPerSec < vMax) {
            accelDistance = (vMax * vMax - currentSpeedPerSec * currentSpeedPerSec) / (2.0 * aMax);
        }

        if (distance <= accelDistance) {
            // 还在加速阶段就到达
            // 匀加速: d = v0*t + 0.5*a*t²
            // 解方程: t = (-v0 + sqrt(v0² + 2ad)) / a
            double discriminant = currentSpeedPerSec * currentSpeedPerSec + 2.0 * aMax * distance;
            return (-currentSpeedPerSec + Math.sqrt(discriminant)) / aMax;
        } else {
            // 先加速到最大速度,然后匀速
            double accelTime = (vMax - currentSpeedPerSec) / aMax;
            double cruiseDistance = distance - accelDistance;
            double cruiseTime = cruiseDistance / vMax;
            return accelTime + cruiseTime;
        }
    }

    /**
     * 计算当前是否处于刹车阶段
     *
     * @param ctx 寻路上下文
     * @return 若应该开始刹车返回 true
     */
    public static boolean shouldStartBraking(PathfindingContext ctx) {
        double distance = ctx.getDistanceToTarget();
        double brakeDistance = ctx.getBrakingDistance() * BRAKE_SAFETY_MARGIN;
        brakeDistance = Math.max(MIN_BRAKE_DISTANCE, brakeDistance);

        return distance <= brakeDistance;
    }
}
