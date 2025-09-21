package net.tigereye.chestcavity.compat.guzhenren.item;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.listeners.OrganOnGroundListener;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;

/**
 * 金肺蛊：在玩家饱食度充足时，将饱食转化为短期的吸收之心。
 */
public enum JinfeiguOrganBehavior implements OrganOnGroundListener, OrganSlowTickListener {
    INSTANCE;

    private static final int FULL_FOOD_LEVEL = 20;
    private static final int HUNGER_COST = 4;
    private static final double ABSORPTION_HEARTS = 4.0;
    private static final int EFFECT_DURATION_TICKS = 20 * 60;

    @Override
    public void onGroundTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        // Work handled in onSlowTick to avoid per-tick overhead.
    }

    @Override
    public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (!(entity instanceof Player player) || entity.level().isClientSide()) {
            return;
        }

        FoodData foodData = player.getFoodData();
        if (foodData.getFoodLevel() < FULL_FOOD_LEVEL) {
            return;
        }

        int stackCount = Math.max(1, organ.getCount());
        float requiredAbsorption = (float) (ABSORPTION_HEARTS * stackCount);
        if (player.getAbsorptionAmount() >= requiredAbsorption - 0.01f) {
            return;
        }

        if (foodData.getFoodLevel() < HUNGER_COST) {
            return;
        }

        MobEffectInstance absorption = buildAbsorptionEffect(stackCount);
        int previousFood = foodData.getFoodLevel();
        float previousSaturation = foodData.getSaturationLevel();

        foodData.setFoodLevel(Math.max(0, previousFood - HUNGER_COST));
        foodData.setSaturation(Math.min(previousSaturation, foodData.getFoodLevel()));

        if (!player.addEffect(absorption)) {
            foodData.setFoodLevel(previousFood);
            foodData.setSaturation(previousSaturation);
            return;
        }

        float targetAbsorption = Math.max(requiredAbsorption, heartsFromAmplifier(absorption.getAmplifier()));
        player.setAbsorptionAmount(Math.max(player.getAbsorptionAmount(), targetAbsorption));
    }

    private static MobEffectInstance buildAbsorptionEffect(int stackCount) {
        float hearts = (float) (ABSORPTION_HEARTS * Math.max(1, stackCount));
        int amplifier = Math.max(0, (int) Math.ceil(hearts / 2.0f) - 1);
        while (amplifier < 255 && heartsFromAmplifier(amplifier) < hearts) {
            amplifier++;
        }
        return new MobEffectInstance(MobEffects.ABSORPTION, EFFECT_DURATION_TICKS, amplifier, false, false, true);
    }

    private static float heartsFromAmplifier(int amplifier) {
        return (amplifier + 1) * 2.0f;
    }
}
