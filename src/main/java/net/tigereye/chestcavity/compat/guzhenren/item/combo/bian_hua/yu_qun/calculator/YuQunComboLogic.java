package net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yu_qun.calculator;

import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yu_qun.tuning.YuQunTuning;

/**
 * 鱼群组合杀招的纯逻辑计算单元。
 * <p>
 * 负责根据输入的道痕值，结合{@link YuQunTuning}中定义的基础参数，计算出最终的技能效果参数。
 * </p>
 */
public final class YuQunComboLogic {
    private YuQunComboLogic() {}

    /**
     * 根据道痕值和协同数计算技能的最终参数。
     * <p>
     * 此方法采用多阶段计算模型：
     * 1.  **输入软化**：将原始的道痕值和协同数通过非线性函数（log, sqrt）映射，以实现“递减收益”，防止数值爆炸。
     * 2.  **乘数计算**：将软化后的值通过带权重（可调）的公式合并，分别计算出增益乘数 G 和减益乘数 A，最终乘积为 M = G * A。
     * 3.  **核心参数**：将乘数 M 应用于范围和推力，并额外叠加线性的协同加成。
     * 4.  **宽度调整**：宽度（cos值）采用独立的加减模型，使各个道痕的影响更解耦、更直观。
     * 5.  **效果阈值**：减速和粒子效果基于软化后的道痕值和协同数，通过阈值判断触发。
     * </p>
     *
     * @param waterDaoHen 水道道痕值。
     * @param changeDaoHen 变化道道痕值。
     * @param fireDaoHen 炎道道痕值。
     * @param synergyCount 协同数。
     * @return 一个包含最终计算出的所有技能参数的 {@link Parameters} 对象。
     */
    public static Parameters computeParameters(
        double waterDaoHen, double changeDaoHen, double fireDaoHen, int synergyCount) {

        // 1) 安全钳制（非负），并做可调“软化”映射：对大值递减收益，避免一把梭
        double w = Math.max(0, waterDaoHen);
        double c = Math.max(0, changeDaoHen);
        double f = Math.max(0, fireDaoHen);
        int s = Math.max(0, synergyCount);

        // 可调基准：X0 越大，前期越平缓；ln 保证单调且有递减收益
        final double X0 = YuQunTuning.DAO_HEN_SOFTENING_X0;
        double FW = Math.log1p(w / X0);
        double FC = Math.log1p(c / X0);
        double FF = Math.log1p(f / X0);
        double GS = Math.sqrt(s); // 协同用 sqrt，前几级更显著

        // 2) 对“越大越好”的量，用 G(增) 与 A(抑) 分离，再相乘
        final double aW = YuQunTuning.AW_WATER_DAO_HEN_FACTOR;
        final double aC = YuQunTuning.AC_CHANGE_DAO_HEN_FACTOR;
        final double aS = YuQunTuning.AS_SYNERGY_FACTOR;
        final double bF = YuQunTuning.BF_FIRE_DAO_HEN_FACTOR;

        double G = 1.0 + aW * FW + aC * FC + aS * GS;
        double A = Math.exp(-bF * FF);
        double M = G * A;

        // 3) 具体参数：范围、推力用 M；基础加成再给一点协同直加，手感更线性
        double finalRange = (YuQunTuning.BASE_RANGE + 0.6 * s) * M;
        double finalPush = (YuQunTuning.BASE_PUSH + 0.02 * s) * M;

        // 4) 宽度：cos 阈值用“显式加减”，而不是乘，以实现“水/变让它更宽，炎让它收窄”
        final double kW = YuQunTuning.KW_WATER_DAO_HEN_WIDTH_FACTOR;
        final double kC = YuQunTuning.KC_CHANGE_DAO_HEN_WIDTH_FACTOR;
        final double kS = YuQunTuning.KS_SYNERGY_WIDTH_FACTOR;
        final double kF = YuQunTuning.KF_FIRE_DAO_HEN_WIDTH_FACTOR;
        double baseCos = YuQunTuning.BASE_WIDTH_COS;
        double cosMin = YuQunTuning.COS_MIN;
        double cosMax = Math.max(YuQunTuning.COS_MAX_CLAMP_FLOOR, baseCos);

        double finalWidthCos = baseCos
                - kW * FW
                - kC * FC
                - kS * GS
                + kF * FF;

        finalWidthCos = Math.max(cosMin, Math.min(cosMax, finalWidthCos));

        // 5) 减速与粒子：分段/阈值更直观
        int slowDuration = (int) Math.round(60 + 4 * s + 30 * FW);
        int slowAmplifier = (int) Math.floor((s + 2 * FW) / 5.0);
        boolean particles = s >= 8 || (FW + FC) > 0.8;

        return new Parameters(finalRange, finalWidthCos, finalPush, slowDuration, slowAmplifier, particles);
    }

    public static boolean isWithinCone(net.minecraft.world.phys.Vec3 origin, net.minecraft.world.phys.Vec3 direction, net.minecraft.world.phys.Vec3 target, double range, double width) {
        net.minecraft.world.phys.Vec3 toTarget = target.subtract(origin);
        if (toTarget.lengthSqr() > range * range) {
            return false;
        }
        double dot = toTarget.normalize().dot(direction);
        return dot >= width;
    }

    /**
     * 存储鱼群技能最终计算参数的记录。
     *
     * @param range 技能的作用范围。
     * @param width 技能作用锥角的宽度（余弦值）。
     * @param pushStrength 技能的推动强度。
     * @param slowDurationTicks 减速效果的持续时间（ticks）。
     * @param slowAmplifier 减速效果的等级。
     * @param spawnSplashParticles 是否生成水花粒子效果。
     */
    public record Parameters(
        double range,
        double width,
        double pushStrength,
        int slowDurationTicks,
        int slowAmplifier,
        boolean spawnSplashParticles) {}
}
