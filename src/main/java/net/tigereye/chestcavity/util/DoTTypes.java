package net.tigereye.chestcavity.util;

import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.ChestCavity;

/** 统一的 DoT 类型标识符集合，便于分类与诊断。 命名规范："dot/<domain>_<name>" 或 "dot/<group>/<name>"。 */
public final class DoTTypes {
  private DoTTypes() {}

  // 通用占位（未显式声明来源）
  public static final ResourceLocation GENERIC = ChestCavity.id("dot/generic");
  public static final ResourceLocation ATTACK_BASE_PERCENT =
      ChestCavity.id("dot/attack_base_percent");

  // Guzhenren 系列（示例：魂道/冰雪道/云道）
  public static final ResourceLocation HUN_DAO_SOUL_FLAME =
      ChestCavity.id("dot/hun_dao_soul_flame");
  public static final ResourceLocation SHUANG_XI_FROSTBITE =
      ChestCavity.id("dot/shuang_xi_frostbite");
  public static final ResourceLocation YIN_YUN_CORROSION = ChestCavity.id("dot/yin_yun_corrosion");
  public static final ResourceLocation YAN_DAO_HUO_YI_AURA =
      ChestCavity.id("dot/yan_dao_huo_yi_aura");
  public static final ResourceLocation YAN_DAO_DRAGONFLAME =
      ChestCavity.id("dot/yan_dao_dragonflame");
  public static final ResourceLocation LEI_DUN_ELECTRIFY = ChestCavity.id("dot/lei_dun_electrify");
  public static final ResourceLocation XIE_WANG_BLEED = ChestCavity.id("dot/xie_wang_bleed");
  // 通用点燃 DoT（由火系命中/余烬/点燃窗口刷新）
  public static final ResourceLocation IGNITE = ChestCavity.id("dot/ignite");

  // 智道（心智系）
  public static final ResourceLocation ZHI_DAO_PSYCHIC_BURN =
      ChestCavity.id("dot/zhi_dao_psychic_burn");
  public static final ResourceLocation ZHI_DAO_INSIGHT_AURA =
      ChestCavity.id("dot/zhi_dao_insight_aura");
}
