package net.tigereye.chestcavity.compat.guzhenren.item.combo.jian_dao;

import net.minecraft.resources.ResourceLocation;

/**
 * 剑道组合杀招的 ID 与依赖声明。
 *
 * <p>集中管理本家族的 ResourceLocation 与依赖物，避免散落硬编码。
 */
public final class JianDaoComboRegistry {

  private JianDaoComboRegistry() {}

  // 技能 ID（第一个 Combo 的占位 ID）
  public static final ResourceLocation SKILL_ID =
      ResourceLocation.fromNamespaceAndPath("guzhenren", "jian_dao_first_combo");

  // 需求物（必须全部存在并装备）
  public static final ResourceLocation ZHI_LU_GU =
      ResourceLocation.fromNamespaceAndPath("guzhenren", "zhi_lu_gu");
  public static final ResourceLocation YU_JUN_GU =
      ResourceLocation.fromNamespaceAndPath("guzhenren", "yu_jun_gu");
  public static final ResourceLocation YI_ZHUAN_REN_DAO_XI_WANG_GU =
      ResourceLocation.fromNamespaceAndPath("guzhenren", "yizhuanrendaoxiwanggu");
  public static final ResourceLocation JIAN_QI_GU =
      ResourceLocation.fromNamespaceAndPath("guzhenren", "jianqigu");

  // 第二个：魔道分支（葬生飞剑）
  public static final ResourceLocation SKILL_ID_REN_SHOU =
      ResourceLocation.fromNamespaceAndPath("guzhenren", "jian_dao_ren_shou_combo");
  // 需求与可选蛊虫
  public static final ResourceLocation REN_SHOU_ZANG_SHENG_GU =
      ResourceLocation.fromNamespaceAndPath("guzhenren", "ren_shou_zang_sheng_gu");
  public static final ResourceLocation XIAO_HUN_GU =
      ResourceLocation.fromNamespaceAndPath("guzhenren", "xiao_hun_gu");
  public static final ResourceLocation JIAN_HEN_GU =
      ResourceLocation.fromNamespaceAndPath("guzhenren", "jianhengu");
  public static final ResourceLocation JIAN_JI_GU =
      ResourceLocation.fromNamespaceAndPath("guzhenren", "jianjigu");
  public static final ResourceLocation JIAN_WEN_GU =
      ResourceLocation.fromNamespaceAndPath("guzhenren", "jianwengu");
}
