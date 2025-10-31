package net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.tuning;

import net.minecraft.resources.ResourceLocation;

public final class YuYueTuning {
  public static final String MOD_ID = "guzhenren";
  public static final ResourceLocation ABILITY_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "yu_yue");

  public static final String COOLDOWN_KEY = "YuYueReadyAt";
  public static final int COOLDOWN_TICKS = 20 * 7;
  public static final double ZHENYUAN_COST = 400.0;
  public static final double JINGLI_COST = 8.0;

  public static final double RANGE_WATER = 7.0;
  public static final double RANGE_MOIST = 4.0;
  public static final double TAIL_BONUS_WATER = 3.0;
  public static final double TAIL_BONUS_DRY = 1.5;
  public static final double UPGRADE_BONUS_RANGE = 1.0;

  public static final double HORIZONTAL_SCALE = 0.45;
  public static final double VERTICAL_IN_WATER = 0.25;
  public static final double VERTICAL_OUT_OF_WATER = 0.12;

  public static final int SLOW_FALL_TICKS = 20;
  public static final int UPGRADE_RESIST_TICKS = 20;

  private YuYueTuning() {}
}
