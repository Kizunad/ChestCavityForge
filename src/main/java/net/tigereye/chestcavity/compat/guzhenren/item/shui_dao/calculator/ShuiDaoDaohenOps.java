package net.tigereye.chestcavity.compat.guzhenren.item.shui_dao.calculator;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.util.DaohenCalculator;

/**
 * 水道道痕计算工具类。
 *
 * <p>汇总水道相关器官的道痕值,用于技能效果增幅计算。
 *
 * <p>当前注册的器官:
 * <ul>
 *   <li>灵涎蛊(ling_xian_gu): 每个提供 1.0 道痕</li>
 *   <li>水体蛊(shui_ti_gu): 每个提供 1.0 道痕</li>
 *   <li>节泽蛊(jiezegu): 每个提供 1.0 道痕</li>
 *   <li>泉涌冥蛊(quan_yong_ming_gu): 每个提供 1.0 道痕</li>
 * </ul>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * double daohen = ShuiDaoDaohenOps.computeDaohen(cc);
 * float finalDamage = baseDamage * (1.0f + (float) daohen * 0.1f);
 * }</pre>
 */
public final class ShuiDaoDaohenOps extends DaohenCalculator {

  private static final ShuiDaoDaohenOps INSTANCE = new ShuiDaoDaohenOps();

  /** 灵涎蛊每个提供的道痕值。 */
  private static final double LING_XIAN_GU_DAOHEN_PER_STACK = 1.0;

  /** 水体蛊每个提供的道痕值。 */
  private static final double SHUI_TI_GU_DAOHEN_PER_STACK = 1.0;

  /** 节泽蛊每个提供的道痕值。 */
  private static final double JIEZE_GU_DAOHEN_PER_STACK = 1.0;

  /** 泉涌冥蛊每个提供的道痕值。 */
  private static final double QUAN_YONG_MING_GU_DAOHEN_PER_STACK = 1.0;

  /** 灵涎蛊物品ID。 */
  private static final ResourceLocation LING_XIAN_GU_ID =
      ResourceLocation.fromNamespaceAndPath("guzhenren", "ling_xian_gu");

  /** 水体蛊物品ID。 */
  private static final ResourceLocation SHUI_TI_GU_ID =
      ResourceLocation.fromNamespaceAndPath("guzhenren", "shui_ti_gu");

  /** 节泽蛊物品ID。 */
  private static final ResourceLocation JIEZE_GU_ID =
      ResourceLocation.fromNamespaceAndPath("guzhenren", "jiezegu");

  /** 泉涌冥蛊物品ID。 */
  private static final ResourceLocation QUAN_YONG_MING_GU_ID =
      ResourceLocation.fromNamespaceAndPath("guzhenren", "quan_yong_ming_gu");

  private ShuiDaoDaohenOps() {
    // 注册灵涎蛊的道痕提供器
    registerProvider(cc -> {
      if (cc == null) {
        return 0.0;
      }
      int count = 0;
      Item item = BuiltInRegistries.ITEM.get(LING_XIAN_GU_ID);
      for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
        ItemStack organ = cc.inventory.getItem(i);
        if (organ.getItem() == item) {
          count += organ.getCount();
        }
      }
      return calculateDaohen(count, LING_XIAN_GU_DAOHEN_PER_STACK);
    });

    // 注册水体蛊的道痕提供器
    registerProvider(cc -> {
      if (cc == null) {
        return 0.0;
      }
      int count = 0;
      Item item = BuiltInRegistries.ITEM.get(SHUI_TI_GU_ID);
      for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
        ItemStack organ = cc.inventory.getItem(i);
        if (organ.getItem() == item) {
          count += organ.getCount();
        }
      }
      return calculateDaohen(count, SHUI_TI_GU_DAOHEN_PER_STACK);
    });

    // 注册节泽蛊的道痕提供器
    registerProvider(cc -> {
      if (cc == null) {
        return 0.0;
      }
      int count = 0;
      Item item = BuiltInRegistries.ITEM.get(JIEZE_GU_ID);
      for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
        ItemStack organ = cc.inventory.getItem(i);
        if (organ.getItem() == item) {
          count += organ.getCount();
        }
      }
      return calculateDaohen(count, JIEZE_GU_DAOHEN_PER_STACK);
    });

    // 注册泉涌冥蛊的道痕提供器
    registerProvider(cc -> {
      if (cc == null) {
        return 0.0;
      }
      int count = 0;
      Item item = BuiltInRegistries.ITEM.get(QUAN_YONG_MING_GU_ID);
      for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
        ItemStack organ = cc.inventory.getItem(i);
        if (organ.getItem() == item) {
          count += organ.getCount();
        }
      }
      return calculateDaohen(count, QUAN_YONG_MING_GU_DAOHEN_PER_STACK);
    });

    // 未来可以在这里继续注册其他水道器官
  }

  /**
   * 计算水道道痕总值。
   *
   * @param cc 胸腔实例
   * @return 道痕总值
   */
  public static double computeDaohen(ChestCavityInstance cc) {
    return INSTANCE.compute(cc);
  }
}
