package net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.ward;

import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 环绕运动寻路器
 * <p>
 * 实现护幕飞剑的环绕轨迹算法,让飞剑以优雅的圆周运动环绕主人。
 *
 * <h3>算法特性</h3>
 * <ul>
 *   <li><b>切向速度</b>: 飞剑沿环绕圆周的切线方向移动</li>
 *   <li><b>径向调整</b>: 同时向目标槽位径向靠近</li>
 *   <li><b>速度调节</b>: 距离槽位越远,环绕速度越快</li>
 *   <li><b>平滑减速</b>: 接近槽位时逐渐减速并停稳</li>
 * </ul>
 *
 * <h3>运动公式</h3>
 * <pre>
 * v_orbit = v_tangent + v_radial
 *
 * v_tangent = ω × r (切向速度,垂直于径向)
 * v_radial = k * (r_target - r_current) (径向速度,指向目标半径)
 *
 * 其中:
 * - ω: 角速度 = baseAngularSpeed * distanceFactor
 * - r: 当前位置相对主人的向量
 * - k: 径向调节系数
 * </pre>
 */
public final class OrbitPathfinder {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrbitPathfinder.class);

    // 环绕运动参数
    private static final double BASE_ANGULAR_SPEED = 0.8;  // 基础角速度(弧度/秒)
    private static final double RADIAL_CORRECTION_FACTOR = 0.6;  // 径向修正系数
    private static final double SLOW_DOWN_DISTANCE = 1.5;  // 开始减速的距离(m)
    private static final double ARRIVAL_THRESHOLD = 0.3;  // 到达判定阈值(m)

    private OrbitPathfinder() {
        throw new AssertionError("OrbitPathfinder should not be instantiated");
    }

    /**
     * 计算环绕运动的目标速度向量
     * <p>
     * 此方法返回的是飞剑应该设置的速度向量(m/tick),已考虑:
     * <ul>
     *   <li>环绕切向运动</li>
     *   <li>径向位置修正</li>
     *   <li>接近时的平滑减速</li>
     *   <li>最大速度限制</li>
     * </ul>
     *
     * @param ctx 寻路上下文
     * @return 目标速度向量(m/tick),可直接用于 setDeltaMovement
     */
    public static Vec3 computeOrbitVelocity(PathfindingContext ctx) {
        if (ctx.owner() == null) {
            // 无主人时,退化为简单直线接近
            return computeDirectApproach(ctx);
        }

        Vec3 ownerPos = ctx.owner().position();
        Vec3 swordPos = ctx.currentPos();
        Vec3 targetPos = ctx.target();

        // 计算相对位置
        Vec3 relativePos = swordPos.subtract(ownerPos);  // r_current
        Vec3 relativeTarget = targetPos.subtract(ownerPos);  // r_target

        // 当前半径和目标半径
        double currentRadius = new Vec3(relativePos.x, 0, relativePos.z).length();
        double targetRadius = new Vec3(relativeTarget.x, 0, relativeTarget.z).length();

        // 计算到目标槽位的距离
        double distToSlot = swordPos.distanceTo(targetPos);

        // === 1. 切向速度分量 ===
        Vec3 tangentialVel = computeTangentialVelocity(
                relativePos,
                currentRadius,
                distToSlot,
                ctx.vMax()
        );

        // === 2. 径向速度分量 ===
        Vec3 radialVel = computeRadialVelocity(
                relativePos,
                relativeTarget,
                currentRadius,
                targetRadius,
                distToSlot,
                ctx.vMax()
        );

        // === 3. 垂直速度分量(Y轴调整) ===
        Vec3 verticalVel = computeVerticalVelocity(
                swordPos,
                targetPos,
                distToSlot,
                ctx.vMax()
        );

        // === 4. 合成总速度 ===
        Vec3 desiredVel = tangentialVel.add(radialVel).add(verticalVel);

        // === 5. 应用速度限制 ===
        double desiredSpeed = desiredVel.length();
        double maxSpeed = ctx.vMax() / 20.0;  // 转换为 m/tick

        if (desiredSpeed > maxSpeed) {
            desiredVel = desiredVel.normalize().scale(maxSpeed);
        }

        // === 6. 接近目标时减速 ===
        if (distToSlot < SLOW_DOWN_DISTANCE) {
            double slowFactor = Math.max(0.3, distToSlot / SLOW_DOWN_DISTANCE);
            desiredVel = desiredVel.scale(slowFactor);
        }

        // === 7. 极近距离时完全停止 ===
        if (distToSlot < ARRIVAL_THRESHOLD) {
            return Vec3.ZERO;
        }

        return desiredVel;
    }

    /**
     * 计算切向速度(绕主人环绕的圆周运动分量)
     * <p>
     * 切向速度垂直于径向,实现圆周运动效果。
     * 速度大小根据距离槽位的远近动态调整。
     *
     * @param relativePos 相对主人的位置向量
     * @param currentRadius 当前半径(XZ平面)
     * @param distToSlot 到目标槽位的距离
     * @param vMax 最大速度(m/s)
     * @return 切向速度向量(m/tick)
     */
    private static Vec3 computeTangentialVelocity(
            Vec3 relativePos,
            double currentRadius,
            double distToSlot,
            double vMax
    ) {
        if (currentRadius < 0.1) {
            return Vec3.ZERO;  // 太靠近主人,不做环绕运动
        }

        // 在XZ平面上计算切向方向(逆时针)
        // 切向 = (-z, 0, x) / radius
        Vec3 tangentDir = new Vec3(-relativePos.z, 0, relativePos.x).normalize();

        // 角速度随距离动态调整
        // 距离槽位越远,转得越快(快速寻找槽位)
        double distanceFactor = Math.min(2.0, 1.0 + distToSlot / 3.0);
        double angularSpeed = BASE_ANGULAR_SPEED * distanceFactor;

        // v = ω * r (切向速度大小)
        double tangentialSpeed = angularSpeed * currentRadius;

        // 限制切向速度不超过最大速度的70%
        tangentialSpeed = Math.min(tangentialSpeed, vMax * 0.7);

        // 转换为 m/tick
        Vec3 tangentialVel = tangentDir.scale(tangentialSpeed / 20.0);

        return tangentialVel;
    }

    /**
     * 计算径向速度(向目标半径调整)
     * <p>
     * 径向速度使飞剑逐渐接近或远离主人,以达到目标环绕半径。
     *
     * @param relativePos 相对主人的当前位置
     * @param relativeTarget 相对主人的目标位置
     * @param currentRadius 当前半径
     * @param targetRadius 目标半径
     * @param distToSlot 到槽位的距离
     * @param vMax 最大速度(m/s)
     * @return 径向速度向量(m/tick)
     */
    private static Vec3 computeRadialVelocity(
            Vec3 relativePos,
            Vec3 relativeTarget,
            double currentRadius,
            double targetRadius,
            double distToSlot,
            double vMax
    ) {
        // 径向偏差
        double radiusDiff = targetRadius - currentRadius;

        if (Math.abs(radiusDiff) < 0.1 && distToSlot < 1.0) {
            // 半径已接近目标,且距离槽位较近,减小径向调整
            return Vec3.ZERO;
        }

        // 径向方向(XZ平面上从主人指向当前位置)
        Vec3 radialDirXZ = new Vec3(relativePos.x, 0, relativePos.z);
        if (radialDirXZ.length() < 0.1) {
            // 退化情况:使用目标方向
            radialDirXZ = new Vec3(relativeTarget.x, 0, relativeTarget.z);
        }
        radialDirXZ = radialDirXZ.normalize();

        // 径向速度大小 = k * Δr
        double radialSpeed = RADIAL_CORRECTION_FACTOR * radiusDiff;

        // 限制径向速度
        radialSpeed = Math.max(-vMax * 0.5, Math.min(vMax * 0.5, radialSpeed));

        // 转换为 m/tick
        Vec3 radialVel = radialDirXZ.scale(radialSpeed / 20.0);

        return radialVel;
    }

    /**
     * 计算垂直速度(Y轴高度调整)
     * <p>
     * 使飞剑逐渐调整到目标高度。
     *
     * @param currentPos 当前位置
     * @param targetPos 目标位置
     * @param distToSlot 到槽位的距离
     * @param vMax 最大速度(m/s)
     * @return 垂直速度向量(m/tick)
     */
    private static Vec3 computeVerticalVelocity(
            Vec3 currentPos,
            Vec3 targetPos,
            double distToSlot,
            double vMax
    ) {
        double yDiff = targetPos.y - currentPos.y;

        if (Math.abs(yDiff) < 0.1) {
            return Vec3.ZERO;
        }

        // 垂直速度与高度差成正比
        double verticalSpeed = yDiff * 0.5;

        // 限制垂直速度
        verticalSpeed = Math.max(-vMax * 0.6, Math.min(vMax * 0.6, verticalSpeed));

        // 接近时减速
        if (distToSlot < SLOW_DOWN_DISTANCE) {
            verticalSpeed *= (distToSlot / SLOW_DOWN_DISTANCE);
        }

        // 转换为 m/tick
        return new Vec3(0, verticalSpeed / 20.0, 0);
    }

    /**
     * 退化算法:无主人时的简单直线接近
     *
     * @param ctx 寻路上下文
     * @return 速度向量(m/tick)
     */
    private static Vec3 computeDirectApproach(PathfindingContext ctx) {
        Vec3 direction = ctx.getDirectionToTarget();
        double distance = ctx.getDistanceToTarget();

        if (distance < ARRIVAL_THRESHOLD) {
            return Vec3.ZERO;
        }

        // 简单的匀速接近
        double speed = Math.min(ctx.vMax(), distance * 2.0);

        // 转换为 m/tick
        return direction.scale(speed / 20.0);
    }

    /**
     * 检查是否已到达环绕槽位
     *
     * @param ctx 寻路上下文
     * @return 若已到达返回 true
     */
    public static boolean hasArrivedAtSlot(PathfindingContext ctx) {
        return ctx.hasReachedTarget(ARRIVAL_THRESHOLD);
    }
}
