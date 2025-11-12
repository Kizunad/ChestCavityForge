package net.tigereye.chestcavity.compat.guzhenren.item.feng_dao.calculator;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.util.DaohenCalculator;

/**
 * 风道道痕计算工具类。
 *
 * <p>汇总风道相关器官的道痕值,用于技能效果增幅计算。
 *
 * <p>当前注册的器官:
 * <ul>
 *   <li>清风轮蛊(qing_feng_lun_gu): 每个提供 1.0 道痕</li>
 * </ul>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * double daohen = FengDaoDaohenOps.compute(cc);
 * float finalDamage = baseDamage * (1.0f + (float) daohen * 0.1f);
 * }</pre>
 */
public final class FengDaoDaohenOps extends DaohenCalculator {

  private static final FengDaoDaohenOps INSTANCE = new FengDaoDaohenOps();

  /** 清风轮蛊每个提供的道痕值。 */
  private static final double QING_FENG_LUN_GU_DAOHEN_PER_STACK = 1.0;

  /** 清风轮蛊物品ID。 */
  private static final ResourceLocation QING_FENG_LUN_GU_ID =
      ResourceLocation.fromNamespaceAndPath("guzhenren", "qing_feng_lun_gu");

  private FengDaoDaohenOps() {
    // 注册清风轮蛊的道痕提供器
    registerProvider(cc -> {
      if (cc == null) {
        return 0.0;
      }

      // 遍历胸腔背包,统计清风轮蛊数量
      int qingFengLunCount = 0;
      Item qingFengLunItem = BuiltInRegistries.ITEM.get(QING_FENG_LUN_GU_ID);

      for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
        ItemStack organ = cc.inventory.getItem(i);
        if (organ.getItem() == qingFengLunItem) {
          qingFengLunCount += organ.getCount();
        }
      }

      return calculateDaohen(qingFengLunCount, QING_FENG_LUN_GU_DAOHEN_PER_STACK);
    });

    // 未来可以在这里继续注册其他风道器官
    // 示例:
    // registerProvider(cc -> calculateWindBladeGuDaohen(cc));
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
