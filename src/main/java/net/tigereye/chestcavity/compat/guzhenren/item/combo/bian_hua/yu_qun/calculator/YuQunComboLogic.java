package net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yu_qun.calculator;

import net.minecraft.world.phys.Vec3;

/** 提供鱼群组合杀招的纯逻辑计算，便于单元测试覆盖。 */
public final class YuQunComboLogic {

  private static final int MAX_SYNERGY = 10;

  private YuQunComboLogic() {}

  /** 依据协同数量计算能力参数。 */
  public static Parameters computeParameters(int synergyCount) {
    int capped = Math.max(0, Math.min(MAX_SYNERGY, synergyCount));
    double range = 10.0 + capped * 0.6;
    double width = 1.75 + capped * 0.1;
    double push = 0.45 + capped * 0.02;
    int slowDuration = 60 + capped * 4;
    int slowAmplifier = capped >= 6 ? 1 : 0;
    boolean spawnSplash = capped >= 4;
    return new Parameters(range, width, push, slowDuration, slowAmplifier, spawnSplash);
  }

  /**
   * 判断目标是否处于扇形攻击锥内。
   *
   * @param origin 攻击者视角位置
   * @param direction 已归一化的朝向向量
   * @param target 目标眼睛位置
   * @param range 最远距离
   * @param width 横向半径
   */
  public static boolean isWithinCone(
      Vec3 origin, Vec3 direction, Vec3 target, double range, double width) {
    Vec3 toTarget = target.subtract(origin);
    double forward = toTarget.dot(direction);
    if (forward <= 0.0 || forward > range) {
      return false;
    }
    Vec3 lateral = toTarget.subtract(direction.scale(forward));
    return lateral.lengthSqr() <= width * width;
  }

  public record Parameters(
      double range,
      double width,
      double pushStrength,
      int slowDurationTicks,
      int slowAmplifier,
      boolean spawnSplashParticles) {}
}
