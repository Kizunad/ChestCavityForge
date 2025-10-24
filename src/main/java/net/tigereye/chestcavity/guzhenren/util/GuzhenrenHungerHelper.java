package net.tigereye.chestcavity.guzhenren.util;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;

/** Utility helpers for manipulating player hunger without relying on real food items. */
public final class GuzhenrenHungerHelper {

  private static final int VANILLA_MAX_FOOD_LEVEL = 20;

  private GuzhenrenHungerHelper() {}

  /**
   * Returns whether the supplied player can benefit from additional food.
   *
   * @param player player to query
   * @return {@code true} if the player exists and is not at full hunger, {@code false} otherwise
   */
  public static boolean needsFood(Player player) {
    if (player == null) {
      return false;
    }
    FoodData foodData = player.getFoodData();
    return foodData != null && foodData.needsFood();
  }

  /**
   * Restores the player's hunger bar to full using vanilla food rules for saturation.
   *
   * @param player the player to feed
   * @return {@code true} if any hunger was restored, {@code false} otherwise
   */
  public static boolean topUpToFull(Player player) {
    return topUpTo(player, VANILLA_MAX_FOOD_LEVEL, 1.0f);
  }

  /**
   * Adds virtual nutrition up to the requested food level, applying saturation using the provided
   * modifier.
   *
   * @param player player receiving the virtual meal
   * @param targetFoodLevel target food level after the meal (clamped to vanilla limits)
   * @param saturationModifier saturation modifier equivalent to {@link
   *     net.minecraft.world.food.FoodProperties#getSaturationModifier()}
   * @return {@code true} if any hunger was restored, {@code false} otherwise
   */
  public static boolean topUpTo(Player player, int targetFoodLevel, float saturationModifier) {
    if (player == null) {
      return false;
    }
    FoodData foodData = player.getFoodData();
    if (foodData == null) {
      return false;
    }
    int clampedTarget = Math.max(0, Math.min(targetFoodLevel, VANILLA_MAX_FOOD_LEVEL));
    int current = foodData.getFoodLevel();
    int missing = clampedTarget - current;
    if (missing <= 0) {
      return false;
    }
    float clampedSaturation = Math.max(0.0f, saturationModifier);
    foodData.eat(missing, clampedSaturation);
    if (foodData.getFoodLevel() > clampedTarget) {
      foodData.setFoodLevel(clampedTarget);
    }
    float saturation = foodData.getSaturationLevel();
    if (saturation > foodData.getFoodLevel()) {
      foodData.setSaturation(foodData.getFoodLevel());
    }
    return true;
  }
}
