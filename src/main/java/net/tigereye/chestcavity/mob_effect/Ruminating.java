package net.tigereye.chestcavity.mob_effect;

import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.registration.CCFoodComponents;

public class Ruminating extends CCStatusEffect {

  public Ruminating() {
    super(MobEffectCategory.BENEFICIAL, 0xC8FF00);
  }

  @Override
  public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
    return duration % ChestCavity.config.RUMINATION_TIME == 1;
  }

  @Override
  public boolean applyEffectTick(LivingEntity entity, int amplifier) {
    if (entity instanceof Player player) {
      if (!entity.level().isClientSide()) {
        FoodData hungerManager = player.getFoodData();
        hungerManager.eat(CCFoodComponents.CUD_FOOD_COMPONENT);
      }
    }
    return true;
  }
}
