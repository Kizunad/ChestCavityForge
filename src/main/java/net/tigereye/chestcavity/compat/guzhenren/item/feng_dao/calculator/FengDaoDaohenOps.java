package net.tigereye.chestcavity.compat.guzhenren.item.feng_dao.calculator;

import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.util.DaohenCalculator;

/**
 * 风道道痕计算工具类。
 *
 * <p>汇总风道相关器官的道痕值,用于技能效果增幅计算。
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * double daohen = FengDaoDaohenOps.compute(cc);
 * float finalDamage = baseDamage * (1.0f + (float) daohen * 0.1f);
 * }</pre>
 */
public final class FengDaoDaohenOps extends DaohenCalculator {

  private static final FengDaoDaohenOps INSTANCE = new FengDaoDaohenOps();

  private FengDaoDaohenOps() {
    // 注册风道相关器官的道痕提供器
    // 当前暂无器官注册道痕,返回固定值用于测试
    // TODO: 根据实际需求添加器官道痕计算
    // 示例:
    // registerProvider(cc ->
    //     calculateDaohen(
    //         cc.getOrganScore(CCOrganScores.QING_FENG_LUN_GU),
    //         1.0));
  }

  /**
   * 计算风道道痕总值。
   *
   * @param cc 胸腔实例
   * @return 道痕总值
   */
  public static double compute(ChestCavityInstance cc) {
    return INSTANCE.compute(cc);
  }
}
