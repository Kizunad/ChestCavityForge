package net.tigereye.chestcavity.compat.guzhenren.item.gu_dao.calculator;

import net.tigereye.chestcavity.compat.guzhenren.util.DaoCooldownCalculator;

/**
 * 骨道冷却时间计算工具类。
 *
 * <p>基于骨道流派经验(liupai_gudao)计算技能冷却时间,
 * 确保冷却时间不低于1秒(20 ticks)。
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * int liupaiExp = cc.getOrganScore(CCOrganScores.LIUPAI_GUDAO);
 * long cooldown = GuDaoCooldownOps.withGuDaoExp(200L, liupaiExp);
 * player.getCooldowns().addCooldown(item, (int) cooldown);
 * }</pre>
 */
public final class GuDaoCooldownOps {

  private GuDaoCooldownOps() {}

  /**
   * 根据骨道流派经验计算冷却时间。
   *
   * @param baseTicks 基础冷却时间(ticks)
   * @param liupaiGudaoExp 流派经验值(liupai_gudao)
   * @return 实际冷却时间,最低20ticks(1秒)
   */
  public static long withGuDaoExp(long baseTicks, int liupaiGudaoExp) {
    return DaoCooldownCalculator.calculateCooldown(baseTicks, liupaiGudaoExp);
  }

  /**
   * 根据骨道流派经验计算冷却时间(自定义最小值)。
   *
   * @param baseTicks 基础冷却时间(ticks)
   * @param liupaiGudaoExp 流派经验值(liupai_gudao)
   * @param minTicks 最低冷却时间(ticks)
   * @return 实际冷却时间,不低于minTicks
   */
  public static long withGuDaoExp(
      long baseTicks,
      int liupaiGudaoExp,
      long minTicks) {
    return DaoCooldownCalculator.calculateCooldown(
        baseTicks,
        liupaiGudaoExp,
        minTicks);
  }
}
