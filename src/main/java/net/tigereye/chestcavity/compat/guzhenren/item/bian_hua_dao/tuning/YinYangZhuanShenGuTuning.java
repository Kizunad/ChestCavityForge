package net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.tuning;

import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.compat.guzhenren.item.common.cost.ResourceCost;

public final class YinYangZhuanShenGuTuning {
  public static final String MOD_ID = "guzhenren";
  public static final ResourceLocation ORGAN_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "yin_yang_zhuan_shen_gu");
  public static final ResourceLocation SKILL_BODY_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "yin_yang_zhuan_shen_gu/body");
  public static final long BODY_COOLDOWN_TICKS = 120L * 20L;
  public static final long FALL_GUARD_TICKS = 60L; // 3 秒
  public static final float FALL_REDUCTION = 0.7f;

  public static final ResourceLocation MAX_HEALTH_YANG_MODIFIER =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "modifier/yin_yang/max_health_yang");
  public static final ResourceLocation MAX_HEALTH_YIN_MODIFIER =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "modifier/yin_yang/max_health_yin");
  public static final ResourceLocation ATTACK_YANG_MODIFIER =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "modifier/yin_yang/attack_yang");
  public static final ResourceLocation ATTACK_YIN_MODIFIER =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "modifier/yin_yang/attack_yin");
  public static final ResourceLocation ARMOR_YANG_MODIFIER =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "modifier/yin_yang/armor_yang");
  public static final ResourceLocation ARMOR_YIN_MODIFIER =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "modifier/yin_yang/armor_yin");
  public static final ResourceLocation MOVE_YANG_MODIFIER =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "modifier/yin_yang/move_yang");
  public static final ResourceLocation MOVE_YIN_MODIFIER =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "modifier/yin_yang/move_yin");
  public static final ResourceLocation KNOCKBACK_YANG_MODIFIER =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "modifier/yin_yang/knockback_yang");

  public static final ResourceCost COST_BODY = new ResourceCost(200.0, 10.0, 5.0, 5.0, 5, 2.0f);

  // 被动数值：每 tick/周期的资源变化与效果（运行时按需使用）
  public static final int PASSIVE_FX_INTERVAL_TICKS = 40; // 每 2 秒播粒子

  public static final double YANG_JINGLI_PER_TICK = 10.0D;
  public static final float YANG_HEAL_PER_TICK = 20.0F;
  public static final int YANG_HUNGER_COST = 5;
  public static final double YANG_HUNPO_DELTA = -1.0D;
  public static final double YANG_NIANTOU_DELTA = -1.0D;

  public static final double YIN_HUNPO_PER_TICK = 20.0D;
  public static final double YIN_NIANTOU_PER_TICK = 2.0D;
  public static final double YIN_ZHENYUAN_PER_TICK = 10.0D;
  public static final double YIN_JINGLI_DELTA = -1.0D;
  public static final int YIN_HUNGER_COST = 5;

  private YinYangZhuanShenGuTuning() {}
}
