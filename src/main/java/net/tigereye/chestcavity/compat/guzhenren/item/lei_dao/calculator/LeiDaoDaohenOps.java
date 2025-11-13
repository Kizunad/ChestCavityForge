package net.tigereye.chestcavity.compat.guzhenren.item.lei_dao.calculator;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.util.DaohenCalculator;

/**
 * 雷道道痕计算工具类。
 *
 * <p>汇总雷道相关器官的道痕值,用于技能效果增幅计算。
 *
 * <p>雷道道痕应用于:
 * <ul>
 *   <li>电流蛊: 伤害增幅</li>
 *   <li>雷盾蛊: 反伤 + 吸收伤害增幅</li>
 * </ul>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * double daohen = LeiDaoDaohenOps.computeDaohen(cc);
 * float finalDamage = baseDamage * (1.0f + (float) daohen * 0.1f);
 * }</pre>
 */
public final class LeiDaoDaohenOps extends DaohenCalculator {

  private static final LeiDaoDaohenOps INSTANCE = new LeiDaoDaohenOps();

  /** 电流蛊每个提供的道痕值。 */
  private static final double DIANLIUGU_DAOHEN_PER_STACK = 1.0;

  /** 雷盾蛊每个提供的道痕值。 */
  private static final double LEIDUNGU_DAOHEN_PER_STACK = 1.5;

  /** 电流蛊物品ID。 */
  private static final ResourceLocation DIANLIUGU_ID =
      ResourceLocation.fromNamespaceAndPath("guzhenren", "dianliugu");

  /** 雷盾蛊物品ID。 */
  private static final ResourceLocation LEIDUNGU_ID =
      ResourceLocation.fromNamespaceAndPath("guzhenren", "leidungu");

  private LeiDaoDaohenOps() {
    // 注册电流蛊的道痕提供器
    registerProvider(cc -> {
      if (cc == null) {
        return 0.0;
      }

      int count = 0;
      Item item = BuiltInRegistries.ITEM.get(DIANLIUGU_ID);

      for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
        ItemStack organ = cc.inventory.getItem(i);
        if (organ.getItem() == item) {
          count += organ.getCount();
        }
      }

      return calculateDaohen(count, DIANLIUGU_DAOHEN_PER_STACK);
    });

    // 注册雷盾蛊的道痕提供器
    registerProvider(cc -> {
      if (cc == null) {
        return 0.0;
      }

      int count = 0;
      Item item = BuiltInRegistries.ITEM.get(LEIDUNGU_ID);

      for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
        ItemStack organ = cc.inventory.getItem(i);
        if (organ.getItem() == item) {
          count += organ.getCount();
        }
      }

      return calculateDaohen(count, LEIDUNGU_DAOHEN_PER_STACK);
    });
  }

  /**
   * 计算雷道道痕总值。
   *
   * @param cc 胸腔实例
   * @return 道痕总值
   */
  public static double computeDaohen(ChestCavityInstance cc) {
    return INSTANCE.compute(cc);
  }
}
