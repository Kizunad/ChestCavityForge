package net.tigereye.chestcavity.compat.guzhenren.util;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;

/** Utility to gently maintain hunger and saturation without overriding food-based effects. */
public final class SaturationHelper {

  private SaturationHelper() {}

  public static void gentlyTopOff(Player player, int targetFood, float saturationDelta) {
    if (player == null) {
      return;
    }
    FoodData data = player.getFoodData();
    if (data.getFoodLevel() < targetFood) {
      data.eat(1, saturationDelta);
    } else if (data.getSaturationLevel() < data.getFoodLevel()) {
      data.eat(0, saturationDelta);
    }
  }
}
