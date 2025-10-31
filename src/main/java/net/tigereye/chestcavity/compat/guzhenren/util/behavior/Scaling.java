package net.tigereye.chestcavity.compat.guzhenren.util.behavior;

/**
 * 通用数值缩放与冷却计算接口。
 *
 * <p>用于统一各“道系”的伤害/范围等增益缩放与冷却缩减（CDR）计算，避免魔法数分散与重复实现。</p>
 */
public interface Scaling {

  /** 将值钳制到[min, max] 区间。 */
  double clamp(double value, double min, double max);

  /**
   * 增益缩放：scaled = base * (1 + coeff * dao)，并做区间钳制。
   * @param base   基础值（如基础伤害/半径/强度）
   * @param dao    对应“道”强度（可为 0+ 的实数）
   * @param coeff  道系系数（建议在 0..1 之间，可为 0）
   * @param min    结果下限（含）
   * @param max    结果上限（含）
   */
  double scaleByDao(double base, double dao, double coeff, double min, double max);

  /**
   * 冷却缩减（秒）：cooldown = max(base * (1 - clamp(cdr, 0, cap)), minCooldown)。
   * @param baseSeconds  基础冷却（秒）
   * @param cdr          冷却缩减比例（可叠加的和）
   * @param minSeconds   冷却下限（秒）
   * @param cdrCap       CDR 上限（0..1）
   */
  double cooldown(double baseSeconds, double cdr, double minSeconds, double cdrCap);

  /** 与 {@link #cooldown(double, double, double, double)} 相同，但以 tick 为单位。 */
  double cooldownTicks(double baseTicks, double cdr, double minTicks, double cdrCap);

  /**
   * 含道系的冷却缩减（秒）。
   * cdrTotal = clamp(extraCdr + daoCoeff * dao, 0, cdrCap)
   * cooldown = max(base * (1 - cdrTotal), minCooldown)
   */
  double withDaoCdr(
      double baseSeconds,
      double dao,
      double daoCoeff,
      double extraCdr,
      double minSeconds,
      double cdrCap);

  /** 秒转 tick（四舍五入）。 */
  long secondsToTicks(double seconds);

  /** tick 转秒。 */
  double ticksToSeconds(long ticks);

  /** 默认实现实例。 */
  Scaling DEFAULT = new StandardScaling();

  // ===== 以下为多道痕聚合的默认实现（无需具体实现类覆写） =====

  /**
   * 多个道痕的加权点积：sum(coeff[i] * dao[i])。
   * 长度不一致时，仅计算到两者最小长度。
   */
  default double dot(double[] dao, double[] coeff) {
    if (dao == null || coeff == null) return 0.0;
    int n = Math.min(dao.length, coeff.length);
    double s = 0.0;
    for (int i = 0; i < n; i++) {
      s += dao[i] * coeff[i];
    }
    return s;
  }

  /**
   * 多道痕增益：scaled = base * (1 + sum(coeff[i]*dao[i])) 并钳制。
   */
  default double scaleByDaoMulti(
      double base, double[] dao, double[] coeff, double min, double max) {
    double sum = dot(dao, coeff);
    // 复用单道实现：把 sum 当作“等效 dao”，系数取 1
    return scaleByDao(base, sum, 1.0, min, max);
  }

  /**
   * 多道痕参与的冷却缩减：
   * totalCdr = extraCdr + sum(coeff[i]*dao[i]);
   * cooldown = max(base * (1 - clamp(totalCdr,0,cap)), minSeconds)
   */
  default double withDaoCdrMulti(
      double baseSeconds,
      double[] dao,
      double[] coeff,
      double extraCdr,
      double minSeconds,
      double cdrCap) {
    double total = extraCdr + dot(dao, coeff);
    return cooldown(baseSeconds, total, minSeconds, cdrCap);
  }
}
