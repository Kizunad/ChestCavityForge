package net.tigereye.chestcavity.compat.guzhenren.flyingsword.tuning;

/**
 * 飞剑释放继承参数（由物品转化到实体）
 */
public final class FlyingSwordInheritTuning {
  private FlyingSwordInheritTuning() {}

  public static final double INHERIT_DMG_MIN = -2.0;
  public static final double INHERIT_DMG_MAX = 10.0;
  public static final double INHERIT_SPEED_MIN = -0.1;
  public static final double INHERIT_SPEED_MAX = 0.3;
  public static final double INHERIT_SHARPNESS_DMG = 0.5;
  public static final double INHERIT_SHARPNESS_VEL = 0.03;
  public static final double INHERIT_ATTACK_DAMAGE_COEF = 0.5;
  public static final double INHERIT_ATTACK_SPEED_COEF = 0.05;

  // 由物品“耐久/护甲”映射到飞剑属性的默认系数
  // 物品最大耐久 → 飞剑最大耐久增量（如：钻石剑 1561 * 0.1 ≈ +156.1）
  public static final double INHERIT_MAX_DAMAGE_TO_MAX_DURABILITY_COEF = 0.10;
  // 护甲值 → 飞剑最大耐久增量（每点护甲转化为固定耐久）
  public static final double INHERIT_ARMOR_TO_MAX_DURABILITY_COEF = 8.0;
  // 护甲对耐久损耗倍率的影响（每点护甲乘以一个衰减系数，<1 表示降低损耗）
  public static final double INHERIT_ARMOR_DURA_LOSS_MULT_PER_POINT = 0.97;
}
