package net.tigereye.chestcavity.mob_effect;

import java.util.Optional;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.interfaces.ChestCavityEntity;
import net.tigereye.chestcavity.registration.CCFoodComponents;

public class FurnacePower extends CCStatusEffect {

  public FurnacePower() {
    super(MobEffectCategory.BENEFICIAL, 0xC8FF00);
  }

  @Override
  public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
    return true;
  }

  @Override
  public boolean applyEffectTick(LivingEntity entity, int amplifier) {
    if (entity instanceof Player player) {
      if (!entity.level().isClientSide()) {
        Optional<ChestCavityEntity> optional = ChestCavityEntity.of(entity);
        if (optional.isPresent()) {
          ChestCavityEntity cce = optional.get();
          ChestCavityInstance cc = cce.getChestCavityInstance();
          cc.furnaceProgress++;
          if (cc.furnaceProgress >= 200) {
            cc.furnaceProgress = 0;
            FoodData hungerManager = player.getFoodData();
            for (int i = 0; i <= amplifier; i++) {
              hungerManager.eat(CCFoodComponents.FURNACE_POWER_FOOD_COMPONENT);
            }
          }
        }
      }
    }
    return true;
  }
}
