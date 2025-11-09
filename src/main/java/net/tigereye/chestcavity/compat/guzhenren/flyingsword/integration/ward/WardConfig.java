package net.tigereye.chestcavity.compat.guzhenren.flyingsword.integration.ward;

/**
 * 护幕系统的全局常量与默认配置
 * <p>
 * 此类包含护幕系统的所有默认数值参数。
 * 这些值用作 {@link WardTuning} 接口的默认实现基准。
 *
 * <h3>设计原则</h3>
 * <ul>
 *   <li>所有常量应为 {@code public static final}</li>
 *   <li>数值应经过平衡性测试</li>
 *   <li>支持通过配置文件覆盖（见 E 阶段）</li>
 * </ul>
 */
public final class WardConfig {

    /**
     * 私有构造函数，防止实例化
     */
    private WardConfig() {
        throw new AssertionError("WardConfig should not be instantiated");
    }

    // ====== 时间窗口 ======

    /**
     * 最小可达时间窗（秒）
     * <p>
     * 如果飞剑到达拦截点的时间 < 此值，视为"太近，反应不及"
     */
    public static final double WINDOW_MIN = 0.1;

    /**
     * 最大可达时间窗（秒）
     * <p>
     * 如果飞剑到达拦截点的时间 > 此值，视为"太远，不值得拦截"
     */
    public static final double WINDOW_MAX = 1.0;

    // ====== 反击条件 ======

    /**
     * 触发反击的最大距离（米）
     * <p>
     * 只有当攻击者距离主人 ≤ 此距离时，才会触发反击
     */
    public static final double COUNTER_RANGE = 5.0;

    // ====== 运动默认值 ======

    /**
     * 基础最大速度（米/秒）
     * <p>
     * 公式：vMax = SPEED_BASE + 0.02*道痕 + 0.001*经验
     */
    public static final double SPEED_BASE = 6.0;

    /**
     * 基础最大加速度（米/秒²）
     * <p>
     * 影响飞剑的机动性和转向速度
     */
    public static final double ACCEL_BASE = 40.0;

    // ====== 耐久默认值 ======

    /**
     * 成功拦截的基础耐久消耗
     * <p>
     * 实际消耗会根据经验衰减：costBlock = round(8 * (1 - R))
     */
    public static final int DURABILITY_BLOCK = 8;

    /**
     * 成功反击的基础耐久消耗
     * <p>
     * 实际消耗会根据经验衰减：costCounter = round(10 * (1 - R))
     */
    public static final int DURABILITY_COUNTER = 10;

    /**
     * 失败尝试的基础耐久消耗
     * <p>
     * 实际消耗会根据经验衰减：costFail = round(2 * (1 - 0.5*R))
     */
    public static final int DURABILITY_FAIL = 2;

    // ====== 经验衰减参数 ======

    /**
     * 经验衰减基准值
     * <p>
     * 用于计算衰减系数：R = exp / (exp + EXP_DECAY_BASE)
     */
    public static final double EXP_DECAY_BASE = 2000.0;

    /**
     * 经验衰减系数上限
     * <p>
     * R 的最大值，即使经验很高也不会超过此值
     */
    public static final double EXP_DECAY_MAX = 0.6;

    // ====== 初始耐久参数 ======

    /**
     * 初始耐久基础值
     * <p>
     * 公式：Dur0 = INITIAL_DUR_BASE + INITIAL_DUR_TRAIL*道痕 + INITIAL_DUR_EXP*经验
     */
    public static final double INITIAL_DUR_BASE = 60.0;

    /**
     * 每点道痕增加的初始耐久
     */
    public static final double INITIAL_DUR_TRAIL = 0.3;

    /**
     * 每点经验增加的初始耐久
     */
    public static final double INITIAL_DUR_EXP = 0.1;

    // ====== 环绕参数 ======

    /**
     * 环绕半径基础值（米）
     * <p>
     * 公式：r = ORBIT_RADIUS_BASE + ORBIT_RADIUS_PER_SWORD * N
     */
    public static final double ORBIT_RADIUS_BASE = 2.6;

    /**
     * 每个护幕飞剑增加的环绕半径（米）
     */
    public static final double ORBIT_RADIUS_PER_SWORD = 0.4;

    // ====== 反应延迟参数 ======

    /**
     * 基础反应延迟（秒）
     * <p>
     * 公式：reaction = clamp(REACTION_BASE - REACTION_EXP_COEF*经验, REACTION_MIN, REACTION_MAX)
     */
    public static final double REACTION_BASE = 0.06;

    /**
     * 每点经验降低的反应延迟（秒）
     */
    public static final double REACTION_EXP_COEF = 0.00005;

    /**
     * 最小反应延迟（秒）
     * <p>
     * 即使经验很高，反应延迟也不会低于此值
     */
    public static final double REACTION_MIN = 0.02;

    /**
     * 最大反应延迟（秒）
     * <p>
     * 反应延迟的上限
     */
    public static final double REACTION_MAX = 0.06;

    // ====== 最大护幕数 ======

    /**
     * 护幕飞剑的最大数量
     * <p>
     * 即使根据道痕和经验计算出更多数量，也不会超过此值
     */
    public static final int MAX_WARDS = 4;

    // ====== 反击伤害参数 ======

    /**
     * 基础反击伤害（生命值）
     * <p>
     * 公式：D = COUNTER_DAMAGE_BASE + 0.05*道痕 + 0.01*经验
     */
    public static final double COUNTER_DAMAGE_BASE = 5.0;

    /**
     * 每点道痕增加的反击伤害
     */
    public static final double COUNTER_DAMAGE_TRAIL_COEF = 0.05;

    /**
     * 每点经验增加的反击伤害
     */
    public static final double COUNTER_DAMAGE_EXP_COEF = 0.01;

    // ====== 速度参数 ======

    /**
     * 每点道痕增加的速度（米/秒）
     */
    public static final double SPEED_TRAIL_COEF = 0.02;

    /**
     * 每点经验增加的速度（米/秒）
     */
    public static final double SPEED_EXP_COEF = 0.001;

    // ====== 拦截判定参数 ======

    /**
     * 拦截成功的距离阈值（米）
     * <p>
     * 当飞剑距离拦截点 < 此值时，视为拦截成功
     */
    public static final double INTERCEPT_SUCCESS_DISTANCE = 0.5;

    /**
     * 返回环绕位的距离阈值（米）
     * <p>
     * 当飞剑距离环绕槽位 < 此值时，视为已返回
     */
    public static final double RETURN_SUCCESS_DISTANCE = 0.5;

    /**
     * 拦截点提前距离（米）
     * <p>
     * 拦截点会在预测命中点前方此距离，确保有效拦截
     */
    public static final double INTERCEPT_OFFSET = 0.3;
}
