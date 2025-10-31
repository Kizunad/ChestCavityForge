package net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.yin_yang_zhuan_shen_gu.tuning;

import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.compat.guzhenren.item.common.cost.ResourceCost;

public final class YinYangZhuanShenGuTuning {
    public static final String MOD_ID = "guzhenren";
    public static final ResourceLocation ORGAN_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "yin_yang_zhuan_shen_gu");
    public static final ResourceLocation SKILL_BODY_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "yin_yang_zhuan_shen_gu/body");
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

    public static final ResourceCost COST_BODY =
        new ResourceCost(200.0, 10.0, 5.0, 5.0, 5, 2.0f);

  private YinYangZhuanShenGuTuning() {}
}
