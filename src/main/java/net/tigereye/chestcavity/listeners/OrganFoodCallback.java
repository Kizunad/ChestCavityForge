package net.tigereye.chestcavity.listeners;


import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.interfaces.ChestCavityEntity;

public class OrganFoodCallback {
    public static EffectiveFoodScores addFoodListener(Item food, FoodProperties foodComponent, ChestCavityEntity cce, EffectiveFoodScores efs) {
        return OrganFoodListeners.callMethods(food, foodComponent, cce, efs);
    }
}
