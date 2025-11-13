package net.tigereye.chestcavity.compat.guzhenren.item.yan_dao.calculator;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.util.DaohenCalculator;

/**
 * 炎道道痕计算工具类。
 *
 * <p>汇总炎道相关器官的道痕值,用于技能效果增幅计算。
 *
 * <p>当前注册的器官:
 * <ul>
 *   <li>火心蛊(huoxingu): 每个提供 1.0 道痕</li>
 *   <li>火人蛊(huorengu): 每个提供 1.0 道痕</li>
 *   <li>火蛊(huo_gu): 每个提供 1.0 道痕</li>
 *   <li>火油蛊(huo_you_gu): 每个提供 1.0 道痕</li>
 *   <li>火龙蛊(huolonggu): 每个提供 1.0 道痕</li>
 *   <li>弹跳火炭蛊(dan_qiao_huo_tan_gu): 每个提供 1.0 道痕</li>
 *   <li>焚身蛊(fen_shen_gu): 每个提供 1.0 道痕</li>
 * </ul>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * double daohen = YanDaoDaohenOps.computeDaohen(cc);
 * float finalDamage = baseDamage * (1.0f + (float) daohen * 0.1f);
 * }</pre>
 */
public final class YanDaoDaohenOps extends DaohenCalculator {

  private static final YanDaoDaohenOps INSTANCE = new YanDaoDaohenOps();

  /** 火心蛊每个提供的道痕值。 */
  private static final double HUOXINGU_DAOHEN_PER_STACK = 1.0;

  /** 火人蛊每个提供的道痕值。 */
  private static final double HUORENGU_DAOHEN_PER_STACK = 1.0;

  /** 火蛊每个提供的道痕值。 */
  private static final double HUO_GU_DAOHEN_PER_STACK = 1.0;

  /** 火油蛊每个提供的道痕值。 */
  private static final double HUO_YOU_GU_DAOHEN_PER_STACK = 1.0;

  /** 火龙蛊每个提供的道痕值。 */
  private static final double HUOLONGGU_DAOHEN_PER_STACK = 1.0;

  /** 弹跳火炭蛊每个提供的道痕值。 */
  private static final double DAN_QIAO_HUO_TAN_GU_DAOHEN_PER_STACK = 1.0;

  /** 焚身蛊每个提供的道痕值。 */
  private static final double FEN_SHEN_GU_DAOHEN_PER_STACK = 1.0;

  /** 火心蛊物品ID。 */
  private static final ResourceLocation HUOXINGU_ID =
      ResourceLocation.fromNamespaceAndPath("guzhenren", "huoxingu");

  /** 火人蛊物品ID。 */
  private static final ResourceLocation HUORENGU_ID =
      ResourceLocation.fromNamespaceAndPath("guzhenren", "huorengu");

  /** 火蛊物品ID。 */
  private static final ResourceLocation HUO_GU_ID =
      ResourceLocation.fromNamespaceAndPath("guzhenren", "huo_gu");

  /** 火油蛊物品ID。 */
  private static final ResourceLocation HUO_YOU_GU_ID =
      ResourceLocation.fromNamespaceAndPath("guzhenren", "huo_you_gu");

  /** 火龙蛊物品ID。 */
  private static final ResourceLocation HUOLONGGU_ID =
      ResourceLocation.fromNamespaceAndPath("guzhenren", "huolonggu");

  /** 弹跳火炭蛊物品ID。 */
  private static final ResourceLocation DAN_QIAO_HUO_TAN_GU_ID =
      ResourceLocation.fromNamespaceAndPath("guzhenren", "dan_qiao_huo_tan_gu");

  /** 焚身蛊物品ID。 */
  private static final ResourceLocation FEN_SHEN_GU_ID =
      ResourceLocation.fromNamespaceAndPath("guzhenren", "fen_shen_gu");

  private YanDaoDaohenOps() {
    // 注册火心蛊的道痕提供器
    registerProvider(cc -> {
      if (cc == null) {
        return 0.0;
      }
      int count = 0;
      Item item = BuiltInRegistries.ITEM.get(HUOXINGU_ID);
      for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
        ItemStack organ = cc.inventory.getItem(i);
        if (organ.getItem() == item) {
          count += organ.getCount();
        }
      }
      return calculateDaohen(count, HUOXINGU_DAOHEN_PER_STACK);
    });

    // 注册火人蛊的道痕提供器
    registerProvider(cc -> {
      if (cc == null) {
        return 0.0;
      }
      int count = 0;
      Item item = BuiltInRegistries.ITEM.get(HUORENGU_ID);
      for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
        ItemStack organ = cc.inventory.getItem(i);
        if (organ.getItem() == item) {
          count += organ.getCount();
        }
      }
      return calculateDaohen(count, HUORENGU_DAOHEN_PER_STACK);
    });

    // 注册火蛊的道痕提供器
    registerProvider(cc -> {
      if (cc == null) {
        return 0.0;
      }
      int count = 0;
      Item item = BuiltInRegistries.ITEM.get(HUO_GU_ID);
      for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
        ItemStack organ = cc.inventory.getItem(i);
        if (organ.getItem() == item) {
          count += organ.getCount();
        }
      }
      return calculateDaohen(count, HUO_GU_DAOHEN_PER_STACK);
    });

    // 注册火油蛊的道痕提供器
    registerProvider(cc -> {
      if (cc == null) {
        return 0.0;
      }
      int count = 0;
      Item item = BuiltInRegistries.ITEM.get(HUO_YOU_GU_ID);
      for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
        ItemStack organ = cc.inventory.getItem(i);
        if (organ.getItem() == item) {
          count += organ.getCount();
        }
      }
      return calculateDaohen(count, HUO_YOU_GU_DAOHEN_PER_STACK);
    });

    // 注册火龙蛊的道痕提供器
    registerProvider(cc -> {
      if (cc == null) {
        return 0.0;
      }
      int count = 0;
      Item item = BuiltInRegistries.ITEM.get(HUOLONGGU_ID);
      for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
        ItemStack organ = cc.inventory.getItem(i);
        if (organ.getItem() == item) {
          count += organ.getCount();
        }
      }
      return calculateDaohen(count, HUOLONGGU_DAOHEN_PER_STACK);
    });

    // 注册弹跳火炭蛊的道痕提供器
    registerProvider(cc -> {
      if (cc == null) {
        return 0.0;
      }
      int count = 0;
      Item item = BuiltInRegistries.ITEM.get(DAN_QIAO_HUO_TAN_GU_ID);
      for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
        ItemStack organ = cc.inventory.getItem(i);
        if (organ.getItem() == item) {
          count += organ.getCount();
        }
      }
      return calculateDaohen(count, DAN_QIAO_HUO_TAN_GU_DAOHEN_PER_STACK);
    });

    // 注册焚身蛊的道痕提供器
    registerProvider(cc -> {
      if (cc == null) {
        return 0.0;
      }
      int count = 0;
      Item item = BuiltInRegistries.ITEM.get(FEN_SHEN_GU_ID);
      for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
        ItemStack organ = cc.inventory.getItem(i);
        if (organ.getItem() == item) {
          count += organ.getCount();
        }
      }
      return calculateDaohen(count, FEN_SHEN_GU_DAOHEN_PER_STACK);
    });

    // 未来可以在这里继续注册其他炎道器官
  }

  /**
   * 计算炎道道痕总值。
   *
   * @param cc 胸腔实例
   * @return 道痕总值
   */
  public static double computeDaohen(ChestCavityInstance cc) {
    return INSTANCE.compute(cc);
  }
}
