package net.tigereye.chestcavity.compat.guzhenren.item.combo.jian_dao.tuning;

import net.minecraft.resources.ResourceLocation;

/**
 * 剑道组合杀招（第一个）数值与开关占位。
 *
 * <p>仅提供框架参数，后续由策划/平衡补充具体数值。
 */
public final class JianDaoComboTuning {

  private JianDaoComboTuning() {}

  // 展示图标
  public static final ResourceLocation ICON =
      ResourceLocation.fromNamespaceAndPath(
          "guzhenren", "textures/skill/flying_sword_summon_zheng_dao.png");

  // Combo 描述提示词（注册用，文档也可复用）
  public static final String DESCRIPTION =
      "消耗四器官与道痕/上限资源，召唤承继主手属性的正道飞剑，并绘制三环剑阵。";

  // 基础开关
  public static final boolean FX_ENABLED = true;
  public static final boolean MESSAGES_ENABLED = true;

  // 冷却/窗口（单位：tick）
  public static final long BASE_COOLDOWN_TICKS = 20L * 15; // 15s，占位
  public static final long COMBO_WINDOW_TICKS = 20L * 3; // 3s 窗口，占位

  // 计数/倍率（仅用于 Calculator 占位校验）
  public static final int MAX_HITS = 3;
  public static final double BASE_MULTIPLIER = 1.0D;
  public static final double GROWTH_PER_HIT = 0.2D; // 每次命中增加 20%

  // 资源（占位：仅记录 Tier，不在行为里实际扣费，等待业务确认）
  public static final int DESIGN_ZHUANSHU = 3; // 3 转
  public static final int DESIGN_JIEDUAN = 1; // 1 阶段

  // 资源消耗（按用户指定，使用 ResourceOps 严格扣除，不足回滚）
  public static final double COST_ZHENYUAN_BASE = 100.0D; // 以 baseCost 计
  // 若采用“上限扣减”语义，使用 KEY_MAX_ZHENYUAN 并直接调整上限字段
  public static final String KEY_MAX_ZHENYUAN = "zuida_zhenyuan";
  public static final double COST_JINGLI = 30.0D;
  public static final String KEY_MAX_JINGLI = "zuida_jingli";
  public static final double COST_HUNPO = 30.0D;
  public static final String KEY_MAX_HUNPO = "zuida_hunpo";
  public static final float COST_HEALTH = 30.0F;
  // MAX_HEALTH 通过属性修饰符实现上限扣减
  public static final net.minecraft.resources.ResourceLocation MAX_HEALTH_MODIFIER_ID =
      net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
          "guzhenren", "modifier/jian_dao/combo_health_cap_cost");
  public static final double COST_JIANDAO_DAOHEN = 100.0D; // 键：daohen_jiandao
  public static final String KEY_DAOHEN_JIANDAO = "daohen_jiandao";

  // 成功后效果：虚弱III（5分钟）
  public static final long EFFECT_WEAKNESS_TICKS = 20L * 60L * 5L;
  public static final int EFFECT_WEAKNESS_AMPLIFIER = 2; // 0->I, 1->II, 2->III

  // 物品→飞剑 继承映射系数（仅用于本 Combo）
  public static final double AFFINITY_ATTACK_DAMAGE_COEF = 0.5; // 攻击伤害 → 伤害
  public static final double AFFINITY_ATTACK_SPEED_ABS_COEF = 0.05; // |攻速| → 最大速度
  public static final double AFFINITY_SHARPNESS_DMG_PER_LVL = 0.5; // 锋利等级 → 伤害
  public static final double AFFINITY_SHARPNESS_VEL_PER_LVL = 0.03; // 锋利等级 → 速度²系数
  public static final double AFFINITY_UNBREAKING_LOSS_MULT_PER_LVL = 0.9; // 每级*0.9
  public static final double AFFINITY_SWEEPING_BASE = 0.30; // 横扫基准
  public static final double AFFINITY_SWEEPING_PER_LVL = 0.15; // 横扫每级
  public static final double AFFINITY_EFFICIENCY_BLOCK_EFF_PER_LVL = 0.5; // 效率 → 破块效率
  public static final double AFFINITY_MINING_SPEED_TO_BLOCK_EFF = 0.05; // Tier速度 → 破块效率
  public static final double AFFINITY_MAX_DAMAGE_TO_MAX_DURABILITY = 0.10; // 物品耐久 → 飞剑耐久
  public static final double AFFINITY_ARMOR_TO_MAX_DURABILITY = 8.0; // 护甲 → 飞剑耐久
  public static final double AFFINITY_ARMOR_DURA_LOSS_MULT_PER_POINT = 0.97; // 每点护甲耐久损耗倍率

  // Combo飞剑独立最大耐久度（完全覆盖默认值，不走继承增量）
  public static final double COMBO_SWORD_MAX_DURABILITY = 200.0; // 基准值，可调
}
