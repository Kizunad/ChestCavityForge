package net.tigereye.chestcavity.compat.guzhenren.flyingsword.integration.ward;

import java.util.UUID;

/**
 * 默认护幕数值供给实现
 * <p>
 * 骨架阶段：所有方法返回常量或简单公式。
 * <p>
 * 在后续阶段，可以替换为读取玩家道痕、经验等数据的实现。
 *
 * <h3>实现策略</h3>
 * <ul>
 *   <li>所有方法使用 {@link WardConfig} 中的常量</li>
 *   <li>公式使用简化版本（不读取实际玩家数据）</li>
 *   <li>支持后续扩展为数据驱动实现</li>
 * </ul>
 */
public class DefaultWardTuning implements WardTuning {

    /**
     * 默认道痕等级（骨架阶段固定值）
     */
    private static final double DEFAULT_TRAIL_LEVEL = 0.0;

    /**
     * 默认流派经验（骨架阶段固定值）
     */
    private static final double DEFAULT_SECT_EXP = 0.0;

    @Override
    public int maxSwords(UUID owner) {
        // 公式：N = clamp(1 + floor(sqrt(道痕/100)) + floor(经验/1000), 1, max)
        // 骨架阶段：返回固定值
        double trail = getTrailLevel(owner);
        double exp = getSectExperience(owner);

        int n = 1 + (int) Math.floor(Math.sqrt(trail / 100.0)) + (int) Math.floor(exp / 1000.0);
        return Math.max(1, Math.min(n, WardConfig.MAX_WARDS));
    }

    @Override
    public double orbitRadius(UUID owner, int currentSwordCount) {
        // 公式：r = 2.6 + 0.4 * N
        return WardConfig.ORBIT_RADIUS_BASE + WardConfig.ORBIT_RADIUS_PER_SWORD * currentSwordCount;
    }

    @Override
    public double vMax(UUID owner) {
        // 公式：vMax = 6.0 + 0.02 * 道痕 + 0.001 * 经验
        double trail = getTrailLevel(owner);
        double exp = getSectExperience(owner);

        return WardConfig.SPEED_BASE
            + WardConfig.SPEED_TRAIL_COEF * trail
            + WardConfig.SPEED_EXP_COEF * exp;
    }

    @Override
    public double aMax(UUID owner) {
        // 骨架阶段：返回常量
        return WardConfig.ACCEL_BASE;
    }

    @Override
    public double reactionDelay(UUID owner) {
        // 公式：reaction = clamp(0.06 - 0.00005 * 经验, 0.02, 0.06)
        double exp = getSectExperience(owner);

        double reaction = WardConfig.REACTION_BASE - WardConfig.REACTION_EXP_COEF * exp;
        return Math.max(WardConfig.REACTION_MIN, Math.min(reaction, WardConfig.REACTION_MAX));
    }

    @Override
    public double counterRange() {
        return WardConfig.COUNTER_RANGE;
    }

    @Override
    public double windowMin() {
        return WardConfig.WINDOW_MIN;
    }

    @Override
    public double windowMax() {
        return WardConfig.WINDOW_MAX;
    }

    @Override
    public int costBlock(UUID owner) {
        // 公式：R = clamp(经验 / (经验 + 2000), 0, 0.6)
        //      costBlock = round(8 * (1 - R))
        double exp = getSectExperience(owner);
        double r = Math.min(exp / (exp + WardConfig.EXP_DECAY_BASE), WardConfig.EXP_DECAY_MAX);

        return (int) Math.round(WardConfig.DURABILITY_BLOCK * (1.0 - r));
    }

    @Override
    public int costCounter(UUID owner) {
        // 公式：R = clamp(经验 / (经验 + 2000), 0, 0.6)
        //      costCounter = round(10 * (1 - R))
        double exp = getSectExperience(owner);
        double r = Math.min(exp / (exp + WardConfig.EXP_DECAY_BASE), WardConfig.EXP_DECAY_MAX);

        return (int) Math.round(WardConfig.DURABILITY_COUNTER * (1.0 - r));
    }

    @Override
    public int costFail(UUID owner) {
        // 公式：R = clamp(经验 / (经验 + 2000), 0, 0.6)
        //      costFail = round(2 * (1 - 0.5*R))
        double exp = getSectExperience(owner);
        double r = Math.min(exp / (exp + WardConfig.EXP_DECAY_BASE), WardConfig.EXP_DECAY_MAX);

        return (int) Math.round(WardConfig.DURABILITY_FAIL * (1.0 - 0.5 * r));
    }

    @Override
    public double counterDamage(UUID owner) {
        // 公式：D = 5.0 + 0.05 * 道痕 + 0.01 * 经验
        double trail = getTrailLevel(owner);
        double exp = getSectExperience(owner);

        return WardConfig.COUNTER_DAMAGE_BASE
            + WardConfig.COUNTER_DAMAGE_TRAIL_COEF * trail
            + WardConfig.COUNTER_DAMAGE_EXP_COEF * exp;
    }

    @Override
    public double initialWardDurability(UUID owner) {
        // 公式：Dur0 = 60 + 0.3 * 道痕 + 0.1 * 经验
        double trail = getTrailLevel(owner);
        double exp = getSectExperience(owner);

        return WardConfig.INITIAL_DUR_BASE
            + WardConfig.INITIAL_DUR_TRAIL * trail
            + WardConfig.INITIAL_DUR_EXP * exp;
    }

    // ====== 辅助方法（骨架阶段返回固定值）======

    /**
     * 获取玩家道痕等级
     * <p>
     * 骨架阶段：返回固定值 0
     * <p>
     * 在后续阶段，可以替换为读取玩家实际道痕数据
     *
     * @param owner 玩家 UUID
     * @return 道痕等级
     */
    protected double getTrailLevel(UUID owner) {
        // TODO: 在后续阶段集成 GuzhenRen API 读取实际道痕
        return DEFAULT_TRAIL_LEVEL;
    }

    /**
     * 获取玩家流派经验
     * <p>
     * 骨架阶段：返回固定值 0
     * <p>
     * 在后续阶段，可以替换为读取玩家实际流派经验
     *
     * @param owner 玩家 UUID
     * @return 流派经验
     */
    protected double getSectExperience(UUID owner) {
        // TODO: 在后续阶段集成 GuzhenRen API 读取实际流派经验
        return DEFAULT_SECT_EXP;
    }
}
