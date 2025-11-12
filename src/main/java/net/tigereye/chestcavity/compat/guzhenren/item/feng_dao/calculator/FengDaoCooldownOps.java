package net.tigereye.chestcavity.compat.guzhenren.item.feng_dao.calculator;

import net.tigereye.chestcavity.compat.guzhenren.util.DaoCooldownCalculator;

/**
 * 风道冷却时间计算工具类。
 *
 * <p>基于风道流派经验(liupai_fengdao)计算技能冷却时间,
 * 确保冷却时间不低于1秒(20 ticks)。
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * int liupaiExp = cc.getOrganScore(CCOrganScores.LIUPAI_FENGDAO);
 * long cooldown = FengDaoCooldownOps.withFengDaoExp(200L, liupaiExp);
 * player.getCooldowns().addCooldown(item, (int) cooldown);
 * }</pre>
 */
public final class FengDaoCooldownOps {

  private FengDaoCooldownOps() {}

  /**
   * 根据风道流派经验计算冷却时间。
   *
   * @param baseTicks 基础冷却时间(ticks)
   * @param liupaiFengdaoExp 流派经验值(liupai_fengdao)
   * @return 实际冷却时间,最低20ticks(1秒)
   */
  public static long withFengDaoExp(long baseTicks, int liupaiFengdaoExp) {
    return DaoCooldownCalculator.calculateCooldown(baseTicks, liupaiFengdaoExp);
  }

  /**
   * 根据风道流派经验计算冷却时间(自定义最小值)。
   *
   * @param baseTicks 基础冷却时间(ticks)
   * @param liupaiFengdaoExp 流派经验值(liupai_fengdao)
   * @param minTicks 最低冷却时间(ticks)
   * @return 实际冷却时间,不低于minTicks
   */
  public static long withFengDaoExp(
      long baseTicks,
      int liupaiFengdaoExp,
      long minTicks) {
    return DaoCooldownCalculator.calculateCooldown(
        baseTicks,
        liupaiFengdaoExp,
        minTicks);
  }
}
