package net.tigereye.chestcavity.compat.guzhenren.item.gu_dao.calculator;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.util.DaohenCalculator;

/**
 * 骨道道痕计算工具类。
 *
 * <p>汇总骨道相关器官的道痕值,用于技能效果增幅计算。
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * double daohen = GuDaoDaohenOps.computeDaohen(cc);
 * float finalDamage = baseDamage * (1.0f + (float) daohen * 0.1f);
 * }</pre>
 */
public final class GuDaoDaohenOps extends DaohenCalculator {

  private static final GuDaoDaohenOps INSTANCE = new GuDaoDaohenOps();

  // 骨道器官道痕值配置 - 待根据实际需求调整
  private static final double GU_ZHU_GU_DAOHEN_PER_STACK = 1.0;
  private static final double GANJINGU_DAOHEN_PER_STACK = 1.5;
  private static final double TIE_GU_GU_DAOHEN_PER_STACK = 1.2;
  private static final double JINGTIEGUGU_DAOHEN_PER_STACK = 2.0;
  private static final double GU_QIANG_GU_DAOHEN_PER_STACK = 1.0;
  private static final double LUO_XUAN_GU_QIANG_GU_DAOHEN_PER_STACK = 2.5;
  private static final double HUGUGU_DAOHEN_PER_STACK = 2.0;
  private static final double YU_GU_GU_DAOHEN_PER_STACK = 3.0;
  private static final double ROU_BAI_GU_DAOHEN_PER_STACK = 1.5;
  private static final double LE_GU_DUN_GU_DAOHEN_PER_STACK = 1.5;

  // 骨道器官物品ID
  private static final ResourceLocation GU_ZHU_GU_ID =
      ResourceLocation.fromNamespaceAndPath("guzhenren", "gu_zhu_gu");
  private static final ResourceLocation GANJINGU_ID =
      ResourceLocation.fromNamespaceAndPath("guzhenren", "ganjingu");
  private static final ResourceLocation TIE_GU_GU_ID =
      ResourceLocation.fromNamespaceAndPath("guzhenren", "tie_gu_gu");
  private static final ResourceLocation JINGTIEGUGU_ID =
      ResourceLocation.fromNamespaceAndPath("guzhenren", "jingtiegugu");
  private static final ResourceLocation GU_QIANG_GU_ID =
      ResourceLocation.fromNamespaceAndPath("guzhenren", "gu_qiang_gu");
  private static final ResourceLocation LUO_XUAN_GU_QIANG_GU_ID =
      ResourceLocation.fromNamespaceAndPath("guzhenren", "luo_xuan_gu_qiang_gu");
  private static final ResourceLocation HUGUGU_ID =
      ResourceLocation.fromNamespaceAndPath("guzhenren", "hugugu");
  private static final ResourceLocation YU_GU_GU_ID =
      ResourceLocation.fromNamespaceAndPath("guzhenren", "yu_gu_gu");
  private static final ResourceLocation ROU_BAI_GU_ID =
      ResourceLocation.fromNamespaceAndPath("guzhenren", "rou_bai_gu");
  private static final ResourceLocation LE_GU_DUN_GU_ID =
      ResourceLocation.fromNamespaceAndPath("guzhenren", "le_gu_dun_gu");

  private GuDaoDaohenOps() {
    // 注册骨竹蛊的道痕提供器
    registerProvider(cc -> calculateOrganDaohen(cc, GU_ZHU_GU_ID, GU_ZHU_GU_DAOHEN_PER_STACK));

    // 注册钢筋骨的道痕提供器
    registerProvider(cc -> calculateOrganDaohen(cc, GANJINGU_ID, GANJINGU_DAOHEN_PER_STACK));

    // 注册铁骨蛊的道痕提供器
    registerProvider(cc -> calculateOrganDaohen(cc, TIE_GU_GU_ID, TIE_GU_GU_DAOHEN_PER_STACK));

    // 注册精铁骨蛊的道痕提供器
    registerProvider(cc ->
        calculateOrganDaohen(cc, JINGTIEGUGU_ID, JINGTIEGUGU_DAOHEN_PER_STACK));

    // 注册骨枪蛊的道痕提供器
    registerProvider(cc ->
        calculateOrganDaohen(cc, GU_QIANG_GU_ID, GU_QIANG_GU_DAOHEN_PER_STACK));

    // 注册螺旋骨枪蛊的道痕提供器
    registerProvider(cc ->
        calculateOrganDaohen(cc, LUO_XUAN_GU_QIANG_GU_ID, LUO_XUAN_GU_QIANG_GU_DAOHEN_PER_STACK));

    // 注册虎骨蛊的道痕提供器
    registerProvider(cc -> calculateOrganDaohen(cc, HUGUGU_ID, HUGUGU_DAOHEN_PER_STACK));

    // 注册玉骨蛊的道痕提供器
    registerProvider(cc -> calculateOrganDaohen(cc, YU_GU_GU_ID, YU_GU_GU_DAOHEN_PER_STACK));

    // 注册肉白骨的道痕提供器
    registerProvider(cc ->
        calculateOrganDaohen(cc, ROU_BAI_GU_ID, ROU_BAI_GU_DAOHEN_PER_STACK));

    // 注册肋骨盾蛊的道痕提供器
    registerProvider(cc ->
        calculateOrganDaohen(cc, LE_GU_DUN_GU_ID, LE_GU_DUN_GU_DAOHEN_PER_STACK));
  }

  /**
   * 计算指定器官的道痕贡献值。
   *
   * @param cc 胸腔实例
   * @param organId 器官物品ID
   * @param daohenPerStack 每个器官提供的道痕值
   * @return 该器官的总道痕贡献
   */
  private static double calculateOrganDaohen(
      ChestCavityInstance cc,
      ResourceLocation organId,
      double daohenPerStack) {
    if (cc == null || cc.inventory == null) {
      return 0.0;
    }

    int count = 0;
    Item organItem = BuiltInRegistries.ITEM.get(organId);

    for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
      ItemStack organ = cc.inventory.getItem(i);
      if (organ.getItem() == organItem) {
        count += organ.getCount();
      }
    }

    return calculateDaohen(count, daohenPerStack);
  }

  /**
   * 计算骨道道痕总值。
   *
   * @param cc 胸腔实例
   * @return 道痕总值
   */
  public static double computeDaohen(ChestCavityInstance cc) {
    return INSTANCE.compute(cc);
  }
}
