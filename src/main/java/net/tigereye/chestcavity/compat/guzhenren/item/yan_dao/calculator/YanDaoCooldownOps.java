package net.tigereye.chestcavity.compat.guzhenren.item.yan_dao.calculator;

import net.tigereye.chestcavity.compat.guzhenren.util.DaoCooldownCalculator;

/**
 * 炎道冷却时间计算工具类。
 *
 * <p>基于炎道流派经验(liupai_yandao)计算技能冷却时间,
 * 确保冷却时间不低于1秒(20 ticks)。
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * int liupaiExp = cc.getOrganScore(CCOrganScores.LIUPAI_YANDAO);
 * long cooldown = YanDaoCooldownOps.withYanDaoExp(200L, liupaiExp);
 * player.getCooldowns().addCooldown(item, (int) cooldown);
 * }</pre>
 */
public final class YanDaoCooldownOps {

  private YanDaoCooldownOps() {}

  /**
   * 根据炎道流派经验计算冷却时间。
   *
   * @param baseTicks 基础冷却时间(ticks)
   * @param liupaiYandaoExp 流派经验值(liupai_yandao)
   * @return 实际冷却时间,最低20ticks(1秒)
   */
  public static long withYanDaoExp(long baseTicks, int liupaiYandaoExp) {
    return DaoCooldownCalculator.calculateCooldown(baseTicks, liupaiYandaoExp);
  }

  /**
   * 根据炎道流派经验计算冷却时间(自定义最小值)。
   *
   * @param baseTicks 基础冷却时间(ticks)
   * @param liupaiYandaoExp 流派经验值(liupai_yandao)
   * @param minTicks 最低冷却时间(ticks)
   * @return 实际冷却时间,不低于minTicks
   */
  public static long withYanDaoExp(
      long baseTicks,
      int liupaiYandaoExp,
      long minTicks) {
    return DaoCooldownCalculator.calculateCooldown(
        baseTicks,
        liupaiYandaoExp,
        minTicks);
  }
}
