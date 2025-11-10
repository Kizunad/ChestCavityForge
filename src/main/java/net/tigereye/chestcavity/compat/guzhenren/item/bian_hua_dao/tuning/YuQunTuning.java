package net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.tuning;

import net.minecraft.resources.ResourceLocation;

public final class YuQunTuning {
  public static final String MOD_ID = "guzhenren";
  public static final ResourceLocation ABILITY_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "yu_qun");

  public static final String COOLDOWN_KEY = "YuQunReadyAt";
  public static final int COOLDOWN_TICKS = 20 * 12;
  public static final double ZHENYUAN_COST = 120.0;
  public static final double JINGLI_COST = 12.0;
  public static final int HUNGER_COST = 2;

  public static final double RANGE = 10.0;
  public static final double UPGRADE_RANGE_BONUS = 2.0;
  public static final double WIDTH = 1.75;

  public static final double PUSH_STRENGTH_BASE = 0.45;
  public static final double PUSH_STRENGTH_UPGRADE = 0.6;
  public static final double PUSH_UPWARD_BASE = 0.45;
  public static final double PUSH_UPWARD_UPGRADE_BONUS = 0.25;

  public static final int SLOW_TICKS = 60;
  public static final int SLOW_AMP_BASE = 0;
  public static final int SLOW_AMP_UPGRADE = 1;

  public static final ResourceLocation BLEED_EFFECT_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "lliuxue");
  public static final int BLEED_TICKS_BASE = 80;
  public static final int BLEED_TICKS_UPGRADE = 120;
  public static final int BLEED_AMP_BASE = 0;
  public static final int BLEED_AMP_UPGRADE = 1;

  private YuQunTuning() {}
}
