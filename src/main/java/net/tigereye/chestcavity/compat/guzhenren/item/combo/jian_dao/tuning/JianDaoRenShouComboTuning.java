package net.tigereye.chestcavity.compat.guzhenren.item.combo.jian_dao.tuning;

import net.minecraft.resources.ResourceLocation;

/** 魔道·剑道 组合杀招（葬生飞剑）调参占位。 */
public final class JianDaoRenShouComboTuning {
  private JianDaoRenShouComboTuning() {}

  // 图标（占位：临时复用正道图标，后续可替换）
  public static final ResourceLocation ICON =
      ResourceLocation.fromNamespaceAndPath(
          "guzhenren", "textures/skill/flying_sword_summon_ren_shou_zang_sheng.png");

  // 描述
  public static final String DESCRIPTION =
      "以近村为供，每5刻扣5点生命并回响心跳，将总扣血注入飞剑耐久，召唤葬生飞剑。";

  // 祭献相关
  public static final double SACRIFICE_RADIUS = 8.0; // 搜索半径
  public static final int SACRIFICE_PULSE_TICKS = 5; // 每5tick一跳
  public static final float SACRIFICE_PULSE_DAMAGE = 5.0f; // 每跳5生命
  public static final int SACRIFICE_MAX_PULSES = 60; // 最长300tick（15s）保护
  public static final int SACRIFICE_MAX_VICTIMS = 24; // 一次最多处理的村民数

  // 心跳音效
  public static final float HEARTBEAT_VOLUME = 0.8f;
  public static final float HEARTBEAT_PITCH = 1.0f;

  // 召唤参数（独立配置）
  public static final long SUMMON_COOLDOWN_TICKS = 20L * 15; // 冷却（占位，可改）
  public static final double DURABILITY_INJECT_MULT = 1.0; // 注入耐久倍率（扣血×倍率）

  // 物品→飞剑 继承映射系数（与正道独立配置）
  public static final double AFFINITY_ATTACK_DAMAGE_COEF = 0.5;
  public static final double AFFINITY_ATTACK_SPEED_ABS_COEF = 0.05;
  public static final double AFFINITY_SHARPNESS_DMG_PER_LVL = 0.5;
  public static final double AFFINITY_SHARPNESS_VEL_PER_LVL = 0.03;
  public static final double AFFINITY_UNBREAKING_LOSS_MULT_PER_LVL = 0.9;
  public static final double AFFINITY_SWEEPING_BASE = 0.30;
  public static final double AFFINITY_SWEEPING_PER_LVL = 0.15;
  public static final double AFFINITY_EFFICIENCY_BLOCK_EFF_PER_LVL = 0.5;
  public static final double AFFINITY_MINING_SPEED_TO_BLOCK_EFF = 0.05;
  public static final double AFFINITY_MAX_DAMAGE_TO_MAX_DURABILITY = 0.10;
  public static final double AFFINITY_ARMOR_TO_MAX_DURABILITY = 8.0;
  public static final double AFFINITY_ARMOR_DURA_LOSS_MULT_PER_POINT = 0.97;

  // 资源消耗（独立配置；默认为占位值，可按需调整）
  public static final double COST_ZHENYUAN_BASE = 100.0D; // 真元上限扣减
  public static final String KEY_MAX_ZHENYUAN = "zuida_zhenyuan";
  public static final double COST_JINGLI = 30.0D; // 精力上限扣减
  public static final String KEY_MAX_JINGLI = "zuida_jingli";
  public static final double COST_HUNPO = 30.0D; // 魂魄上限扣减
  public static final String KEY_MAX_HUNPO = "zuida_hunpo";
  public static final float COST_HEALTH = 30.0F; // 最大生命扣减
  public static final net.minecraft.resources.ResourceLocation MAX_HEALTH_MODIFIER_ID =
      net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
          "guzhenren", "modifier/jian_dao/ren_shou/combo_health_cap_cost");
  public static final double COST_DAOHEN = 200.0D; // 道痕消耗
  public static final String KEY_DAOHEN = "daohen_jiandao"; // 默认使用“剑道道痕”
}
