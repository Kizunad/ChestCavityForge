package net.tigereye.chestcavity.registration;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.tigereye.chestcavity.ChestCavity;

public class CCFoodComponents {
    public static final FoodProperties ANIMAL_MUSCLE_FOOD_COMPONENT = new FoodProperties.Builder().nutrition(1).saturationMod(.4f).meat().fast().build();
    public static final FoodProperties SMALL_ANIMAL_MUSCLE_FOOD_COMPONENT = new FoodProperties.Builder().nutrition(1).saturationMod(.2f).meat().fast().build();
    public static final FoodProperties BURNT_MEAT_CHUNK_COMPONENT = new FoodProperties.Builder().nutrition(1).saturationMod(.8f).meat().fast().build();
    public static final FoodProperties RAW_BUTCHERED_MEAT_FOOD_COMPONENT = new FoodProperties.Builder().nutrition(2).saturationMod(.4f).meat().build();
    public static final FoodProperties COOKED_BUTCHERED_MEAT_FOOD_COMPONENT = new FoodProperties.Builder().nutrition(3).saturationMod(.8f).meat().build();
    public static final FoodProperties RAW_ORGAN_MEAT_FOOD_COMPONENT = new FoodProperties.Builder().nutrition(2).saturationMod(0.6f).meat().fast().build();
    public static final FoodProperties COOKED_ORGAN_MEAT_FOOD_COMPONENT = new FoodProperties.Builder().nutrition(3).saturationMod(1.2f).meat().fast().build();
    public static final FoodProperties RAW_SAUSAGE_FOOD_COMPONENT = new FoodProperties.Builder().nutrition(4).saturationMod(.4f).meat().build();
    public static final FoodProperties COOKED_SAUSAGE_FOOD_COMPONENT = new FoodProperties.Builder().nutrition(8).saturationMod(.8f).meat().build();
    public static final FoodProperties RAW_RICH_SAUSAGE_FOOD_COMPONENT = new FoodProperties.Builder().nutrition(4).saturationMod(.6f).meat().build();
    public static final FoodProperties COOKED_RICH_SAUSAGE_FOOD_COMPONENT = new FoodProperties.Builder().nutrition(8).saturationMod(1.2f).meat().build();
    public static final FoodProperties RAW_MINI_SAUSAGE_FOOD_COMPONENT = new FoodProperties.Builder().nutrition(3).saturationMod(.4f).meat().build();
    public static final FoodProperties COOKED_MINI_SAUSAGE_FOOD_COMPONENT = new FoodProperties.Builder().nutrition(6).saturationMod(.8f).meat().build();
    public static final FoodProperties RAW_RICH_MINI_SAUSAGE_FOOD_COMPONENT = new FoodProperties.Builder().nutrition(3).saturationMod(.6f).meat().build();
    public static final FoodProperties COOKED_RICH_MINI_SAUSAGE_FOOD_COMPONENT = new FoodProperties.Builder().nutrition(6).saturationMod(1.2f).meat().build();

    public static final FoodProperties ROTTEN_MUSCLE_FOOD_COMPONENT = new FoodProperties.Builder().nutrition(1).saturationMod(0.1F).effect(new MobEffectInstance(MobEffects.HUNGER, 600, 0), 0.8F).meat().build();
    public static final FoodProperties ROTTEN_SAUSAGE_FOOD_COMPONENT = new FoodProperties.Builder().nutrition(8).saturationMod(0.1F).effect(new MobEffectInstance(MobEffects.HUNGER, 600, 0), 0.8F).meat().build();

    public static final FoodProperties INSECT_MUSCLE_FOOD_COMPONENT = new FoodProperties.Builder().nutrition(1).saturationMod(.4f).meat().fast()
            .effect(new MobEffectInstance(MobEffects.POISON, 80), 1f).build();
    public static final FoodProperties RAW_TOXIC_MEAT_FOOD_COMPONENT = new FoodProperties.Builder().nutrition(2).saturationMod(.4f).meat()
            .effect(new MobEffectInstance(MobEffects.POISON, 80), 1f).build();
    public static final FoodProperties COOKED_TOXIC_MEAT_FOOD_COMPONENT = new FoodProperties.Builder().nutrition(3).saturationMod(.8f).meat()
            .effect(new MobEffectInstance(MobEffects.CONFUSION, 160, 1), 1f).build();
    public static final FoodProperties RAW_TOXIC_ORGAN_MEAT_FOOD_COMPONENT = new FoodProperties.Builder().nutrition(2).saturationMod(0.6f).meat().fast()
            .effect(new MobEffectInstance(MobEffects.POISON, 80), 1f).build();
    public static final FoodProperties COOKED_TOXIC_ORGAN_MEAT_FOOD_COMPONENT = new FoodProperties.Builder().nutrition(3).saturationMod(1.2f).meat().fast()
            .effect(new MobEffectInstance(MobEffects.CONFUSION, 160, 1), 1f).build();
    public static final FoodProperties RAW_TOXIC_SAUSAGE_FOOD_COMPONENT = new FoodProperties.Builder().nutrition(4).saturationMod(.4f).meat()
            .effect(new MobEffectInstance(MobEffects.POISON, 80), 1f).build();
    public static final FoodProperties COOKED_TOXIC_SAUSAGE_FOOD_COMPONENT = new FoodProperties.Builder().nutrition(8).saturationMod(.8f).meat()
            .effect(new MobEffectInstance(MobEffects.CONFUSION, 160, 1), 1f).build();
    public static final FoodProperties RAW_RICH_TOXIC_SAUSAGE_FOOD_COMPONENT = new FoodProperties.Builder().nutrition(4).saturationMod(.6f).meat()
            .effect(new MobEffectInstance(MobEffects.POISON, 80), 1f).build();
    public static final FoodProperties COOKED_RICH_TOXIC_SAUSAGE_FOOD_COMPONENT = new FoodProperties.Builder().nutrition(8).saturationMod(1.2f).meat()
            .effect(new MobEffectInstance(MobEffects.CONFUSION, 160, 1), 1f).build();

    public static final FoodProperties ALIEN_MUSCLE_FOOD_COMPONENT = new FoodProperties.Builder().nutrition(1).saturationMod(.4f).meat().fast()
            .effect(new MobEffectInstance(MobEffects.LEVITATION, 20), 1f).build();
    public static final FoodProperties RAW_ALIEN_MEAT_FOOD_COMPONENT = new FoodProperties.Builder().nutrition(2).saturationMod(.4f).meat()
            .effect(new MobEffectInstance(MobEffects.LEVITATION, 80), 1f).build();
    public static final FoodProperties COOKED_ALIEN_MEAT_FOOD_COMPONENT = new FoodProperties.Builder().nutrition(3).saturationMod(.8f).meat()
            .effect(new MobEffectInstance(MobEffects.SLOW_FALLING, 10, 1), 1f).build();
    public static final FoodProperties RAW_ALIEN_ORGAN_MEAT_FOOD_COMPONENT = new FoodProperties.Builder().nutrition(2).saturationMod(0.6f).meat().fast()
            .effect(new MobEffectInstance(MobEffects.LEVITATION, 40), 1f).build();
    public static final FoodProperties COOKED_ALIEN_ORGAN_MEAT_FOOD_COMPONENT = new FoodProperties.Builder().nutrition(3).saturationMod(1.2f).meat().fast()
            .effect(new MobEffectInstance(MobEffects.SLOW_FALLING, 15, 1), 1f).build();
    public static final FoodProperties RAW_ALIEN_SAUSAGE_FOOD_COMPONENT = new FoodProperties.Builder().nutrition(4).saturationMod(.4f).meat()
            .effect(new MobEffectInstance(MobEffects.LEVITATION, 80), 1f).build();
    public static final FoodProperties COOKED_ALIEN_SAUSAGE_FOOD_COMPONENT = new FoodProperties.Builder().nutrition(8).saturationMod(.8f).meat()
            .effect(new MobEffectInstance(MobEffects.SLOW_FALLING, 20, 1), 1f).build();
    public static final FoodProperties RAW_RICH_ALIEN_SAUSAGE_FOOD_COMPONENT = new FoodProperties.Builder().nutrition(4).saturationMod(.6f).meat()
            .effect(new MobEffectInstance(MobEffects.LEVITATION, 320), 1f).build();
    public static final FoodProperties COOKED_RICH_ALIEN_SAUSAGE_FOOD_COMPONENT = new FoodProperties.Builder().nutrition(8).saturationMod(1.2f).meat()
            .effect(new MobEffectInstance(MobEffects.SLOW_FALLING, 40, 1), 1f).build();

    public static final FoodProperties DRAGON_MUSCLE_FOOD_COMPONENT = new FoodProperties.Builder().nutrition(1).saturationMod(.4f).meat().fast()
            .effect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 300), 1f)
            .effect(new MobEffectInstance(MobEffects.DIG_SPEED, 300), 1f)
            .alwaysEat().build();
    public static final FoodProperties RAW_DRAGON_MEAT_FOOD_COMPONENT = new FoodProperties.Builder().nutrition(2).saturationMod(.4f).meat()
            .effect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 900), 1f)
            .effect(new MobEffectInstance(MobEffects.DIG_SPEED, 900), 1f)
            .alwaysEat().build();
    public static final FoodProperties COOKED_DRAGON_MEAT_FOOD_COMPONENT = new FoodProperties.Builder().nutrition(3).saturationMod(.8f).meat()
            .effect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 150, 1), 1f)
            .effect(new MobEffectInstance(MobEffects.DIG_SPEED, 150, 1), 1f)
            .alwaysEat().build();
    public static final FoodProperties RAW_DRAGON_ORGAN_MEAT_FOOD_COMPONENT = new FoodProperties.Builder().nutrition(2).saturationMod(0.6f).meat().fast()
            .effect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 90*20), 1f)
            .effect(new MobEffectInstance(MobEffects.DIG_SPEED, 90*20), 1f)
            .alwaysEat().build();
    public static final FoodProperties COOKED_DRAGON_ORGAN_MEAT_FOOD_COMPONENT = new FoodProperties.Builder().nutrition(3).saturationMod(1.2f).meat().fast()
            .effect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 15*20, 1), 1f)
            .effect(new MobEffectInstance(MobEffects.DIG_SPEED, 15*20, 1), 1f)
            .alwaysEat().build();
    public static final FoodProperties RAW_DRAGON_SAUSAGE_FOOD_COMPONENT = new FoodProperties.Builder().nutrition(4).saturationMod(.4f).meat()
            .effect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 480*20), 1f)
            .effect(new MobEffectInstance(MobEffects.DIG_SPEED, 480*20), 1f)
            .alwaysEat().build();
    public static final FoodProperties COOKED_DRAGON_SAUSAGE_FOOD_COMPONENT = new FoodProperties.Builder().nutrition(8).saturationMod(.8f).meat()
            .effect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 90*20, 1), 1f)
            .effect(new MobEffectInstance(MobEffects.DIG_SPEED, 90*20, 1), 1f)
            .alwaysEat().build();
    public static final FoodProperties RAW_RICH_DRAGON_SAUSAGE_FOOD_COMPONENT = new FoodProperties.Builder().nutrition(4).saturationMod(.6f).meat()
            .effect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 960*20), 1f)
            .effect(new MobEffectInstance(MobEffects.DIG_SPEED, 960*20), 1f)
            .alwaysEat().build();
    public static final FoodProperties COOKED_RICH_DRAGON_SAUSAGE_FOOD_COMPONENT = new FoodProperties.Builder().nutrition(8).saturationMod(1.2f).meat()
            .effect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 180*20, 1), 1f)
            .effect(new MobEffectInstance(MobEffects.DIG_SPEED, 180*20, 1), 1f)
            .alwaysEat().build();
    public static final FoodProperties DRAGON_HEART_FOOD_COMPONENT = new FoodProperties.Builder().nutrition(1).saturationMod(.4f).meat().fast()
            .effect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 30*20, 3), 1f)
            .effect(new MobEffectInstance(MobEffects.DIG_SPEED, 30*20, 3), 1f)
            .effect(new MobEffectInstance(MobEffects.POISON, 2, 3), 1f)
            .effect(new MobEffectInstance(MobEffects.WITHER, 2, 3), 1f)
            .effect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 2, 3), 1f)
            .effect(new MobEffectInstance(MobEffects.HUNGER, 2, 3), 1f)
            .effect(new MobEffectInstance(MobEffects.CONFUSION, 2, 3), 1f)
            .effect(new MobEffectInstance(MobEffects.BLINDNESS, 2, 3), 1f)
            .effect(new MobEffectInstance(MobEffects.SLOW_FALLING, 2, 3), 1f)
            .effect(new MobEffectInstance(MobEffects.LEVITATION, 2, 3), 1f)
            .alwaysEat().build();
    
    public static final FoodProperties HUMAN_MUSCLE_FOOD_COMPONENT = new FoodProperties.Builder().nutrition(2).saturationMod(.4f).meat().fast()
            .effect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 24000, 1), ChestCavity.config.RISK_OF_PRIONS).effect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 24000, 1), ChestCavity.config.RISK_OF_PRIONS).effect(new MobEffectInstance(MobEffects.WEAKNESS, 24000, 1), ChestCavity.config.RISK_OF_PRIONS).build();
    public static final FoodProperties RAW_MAN_MEAT_FOOD_COMPONENT = new FoodProperties.Builder().nutrition(3).saturationMod(.4f).meat()
            .effect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 24000, 1), ChestCavity.config.RISK_OF_PRIONS).effect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 24000, 1), ChestCavity.config.RISK_OF_PRIONS).effect(new MobEffectInstance(MobEffects.WEAKNESS, 24000, 1), ChestCavity.config.RISK_OF_PRIONS).build();
    public static final FoodProperties COOKED_MAN_MEAT_FOOD_COMPONENT = new FoodProperties.Builder().nutrition(4).saturationMod(.8f).meat()
            .effect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 24000, 0), ChestCavity.config.RISK_OF_PRIONS).effect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 24000, 0), ChestCavity.config.RISK_OF_PRIONS).effect(new MobEffectInstance(MobEffects.WEAKNESS, 24000, 0), ChestCavity.config.RISK_OF_PRIONS).build();
    public static final FoodProperties RAW_HUMAN_ORGAN_MEAT_FOOD_COMPONENT = new FoodProperties.Builder().nutrition(3).saturationMod(0.6f).meat().fast()
            .effect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 24000, 1), ChestCavity.config.RISK_OF_PRIONS).effect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 24000, 1), ChestCavity.config.RISK_OF_PRIONS).effect(new MobEffectInstance(MobEffects.WEAKNESS, 24000, 1), ChestCavity.config.RISK_OF_PRIONS).build();
    public static final FoodProperties COOKED_HUMAN_ORGAN_MEAT_FOOD_COMPONENT = new FoodProperties.Builder().nutrition(4).saturationMod(1.2f).meat().fast()
            .effect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 24000, 0), ChestCavity.config.RISK_OF_PRIONS).effect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 24000, 0), ChestCavity.config.RISK_OF_PRIONS).effect(new MobEffectInstance(MobEffects.WEAKNESS, 24000, 0), ChestCavity.config.RISK_OF_PRIONS).build();
    public static final FoodProperties RAW_HUMAN_SAUSAGE_FOOD_COMPONENT = new FoodProperties.Builder().nutrition(5).saturationMod(.4f).meat()
            .effect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 24000, 1), ChestCavity.config.RISK_OF_PRIONS).effect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 24000, 1), ChestCavity.config.RISK_OF_PRIONS).effect(new MobEffectInstance(MobEffects.WEAKNESS, 24000, 1), ChestCavity.config.RISK_OF_PRIONS).build();
    public static final FoodProperties COOKED_HUMAN_SAUSAGE_FOOD_COMPONENT = new FoodProperties.Builder().nutrition(9).saturationMod(.8f).meat()
            .effect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 24000, 0), ChestCavity.config.RISK_OF_PRIONS).effect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 24000, 0), ChestCavity.config.RISK_OF_PRIONS).effect(new MobEffectInstance(MobEffects.WEAKNESS, 24000, 0), ChestCavity.config.RISK_OF_PRIONS).build();
    public static final FoodProperties RAW_RICH_HUMAN_SAUSAGE_FOOD_COMPONENT = new FoodProperties.Builder().nutrition(5).saturationMod(.6f).meat()
            .effect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 24000, 1), ChestCavity.config.RISK_OF_PRIONS).effect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 24000, 1), ChestCavity.config.RISK_OF_PRIONS).effect(new MobEffectInstance(MobEffects.WEAKNESS, 24000, 1), ChestCavity.config.RISK_OF_PRIONS).build();
    public static final FoodProperties COOKED_RICH_HUMAN_SAUSAGE_FOOD_COMPONENT = new FoodProperties.Builder().nutrition(9).saturationMod(1.2f).meat()
            .effect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 24000, 0), ChestCavity.config.RISK_OF_PRIONS).effect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 24000, 0), ChestCavity.config.RISK_OF_PRIONS).effect(new MobEffectInstance(MobEffects.WEAKNESS, 24000, 0), ChestCavity.config.RISK_OF_PRIONS).build();

    public static final FoodProperties CUD_FOOD_COMPONENT = new FoodProperties.Builder().nutrition(1).saturationMod(.1f).build();
    public static final FoodProperties FURNACE_POWER_FOOD_COMPONENT = new FoodProperties.Builder().nutrition(1).saturationMod(.6f).build();
}
