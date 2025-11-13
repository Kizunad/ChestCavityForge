package net.tigereye.chestcavity.compat.guzhenren.item.li_dao.calculator;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.util.DaohenCalculator;

/**
 * 力道道痕计算工具类。
 *
 * <p>汇总力道相关器官的道痕值,用于技能效果增幅计算。
 *
 * <p>当前注册的器官:
 * <ul>
 *   <li>化石蛊(hua_shi_gu): 每个提供 1.0 道痕</li>
 *   <li>熊豪蛊(xiong_hao_gu): 每个提供 2.0 道痕</li>
 *   <li>蛮力天牛蛊(man_li_tian_niu_gu): 每个提供 1.0 道痕</li>
 * </ul>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * double daohen = LiDaoDaohenOps.computeDaohen(cc);
 * float finalDamage = baseDamage * (1.0f + (float) daohen * 0.1f);
 * }</pre>
 */
public final class LiDaoDaohenOps extends DaohenCalculator {

  private static final LiDaoDaohenOps INSTANCE = new LiDaoDaohenOps();

  /** 化石蛊每个提供的道痕值。 */
  private static final double HUA_SHI_GU_DAOHEN_PER_STACK = 1.0;

  /** 熊豪蛊每个提供的道痕值。 */
  private static final double XIONG_HAO_GU_DAOHEN_PER_STACK = 2.0;

  /** 蛮力天牛蛊每个提供的道痕值。 */
  private static final double MAN_LI_TIAN_NIU_GU_DAOHEN_PER_STACK = 1.0;

  /** 化石蛊物品ID。 */
  private static final ResourceLocation HUA_SHI_GU_ID =
      ResourceLocation.fromNamespaceAndPath("guzhenren", "hua_shi_gu");

  /** 熊豪蛊物品ID。 */
  private static final ResourceLocation XIONG_HAO_GU_ID =
      ResourceLocation.fromNamespaceAndPath("guzhenren", "xiong_hao_gu");

  /** 蛮力天牛蛊物品ID。 */
  private static final ResourceLocation MAN_LI_TIAN_NIU_GU_ID =
      ResourceLocation.fromNamespaceAndPath("guzhenren", "man_li_tian_niu_gu");

  private LiDaoDaohenOps() {
    // 注册化石蛊的道痕提供器
    registerProvider(cc -> {
      if (cc == null) {
        return 0.0;
      }
      int count = 0;
      Item item = BuiltInRegistries.ITEM.get(HUA_SHI_GU_ID);
      for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
        ItemStack organ = cc.inventory.getItem(i);
        if (organ.getItem() == item) {
          count += organ.getCount();
        }
      }
      return calculateDaohen(count, HUA_SHI_GU_DAOHEN_PER_STACK);
    });

    // 注册熊豪蛊的道痕提供器
    registerProvider(cc -> {
      if (cc == null) {
        return 0.0;
      }
      int count = 0;
      Item item = BuiltInRegistries.ITEM.get(XIONG_HAO_GU_ID);
      for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
        ItemStack organ = cc.inventory.getItem(i);
        if (organ.getItem() == item) {
          count += organ.getCount();
        }
      }
      return calculateDaohen(count, XIONG_HAO_GU_DAOHEN_PER_STACK);
    });

    // 注册蛮力天牛蛊的道痕提供器
    registerProvider(cc -> {
      if (cc == null) {
        return 0.0;
      }
      int count = 0;
      Item item = BuiltInRegistries.ITEM.get(MAN_LI_TIAN_NIU_GU_ID);
      for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
        ItemStack organ = cc.inventory.getItem(i);
        if (organ.getItem() == item) {
          count += organ.getCount();
        }
      }
      return calculateDaohen(count, MAN_LI_TIAN_NIU_GU_DAOHEN_PER_STACK);
    });

    // 未来可以在这里继续注册其他力道器官
  }

  /**
   * 计算力道道痕总值。
   *
   * @param cc 胸腔实例
   * @return 道痕总值
   */
  public static double computeDaohen(ChestCavityInstance cc) {
    return INSTANCE.compute(cc);
  }
}
