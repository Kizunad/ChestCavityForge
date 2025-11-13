package net.tigereye.chestcavity.compat.guzhenren.item.li_dao.calculator;

import net.tigereye.chestcavity.compat.guzhenren.util.DaoCooldownCalculator;

/**
 * 力道冷却时间计算工具类。
 *
 * <p>基于力道流派经验(liupai_lidao)计算技能冷却时间,
 * 确保冷却时间不低于1秒(20 ticks)。
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * int liupaiExp = cc.getOrganScore(CCOrganScores.LIUPAI_LIDAO);
 * long cooldown = LiDaoCooldownOps.withLiDaoExp(200L, liupaiExp);
 * player.getCooldowns().addCooldown(item, (int) cooldown);
 * }</pre>
 */
public final class LiDaoCooldownOps {

  private LiDaoCooldownOps() {}

  /**
   * 根据力道流派经验计算冷却时间。
   *
   * @param baseTicks 基础冷却时间(ticks)
   * @param liupaiLidaoExp 流派经验值(liupai_lidao)
   * @return 实际冷却时间,最低20ticks(1秒)
   */
  public static long withLiDaoExp(long baseTicks, int liupaiLidaoExp) {
    return DaoCooldownCalculator.calculateCooldown(baseTicks, liupaiLidaoExp);
  }

  /**
   * 根据力道流派经验计算冷却时间(自定义最小值)。
   *
   * @param baseTicks 基础冷却时间(ticks)
   * @param liupaiLidaoExp 流派经验值(liupai_lidao)
   * @param minTicks 最低冷却时间(ticks)
   * @return 实际冷却时间,不低于minTicks
   */
  public static long withLiDaoExp(
      long baseTicks,
      int liupaiLidaoExp,
      long minTicks) {
    return DaoCooldownCalculator.calculateCooldown(
        baseTicks,
        liupaiLidaoExp,
        minTicks);
  }
}
