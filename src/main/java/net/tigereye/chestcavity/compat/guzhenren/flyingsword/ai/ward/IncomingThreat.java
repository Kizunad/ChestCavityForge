package net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.ward;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * 完整的威胁描述（近战或投射）
 * <p>
 * 此记录封装了来自攻击者的威胁信息，用于护幕系统的拦截规划。
 * 威胁可以是投射物（箭、火球等）或近战攻击。
 *
 * <h3>投射物威胁</h3>
 * 投射物威胁包含位置和速度信息，用于预测轨迹：
 * <ul>
 *   <li>{@link #projPos} - 投射物当前位置</li>
 *   <li>{@link #projVel} - 投射物速度向量</li>
 * </ul>
 *
 * <h3>近战威胁</h3>
 * 近战威胁基于攻击者和目标的位置关系：
 * <ul>
 *   <li>{@link #projPos} 和 {@link #projVel} 为 null</li>
 *   <li>使用 {@link #attacker} 位置到 {@link #target} 位置的线段</li>
 * </ul>
 *
 * @param attacker 攻击发起者（实体）
 * @param target 预期目标（通常为玩家）
 * @param targetHitPoint 预期命中点（用于投射预测或近战线段），可选
 * @param projPos 投射物当前位置（null 表示近战）
 * @param projVel 投射物速度（null 表示近战或未知）
 * @param worldTime 事件发生的世界时刻（tick）
 */
public record IncomingThreat(
        Entity attacker,
        Entity target,
        @Nullable Vec3 targetHitPoint,
        @Nullable Vec3 projPos,
        @Nullable Vec3 projVel,
        long worldTime
) {
    /**
     * 判定威胁是否为投射物
     * <p>
     * 投射物威胁同时包含位置和速度信息
     *
     * @return 如果是投射物威胁返回 true
     */
    public boolean isProjectile() {
        return projPos != null && projVel != null;
    }

    /**
     * 判定威胁是否为近战
     * <p>
     * 近战威胁不包含投射物位置和速度信息
     *
     * @return 如果是近战威胁返回 true
     */
    public boolean isMelee() {
        return projPos == null && projVel == null;
    }

    /**
     * 获取威胁的简要描述（用于日志）
     *
     * @return 威胁描述字符串
     */
    public String describe() {
        if (isProjectile()) {
            return String.format("Projectile[%s -> %s @ %.1fm/s]",
                    attacker.getName().getString(),
                    target.getName().getString(),
                    projVel != null ? projVel.length() : 0.0);
        } else {
            return String.format("Melee[%s -> %s]",
                    attacker.getName().getString(),
                    target.getName().getString());
        }
    }
}
