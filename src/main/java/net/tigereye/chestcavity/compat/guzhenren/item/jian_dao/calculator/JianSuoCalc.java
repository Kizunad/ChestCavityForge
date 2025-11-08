package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.calculator;

import static net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.tuning.JianSuoGuTuning.*;

/**
 * 剑梭蛊公式计算工具类。
 *
 * <p>提供：
 * <ul>
 *   <li>突进距离计算（基于道痕）</li>
 *   <li>路径伤害计算（基于道痕 + 速度）</li>
 *   <li>躲避几率计算（基于道痕）</li>
 *   <li>减伤比例计算（基于道痕）</li>
 *   <li>冷却时间计算（基于道痕）</li>
 * </ul>
 *
 * <p>便于单元测试与调优。
 */
public final class JianSuoCalc {

  private JianSuoCalc() {}

  /**
   * 计算突进距离。
   *
   * <p>公式：clamp(BASE_DASH_DISTANCE * (1 + 0.25 * DH100), 0, MAX_DASH_DISTANCE)
   * <p>其中 DH100 = floor(daohen / 100)
   *
   * @param daohen 剑道道痕
   * @return 突进距离（格）
   */
  public static double dashDistance(double daohen) {
    double dh100 = Math.floor(daohen / 100.0);
    double dist = BASE_DASH_DISTANCE * (1.0 + DASH_DIST_PER_100_DAOHEN * dh100);
    return Math.min(Math.max(dist, 0.0), MAX_DASH_DISTANCE);
  }

  /**
   * 计算路径伤害。
   *
   * <p>公式：BASE_ATK * (1 + 0.35 * DH100) * (1 + vScale)
   * <p>其中 vScale = min(velocity / 10.0, VELOCITY_SCALE_MAX)
   *
   * @param daohen 剑道道痕
   * @param velocity 当前移动速度（m/s）
   * @return 伤害系数
   */
  public static double pathDamage(double daohen, double velocity) {
    double dh100 = Math.floor(daohen / 100.0);
    double vScale = Math.min(velocity / 10.0, VELOCITY_SCALE_MAX);
    return BASE_ATK * (1.0 + DAMAGE_PER_100_DAOHEN * dh100) * (1.0 + vScale);
  }

  /**
   * 计算躲避几率。
   *
   * <p>公式：clamp(EVADE_CHANCE_BASE + EVADE_CHANCE_PER_100 * DH100, 0, EVADE_CHANCE_MAX)
   *
   * @param daohen 剑道道痕
   * @return 躲避几率（0-1）
   */
  public static double evadeChance(double daohen) {
    double dh100 = Math.floor(daohen / 100.0);
    double chance = EVADE_CHANCE_BASE + EVADE_CHANCE_PER_100 * dh100;
    return Math.min(Math.max(chance, 0.0), EVADE_CHANCE_MAX);
  }

  /**
   * 计算减伤比例。
   *
   * <p>公式：clamp(EVADE_REDUCE_MIN + EVADE_REDUCE_PER_100 * DH100, EVADE_REDUCE_MIN, EVADE_REDUCE_MAX)
   *
   * @param daohen 剑道道痕
   * @return 减伤比例（0-1）
   */
  public static double evadeReduce(double daohen) {
    double dh100 = Math.floor(daohen / 100.0);
    double reduce = EVADE_REDUCE_MIN + EVADE_REDUCE_PER_100 * dh100;
    return Math.min(Math.max(reduce, EVADE_REDUCE_MIN), EVADE_REDUCE_MAX);
  }

  /**
   * 计算主动技能冷却时间。
   *
   * <p>随道痕递减，范围：[ACTIVE_COOLDOWN_MIN_TICKS, ACTIVE_COOLDOWN_MAX_TICKS]
   * <p>公式：MAX - (DH100 / 10.0) * (MAX - MIN)
   *
   * @param daohen 剑道道痕
   * @return 冷却时间（ticks）
   */
  public static int activeCooldown(double daohen) {
    double dh100 = Math.floor(daohen / 100.0);
    double ratio = Math.min(dh100 / 10.0, 1.0); // 最多 1000 道痕时达到最小冷却
    int cd = (int) (ACTIVE_COOLDOWN_MAX_TICKS - ratio * (ACTIVE_COOLDOWN_MAX_TICKS - ACTIVE_COOLDOWN_MIN_TICKS));
    return Math.max(cd, ACTIVE_COOLDOWN_MIN_TICKS);
  }

  /**
   * 将秒转换为 ticks。
   *
   * @param seconds 秒数
   * @return ticks
   */
  public static int secondsToTicks(double seconds) {
    return (int) (seconds * 20.0);
  }

  /**
   * 将 ticks 转换为秒。
   *
   * @param ticks ticks
   * @return 秒数
   */
  public static double ticksToSeconds(int ticks) {
    return ticks / 20.0;
  }
}
