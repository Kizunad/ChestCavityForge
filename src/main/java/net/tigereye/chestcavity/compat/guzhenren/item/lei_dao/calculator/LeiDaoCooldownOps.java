package net.tigereye.chestcavity.compat.guzhenren.item.lei_dao.calculator;

import net.tigereye.chestcavity.compat.guzhenren.util.DaoCooldownCalculator;

/**
 * 雷道冷却时间计算工具类。
 *
 * <p>基于雷道流派经验(liupai_leidao)计算技能冷却时间,
 * 确保冷却时间不低于1秒(20 ticks)。
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * int liupaiExp = cc.getOrganScore(CCOrganScores.LIUPAI_LEIDAO);
 * long cooldown = LeiDaoCooldownOps.withLeiDaoExp(200L, liupaiExp);
 * player.getCooldowns().addCooldown(item, (int) cooldown);
 * }</pre>
 */
public final class LeiDaoCooldownOps {

  private LeiDaoCooldownOps() {}

  /**
   * 根据雷道流派经验计算冷却时间。
   *
   * @param baseTicks 基础冷却时间(ticks)
   * @param liupaiLeidaoExp 流派经验值(liupai_leidao)
   * @return 实际冷却时间,最低20ticks(1秒)
   */
  public static long withLeiDaoExp(long baseTicks, int liupaiLeidaoExp) {
    return DaoCooldownCalculator.calculateCooldown(baseTicks, liupaiLeidaoExp);
  }

  /**
   * 根据雷道流派经验计算冷却时间(自定义最小值)。
   *
   * @param baseTicks 基础冷却时间(ticks)
   * @param liupaiLeidaoExp 流派经验值(liupai_leidao)
   * @param minTicks 最低冷却时间(ticks)
   * @return 实际冷却时间,不低于minTicks
   */
  public static long withLeiDaoExp(
      long baseTicks,
      int liupaiLeidaoExp,
      long minTicks) {
    return DaoCooldownCalculator.calculateCooldown(
        baseTicks,
        liupaiLeidaoExp,
        minTicks);
  }
}
