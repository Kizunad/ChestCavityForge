package net.tigereye.chestcavity.compat.guzhenren.item.shui_dao.calculator;

import net.tigereye.chestcavity.compat.guzhenren.util.DaoCooldownCalculator;

/**
 * 水道冷却时间计算工具类。
 *
 * <p>基于水道流派经验(liupai_shuidao)计算技能冷却时间,
 * 确保冷却时间不低于1秒(20 ticks)。
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * int liupaiExp = cc.getOrganScore(CCOrganScores.LIUPAI_SHUIDAO);
 * long cooldown = ShuiDaoCooldownOps.withShuiDaoExp(200L, liupaiExp);
 * player.getCooldowns().addCooldown(item, (int) cooldown);
 * }</pre>
 */
public final class ShuiDaoCooldownOps {

  private ShuiDaoCooldownOps() {}

  /**
   * 根据水道流派经验计算冷却时间。
   *
   * @param baseTicks 基础冷却时间(ticks)
   * @param liupaiShuiDaoExp 流派经验值(liupai_shuidao)
   * @return 实际冷却时间,最低20ticks(1秒)
   */
  public static long withShuiDaoExp(long baseTicks, int liupaiShuiDaoExp) {
    return DaoCooldownCalculator.calculateCooldown(baseTicks, liupaiShuiDaoExp);
  }

  /**
   * 根据水道流派经验计算冷却时间(自定义最小值)。
   *
   * @param baseTicks 基础冷却时间(ticks)
   * @param liupaiShuiDaoExp 流派经验值(liupai_shuidao)
   * @param minTicks 最低冷却时间(ticks)
   * @return 实际冷却时间,不低于minTicks
   */
  public static long withShuiDaoExp(
      long baseTicks,
      int liupaiShuiDaoExp,
      long minTicks) {
    return DaoCooldownCalculator.calculateCooldown(
        baseTicks,
        liupaiShuiDaoExp,
        minTicks);
  }
}
