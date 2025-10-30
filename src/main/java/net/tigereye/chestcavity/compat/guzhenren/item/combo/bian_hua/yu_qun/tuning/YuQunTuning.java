package net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yu_qun.tuning;

import java.util.List;

/**
 * 鱼群组合杀招的调参工具类。
 * <p>
 * 定义了技能的基础数值，如范围、宽度、推动强度和冷却时间，并提供了计算最终冷却时间的方法。
 * </p>
 */
public final class YuQunTuning {
    private YuQunTuning() {}

    // ===================
    //  基础属性
    // ===================
    /** 基础范围 */
    public static final double BASE_RANGE = 8.0;
    /** 基础宽度（锥形范围的夹角余弦值） */
    public static final double BASE_WIDTH_COS = 0.8;
    /** 基础推动强度 */
    public static final double BASE_PUSH = 1.2;
    /** 基础伤害 */
    public static final double BASE_DAMAGE = 10.0;

    // ===================
    //  道痕软化映射
    // ===================
    /** 道痕软化映射基准。此值越大，道痕在前期提供的增益越平缓。 */
    public static final double DAO_HEN_SOFTENING_X0 = 600.0;

    // ===================
    //  增益/减益乘数 (Multiplier)
    // ===================
    /** 水道道痕对增益的贡献系数 */
    public static final double AW_WATER_DAO_HEN_FACTOR = 0.9;
    /** 变化道道痕对增益的贡献系数 */
    public static final double AC_CHANGE_DAO_HEN_FACTOR = 0.7;
    /** 协同数对增益的贡献系数 */
    public static final double AS_SYNERGY_FACTOR = 0.35;
    /** 炎道道痕对减益（压制）的贡献系数 */
    public static final double BF_FIRE_DAO_HEN_FACTOR = 0.9;

    // ===================
    //  宽度 (Width) 独立调整
    // ===================
    /** 水道道痕对宽度（cos值）的减小系数 (使锥形更宽) */
    public static final double KW_WATER_DAO_HEN_WIDTH_FACTOR = 0.05;
    /** 变化道道痕对宽度（cos值）的减小系数 */
    public static final double KC_CHANGE_DAO_HEN_WIDTH_FACTOR = 0.04;
    /** 协同数对宽度（cos值）的减小系数 */
    public static final double KS_SYNERGY_WIDTH_FACTOR = 0.03;
    /** 炎道道痕对宽度（cos值）的增加系数 (使锥形更窄) */
    public static final double KF_FIRE_DAO_HEN_WIDTH_FACTOR = 0.06;
    /** 宽度（cos值）的最小值，防止锥形范围过大 */
    public static final double COS_MIN = 0.05;
    /** 宽度（cos值）最大值的钳制下限，防止锥形范围比基础值更窄 */
    public static final double COS_MAX_CLAMP_FLOOR = 0.95;

    // ===================
    //  冷却 (Cooldown)
    // ===================
    /** 基础冷却时间（ticks） */
    public static final int BASE_COOLDOWN_TICKS = 20 * 12;
    /** 流派经验对冷却时间的减免配置 */
    public static final List<CooldownTier> FLOW_EXPERIENCE_COOLDOWN =
        List.of(new CooldownTier(10_001.0D, 20 * 4));

    /**
     * 根据流派总经验值计算最终的冷却时间。
     * @param totalExperience 水道和变化道流派的总经验值。
     * @return 计算得出的冷却时间（单位：ticks），最小为5。
     */
    public static int computeCooldownTicks(double totalExperience) {
        return computeCooldownTicks(totalExperience, FLOW_EXPERIENCE_COOLDOWN);
    }

    /**
     * (用于测试) 根据流派总经验值和指定的冷却阶梯计算最终的冷却时间。
     * @param totalExperience 水道和变化道流派的总经验值。
     * @param tiers 冷却时间的阶梯配置。
     * @return 计算得出的冷却时间（单位：ticks），最小为5。
     */
    static int computeCooldownTicks(double totalExperience, List<CooldownTier> tiers) {
        int result = BASE_COOLDOWN_TICKS;
        double sanitized = Math.max(0.0D, totalExperience);
        for (CooldownTier tier : tiers) {
            double threshold = Math.max(1.0D, tier.threshold());
            double ratio = Math.min(1.0D, sanitized / threshold);
            int candidate = (int) Math.round(
                BASE_COOLDOWN_TICKS - (BASE_COOLDOWN_TICKS - tier.minCooldownTicks()) * ratio);
            candidate = Math.max(tier.minCooldownTicks(), candidate);
            result = Math.min(result, candidate);
        }
        return Math.max(5, result);
    }

    public record CooldownTier(double threshold, int minCooldownTicks) {}
}
