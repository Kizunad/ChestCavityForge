package net.tigereye.chestcavity.registration;

import net.minecraft.world.food.Foods;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.RegistryObject;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.items.ChestOpener;
import net.tigereye.chestcavity.items.CreeperAppendix;
import net.tigereye.chestcavity.items.VenomGland;

public class CCItems {
	public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ChestCavity.MODID);


	public static final Item.Properties CHEST_OPENER_PROPERTIES = new Item.Properties().stacksTo(1).tab(CreativeModeTab.TAB_TOOLS);
	public static final Item.Properties FOOD_ITEM_PROPERTIES = new Item.Properties().stacksTo(64).tab(CreativeModeTab.TAB_FOOD);

	public static final RegistryObject<Item> CHEST_OPENER = ITEMS.register("chest_opener", ChestOpener::new);
	public static final RegistryObject<Item> WOODEN_CLEAVER = ITEMS.register("wooden_cleaver", () -> new SwordItem(Tiers.WOOD,6,-3.2f,new Item.Properties().tab(CreativeModeTab.TAB_COMBAT)));
	public static final RegistryObject<Item> GOLD_CLEAVER = ITEMS.register("gold_cleaver", () -> new SwordItem(Tiers.GOLD,6,-3.0f,new Item.Properties().tab(CreativeModeTab.TAB_COMBAT)));
	public static final RegistryObject<Item> STONE_CLEAVER = ITEMS.register("stone_cleaver", () -> new SwordItem(Tiers.STONE,7,-3.2f,new Item.Properties().tab(CreativeModeTab.TAB_COMBAT)));
	public static final RegistryObject<Item> IRON_CLEAVER = ITEMS.register("iron_cleaver", () -> new SwordItem(Tiers.IRON,6,-3.1f,new Item.Properties().tab(CreativeModeTab.TAB_COMBAT)));
	public static final RegistryObject<Item> DIAMOND_CLEAVER = ITEMS.register("diamond_cleaver", () -> new SwordItem(Tiers.DIAMOND,5,-3.0f,new Item.Properties().tab(CreativeModeTab.TAB_COMBAT)));
	public static final RegistryObject<Item> NETHERITE_CLEAVER = ITEMS.register("netherite_cleaver", () -> new SwordItem(Tiers.NETHERITE,5,-3.0f,new Item.Properties().tab(CreativeModeTab.TAB_COMBAT).fireResistant()));

	public static final RegistryObject<Item> HUMAN_APPENDIX = ITEMS.register("appendix", () -> new Item(new Item.Properties().stacksTo(1).tab(ChestCavity.ORGAN_ITEM_GROUP).food(CCFoodComponents.RAW_HUMAN_ORGAN_MEAT_FOOD_COMPONENT)));
	public static final RegistryObject<Item> HUMAN_HEART = ITEMS.register("heart", () -> new Item(new Item.Properties().stacksTo(1).tab(ChestCavity.ORGAN_ITEM_GROUP).food(CCFoodComponents.RAW_HUMAN_ORGAN_MEAT_FOOD_COMPONENT)));
	public static final RegistryObject<Item> HUMAN_INTESTINE = ITEMS.register("intestine", () -> new Item(new Item.Properties().stacksTo(1).tab(ChestCavity.ORGAN_ITEM_GROUP).food(CCFoodComponents.RAW_HUMAN_ORGAN_MEAT_FOOD_COMPONENT)));
	public static final RegistryObject<Item> HUMAN_KIDNEY = ITEMS.register("kidney", () -> new Item(new Item.Properties().stacksTo(1).tab(ChestCavity.ORGAN_ITEM_GROUP).food(CCFoodComponents.RAW_HUMAN_ORGAN_MEAT_FOOD_COMPONENT)));
	public static final RegistryObject<Item> HUMAN_LIVER = ITEMS.register("liver", () -> new Item(new Item.Properties().stacksTo(1).tab(ChestCavity.ORGAN_ITEM_GROUP).food(CCFoodComponents.RAW_HUMAN_ORGAN_MEAT_FOOD_COMPONENT)));
	public static final RegistryObject<Item> HUMAN_LUNG = ITEMS.register("lung", () -> new Item(new Item.Properties().stacksTo(1).tab(ChestCavity.ORGAN_ITEM_GROUP).food(CCFoodComponents.RAW_HUMAN_ORGAN_MEAT_FOOD_COMPONENT)));
	public static final RegistryObject<Item> HUMAN_MUSCLE = ITEMS.register("muscle", () -> new Item(new Item.Properties().stacksTo(16).tab(ChestCavity.ORGAN_ITEM_GROUP).food(CCFoodComponents.HUMAN_MUSCLE_FOOD_COMPONENT)));
	public static final RegistryObject<Item> HUMAN_RIB = ITEMS.register("rib", () -> new Item(new Item.Properties().stacksTo(4).tab(ChestCavity.ORGAN_ITEM_GROUP)));
	public static final RegistryObject<Item> HUMAN_SPINE = ITEMS.register("spine", () -> new Item(new Item.Properties().stacksTo(1).tab(ChestCavity.ORGAN_ITEM_GROUP)));
	public static final RegistryObject<Item> HUMAN_SPLEEN = ITEMS.register("spleen", () -> new Item(new Item.Properties().stacksTo(1).tab(ChestCavity.ORGAN_ITEM_GROUP).food(CCFoodComponents.RAW_HUMAN_ORGAN_MEAT_FOOD_COMPONENT)));
	public static final RegistryObject<Item> HUMAN_STOMACH = ITEMS.register("stomach", () -> new Item(new Item.Properties().stacksTo(1).tab(ChestCavity.ORGAN_ITEM_GROUP).food(CCFoodComponents.RAW_HUMAN_ORGAN_MEAT_FOOD_COMPONENT)));

	public static final RegistryObject<Item> ROTTEN_APPENDIX = ITEMS.register("rotten_appendix", () -> new Item(new Item.Properties().stacksTo(1).tab(ChestCavity.ORGAN_ITEM_GROUP).food(Foods.ROTTEN_FLESH)));
	public static final RegistryObject<Item> ROTTEN_HEART = ITEMS.register("rotten_heart", () -> new Item(new Item.Properties().stacksTo(1).tab(ChestCavity.ORGAN_ITEM_GROUP).food(Foods.ROTTEN_FLESH)));
	public static final RegistryObject<Item> ROTTEN_INTESTINE = ITEMS.register("rotten_intestine", () -> new Item(new Item.Properties().stacksTo(1).tab(ChestCavity.ORGAN_ITEM_GROUP).food(Foods.ROTTEN_FLESH)));
	public static final RegistryObject<Item> ROTTEN_KIDNEY = ITEMS.register("rotten_kidney", () -> new Item(new Item.Properties().stacksTo(1).tab(ChestCavity.ORGAN_ITEM_GROUP).food(Foods.ROTTEN_FLESH)));
	public static final RegistryObject<Item> ROTTEN_LIVER = ITEMS.register("rotten_liver", () -> new Item(new Item.Properties().stacksTo(1).tab(ChestCavity.ORGAN_ITEM_GROUP).food(Foods.ROTTEN_FLESH)));
	public static final RegistryObject<Item> ROTTEN_LUNG = ITEMS.register("rotten_lung", () -> new Item(new Item.Properties().stacksTo(1).tab(ChestCavity.ORGAN_ITEM_GROUP).food(Foods.ROTTEN_FLESH)));
	public static final RegistryObject<Item> ROTTEN_MUSCLE = ITEMS.register("rotten_muscle", () -> new Item(new Item.Properties().stacksTo(16).tab(ChestCavity.ORGAN_ITEM_GROUP).food(CCFoodComponents.ROTTEN_MUSCLE_FOOD_COMPONENT)));
	public static final RegistryObject<Item> ROTTEN_RIB = ITEMS.register("rotten_rib", () -> new Item(new Item.Properties().stacksTo(4).tab(ChestCavity.ORGAN_ITEM_GROUP)));
	public static final RegistryObject<Item> ROTTEN_SPINE = ITEMS.register("rotten_spine", () -> new Item(new Item.Properties().stacksTo(1).tab(ChestCavity.ORGAN_ITEM_GROUP)));
	public static final RegistryObject<Item> ROTTEN_SPLEEN = ITEMS.register("rotten_spleen", () -> new Item(new Item.Properties().stacksTo(1).tab(ChestCavity.ORGAN_ITEM_GROUP).food(Foods.ROTTEN_FLESH)));
	public static final RegistryObject<Item> ROTTEN_STOMACH = ITEMS.register("rotten_stomach", () -> new Item(new Item.Properties().stacksTo(1).tab(ChestCavity.ORGAN_ITEM_GROUP).food(Foods.ROTTEN_FLESH)));
	public static final RegistryObject<Item> WITHERED_RIB = ITEMS.register("withered_rib", () -> new Item(new Item.Properties().stacksTo(4).tab(ChestCavity.ORGAN_ITEM_GROUP)));
	public static final RegistryObject<Item> WITHERED_SPINE = ITEMS.register("withered_spine", () -> new Item(new Item.Properties().stacksTo(1).tab(ChestCavity.ORGAN_ITEM_GROUP)));
	public static final RegistryObject<Item> WRITHING_SOULSAND = ITEMS.register("writhing_soulsand", () -> new Item(new Item.Properties().stacksTo(16).tab(ChestCavity.ORGAN_ITEM_GROUP)));

	public static final RegistryObject<Item> ANIMAL_APPENDIX = ITEMS.register("animal_appendix", () -> new Item(new Item.Properties().stacksTo(1).tab(ChestCavity.ORGAN_ITEM_GROUP).food(CCFoodComponents.RAW_ORGAN_MEAT_FOOD_COMPONENT)));
	public static final RegistryObject<Item> ANIMAL_HEART = ITEMS.register("animal_heart", () -> new Item(new Item.Properties().stacksTo(1).tab(ChestCavity.ORGAN_ITEM_GROUP).food(CCFoodComponents.RAW_ORGAN_MEAT_FOOD_COMPONENT)));
	public static final RegistryObject<Item> ANIMAL_INTESTINE = ITEMS.register("animal_intestine", () -> new Item(new Item.Properties().stacksTo(1).tab(ChestCavity.ORGAN_ITEM_GROUP).food(CCFoodComponents.RAW_ORGAN_MEAT_FOOD_COMPONENT)));
	public static final RegistryObject<Item> ANIMAL_KIDNEY = ITEMS.register("animal_kidney", () -> new Item(new Item.Properties().stacksTo(1).tab(ChestCavity.ORGAN_ITEM_GROUP).food(CCFoodComponents.RAW_ORGAN_MEAT_FOOD_COMPONENT)));
	public static final RegistryObject<Item> ANIMAL_LIVER = ITEMS.register("animal_liver", () -> new Item(new Item.Properties().stacksTo(1).tab(ChestCavity.ORGAN_ITEM_GROUP).food(CCFoodComponents.RAW_ORGAN_MEAT_FOOD_COMPONENT)));
	public static final RegistryObject<Item> ANIMAL_LUNG = ITEMS.register("animal_lung", () -> new Item(new Item.Properties().stacksTo(1).tab(ChestCavity.ORGAN_ITEM_GROUP).food(CCFoodComponents.RAW_ORGAN_MEAT_FOOD_COMPONENT)));
	public static final RegistryObject<Item> ANIMAL_MUSCLE = ITEMS.register("animal_muscle", () -> new Item(new Item.Properties().stacksTo(16).tab(ChestCavity.ORGAN_ITEM_GROUP).food(CCFoodComponents.ANIMAL_MUSCLE_FOOD_COMPONENT)));
	public static final RegistryObject<Item> ANIMAL_RIB = ITEMS.register("animal_rib", () -> new Item(new Item.Properties().stacksTo(4).tab(ChestCavity.ORGAN_ITEM_GROUP)));
	public static final RegistryObject<Item> ANIMAL_SPINE = ITEMS.register("animal_spine", () -> new Item(new Item.Properties().stacksTo(1).tab(ChestCavity.ORGAN_ITEM_GROUP)));
	public static final RegistryObject<Item> ANIMAL_SPLEEN = ITEMS.register("animal_spleen", () -> new Item(new Item.Properties().stacksTo(1).tab(ChestCavity.ORGAN_ITEM_GROUP).food(CCFoodComponents.RAW_ORGAN_MEAT_FOOD_COMPONENT)));
	public static final RegistryObject<Item> ANIMAL_STOMACH = ITEMS.register("animal_stomach", () -> new Item(new Item.Properties().stacksTo(1).tab(ChestCavity.ORGAN_ITEM_GROUP).food(CCFoodComponents.RAW_ORGAN_MEAT_FOOD_COMPONENT)));
	public static final RegistryObject<Item> AQUATIC_MUSCLE = ITEMS.register("aquatic_muscle", () -> new Item(new Item.Properties().stacksTo(16).tab(ChestCavity.ORGAN_ITEM_GROUP).food(CCFoodComponents.ANIMAL_MUSCLE_FOOD_COMPONENT)));
	public static final RegistryObject<Item> FISH_MUSCLE = ITEMS.register("fish_muscle", () -> new Item(new Item.Properties().stacksTo(16).tab(ChestCavity.ORGAN_ITEM_GROUP).food(CCFoodComponents.ANIMAL_MUSCLE_FOOD_COMPONENT)));
	public static final RegistryObject<Item> GILLS = ITEMS.register("gills", () -> new Item(new Item.Properties().stacksTo(1).tab(ChestCavity.ORGAN_ITEM_GROUP).food(CCFoodComponents.RAW_ORGAN_MEAT_FOOD_COMPONENT)));
	public static final RegistryObject<Item> LLAMA_LUNG = ITEMS.register("llama_lung", () -> new Item(new Item.Properties().stacksTo(1).tab(ChestCavity.ORGAN_ITEM_GROUP).food(CCFoodComponents.RAW_ORGAN_MEAT_FOOD_COMPONENT)));
	public static final RegistryObject<Item> CARNIVORE_STOMACH = ITEMS.register("carnivore_stomach", () -> new Item(new Item.Properties().stacksTo(1).tab(ChestCavity.ORGAN_ITEM_GROUP).food(CCFoodComponents.RAW_ORGAN_MEAT_FOOD_COMPONENT)));
	public static final RegistryObject<Item> CARNIVORE_INTESTINE = ITEMS.register("carnivore_intestine", () -> new Item(new Item.Properties().stacksTo(1).tab(ChestCavity.ORGAN_ITEM_GROUP).food(CCFoodComponents.RAW_ORGAN_MEAT_FOOD_COMPONENT)));
	public static final RegistryObject<Item> HERBIVORE_RUMEN = ITEMS.register("herbivore_rumen", () -> new Item(new Item.Properties().stacksTo(1).tab(ChestCavity.ORGAN_ITEM_GROUP).food(CCFoodComponents.RAW_ORGAN_MEAT_FOOD_COMPONENT)));
	public static final RegistryObject<Item> HERBIVORE_STOMACH = ITEMS.register("herbivore_stomach", () -> new Item(new Item.Properties().stacksTo(1).tab(ChestCavity.ORGAN_ITEM_GROUP).food(CCFoodComponents.RAW_ORGAN_MEAT_FOOD_COMPONENT)));
	public static final RegistryObject<Item> HERBIVORE_INTESTINE = ITEMS.register("herbivore_intestine", () -> new Item(new Item.Properties().stacksTo(1).tab(ChestCavity.ORGAN_ITEM_GROUP).food(CCFoodComponents.RAW_ORGAN_MEAT_FOOD_COMPONENT)));
	public static final RegistryObject<Item> BRUTISH_MUSCLE = ITEMS.register("brutish_muscle", () -> new Item(new Item.Properties().stacksTo(16).tab(ChestCavity.ORGAN_ITEM_GROUP).food(CCFoodComponents.ANIMAL_MUSCLE_FOOD_COMPONENT)));
	public static final RegistryObject<Item> SWIFT_MUSCLE = ITEMS.register("swift_muscle", () -> new Item(new Item.Properties().stacksTo(16).tab(ChestCavity.ORGAN_ITEM_GROUP).food(CCFoodComponents.ANIMAL_MUSCLE_FOOD_COMPONENT)));
	public static final RegistryObject<Item> SPRINGY_MUSCLE = ITEMS.register("springy_muscle", () -> new Item(new Item.Properties().stacksTo(16).tab(ChestCavity.ORGAN_ITEM_GROUP).food(CCFoodComponents.ANIMAL_MUSCLE_FOOD_COMPONENT)));

	public static final RegistryObject<Item> FIREPROOF_APPENDIX = ITEMS.register("fireproof_appendix", () -> new Item(new Item.Properties().stacksTo(1).tab(ChestCavity.ORGAN_ITEM_GROUP).food(CCFoodComponents.RAW_ORGAN_MEAT_FOOD_COMPONENT)));
	public static final RegistryObject<Item> FIREPROOF_HEART = ITEMS.register("fireproof_heart", () -> new Item(new Item.Properties().stacksTo(1).tab(ChestCavity.ORGAN_ITEM_GROUP).food(CCFoodComponents.RAW_ORGAN_MEAT_FOOD_COMPONENT)));
	public static final RegistryObject<Item> FIREPROOF_INTESTINE = ITEMS.register("fireproof_intestine", () -> new Item(new Item.Properties().stacksTo(1).tab(ChestCavity.ORGAN_ITEM_GROUP).food(CCFoodComponents.RAW_ORGAN_MEAT_FOOD_COMPONENT)));
	public static final RegistryObject<Item> FIREPROOF_KIDNEY = ITEMS.register("fireproof_kidney", () -> new Item(new Item.Properties().stacksTo(1).tab(ChestCavity.ORGAN_ITEM_GROUP).food(CCFoodComponents.RAW_ORGAN_MEAT_FOOD_COMPONENT)));
	public static final RegistryObject<Item> FIREPROOF_LIVER = ITEMS.register("fireproof_liver", () -> new Item(new Item.Properties().stacksTo(1).tab(ChestCavity.ORGAN_ITEM_GROUP).food(CCFoodComponents.RAW_ORGAN_MEAT_FOOD_COMPONENT)));
	public static final RegistryObject<Item> FIREPROOF_LUNG = ITEMS.register("fireproof_lung", () -> new Item(new Item.Properties().stacksTo(1).tab(ChestCavity.ORGAN_ITEM_GROUP).food(CCFoodComponents.RAW_ORGAN_MEAT_FOOD_COMPONENT)));
	public static final RegistryObject<Item> FIREPROOF_MUSCLE = ITEMS.register("fireproof_muscle", () -> new Item(new Item.Properties().stacksTo(16).tab(ChestCavity.ORGAN_ITEM_GROUP).food(CCFoodComponents.ANIMAL_MUSCLE_FOOD_COMPONENT)));
	public static final RegistryObject<Item> FIREPROOF_RIB = ITEMS.register("fireproof_rib", () -> new Item(new Item.Properties().stacksTo(4).tab(ChestCavity.ORGAN_ITEM_GROUP)));
	public static final RegistryObject<Item> FIREPROOF_SPINE = ITEMS.register("fireproof_spine", () -> new Item(new Item.Properties().stacksTo(1).tab(ChestCavity.ORGAN_ITEM_GROUP)));
	public static final RegistryObject<Item> FIREPROOF_SPLEEN = ITEMS.register("fireproof_spleen", () -> new Item(new Item.Properties().stacksTo(1).tab(ChestCavity.ORGAN_ITEM_GROUP).food(CCFoodComponents.RAW_ORGAN_MEAT_FOOD_COMPONENT)));
	public static final RegistryObject<Item> FIREPROOF_STOMACH = ITEMS.register("fireproof_stomach", () -> new Item(new Item.Properties().stacksTo(1).tab(ChestCavity.ORGAN_ITEM_GROUP).food(CCFoodComponents.RAW_ORGAN_MEAT_FOOD_COMPONENT)));

	public static final RegistryObject<Item> SMALL_ANIMAL_APPENDIX = ITEMS.register("small_animal_appendix", () -> new Item(new Item.Properties().stacksTo(1).tab(ChestCavity.ORGAN_ITEM_GROUP).food(CCFoodComponents.SMALL_ANIMAL_MUSCLE_FOOD_COMPONENT)));
	public static final RegistryObject<Item> SMALL_ANIMAL_HEART = ITEMS.register("small_animal_heart", () -> new Item(new Item.Properties().stacksTo(1).tab(ChestCavity.ORGAN_ITEM_GROUP).food(CCFoodComponents.SMALL_ANIMAL_MUSCLE_FOOD_COMPONENT)));
	public static final RegistryObject<Item> SMALL_ANIMAL_INTESTINE = ITEMS.register("small_animal_intestine", () -> new Item(new Item.Properties().stacksTo(1).tab(ChestCavity.ORGAN_ITEM_GROUP).food(CCFoodComponents.SMALL_ANIMAL_MUSCLE_FOOD_COMPONENT)));
	public static final RegistryObject<Item> SMALL_ANIMAL_KIDNEY = ITEMS.register("small_animal_kidney", () -> new Item(new Item.Properties().stacksTo(1).tab(ChestCavity.ORGAN_ITEM_GROUP).food(CCFoodComponents.SMALL_ANIMAL_MUSCLE_FOOD_COMPONENT)));
	public static final RegistryObject<Item> SMALL_ANIMAL_LIVER = ITEMS.register("small_animal_liver", () -> new Item(new Item.Properties().stacksTo(1).tab(ChestCavity.ORGAN_ITEM_GROUP).food(CCFoodComponents.SMALL_ANIMAL_MUSCLE_FOOD_COMPONENT)));
	public static final RegistryObject<Item> SMALL_ANIMAL_LUNG = ITEMS.register("small_animal_lung", () -> new Item(new Item.Properties().stacksTo(1).tab(ChestCavity.ORGAN_ITEM_GROUP).food(CCFoodComponents.SMALL_ANIMAL_MUSCLE_FOOD_COMPONENT)));
	public static final RegistryObject<Item> SMALL_ANIMAL_MUSCLE = ITEMS.register("small_animal_muscle", () -> new Item(new Item.Properties().stacksTo(16).tab(ChestCavity.ORGAN_ITEM_GROUP).food(CCFoodComponents.SMALL_ANIMAL_MUSCLE_FOOD_COMPONENT)));
	public static final RegistryObject<Item> SMALL_ANIMAL_RIB = ITEMS.register("small_animal_rib", () -> new Item(new Item.Properties().stacksTo(4).tab(ChestCavity.ORGAN_ITEM_GROUP)));
	public static final RegistryObject<Item> SMALL_ANIMAL_SPINE = ITEMS.register("small_animal_spine", () -> new Item(new Item.Properties().stacksTo(1).tab(ChestCavity.ORGAN_ITEM_GROUP)));
	public static final RegistryObject<Item> SMALL_ANIMAL_SPLEEN = ITEMS.register("small_animal_spleen", () -> new Item(new Item.Properties().stacksTo(1).tab(ChestCavity.ORGAN_ITEM_GROUP).food(CCFoodComponents.SMALL_ANIMAL_MUSCLE_FOOD_COMPONENT)));
	public static final RegistryObject<Item> SMALL_ANIMAL_STOMACH = ITEMS.register("small_animal_stomach", () -> new Item(new Item.Properties().stacksTo(1).tab(ChestCavity.ORGAN_ITEM_GROUP).food(CCFoodComponents.SMALL_ANIMAL_MUSCLE_FOOD_COMPONENT)));
	public static final RegistryObject<Item> RABBIT_HEART = ITEMS.register("rabbit_heart", () -> new Item(new Item.Properties().stacksTo(1).tab(ChestCavity.ORGAN_ITEM_GROUP).food(CCFoodComponents.SMALL_ANIMAL_MUSCLE_FOOD_COMPONENT)));
	public static final RegistryObject<Item> SMALL_AQUATIC_MUSCLE = ITEMS.register("small_aquatic_muscle", () -> new Item(new Item.Properties().stacksTo(16).tab(ChestCavity.ORGAN_ITEM_GROUP).food(CCFoodComponents.SMALL_ANIMAL_MUSCLE_FOOD_COMPONENT)));
	public static final RegistryObject<Item> SMALL_FISH_MUSCLE = ITEMS.register("small_fish_muscle", () -> new Item(new Item.Properties().stacksTo(16).tab(ChestCavity.ORGAN_ITEM_GROUP).food(CCFoodComponents.SMALL_ANIMAL_MUSCLE_FOOD_COMPONENT)));
	public static final RegistryObject<Item> SMALL_SPRINGY_MUSCLE = ITEMS.register("small_springy_muscle", () -> new Item(new Item.Properties().stacksTo(16).tab(ChestCavity.ORGAN_ITEM_GROUP).food(CCFoodComponents.SMALL_ANIMAL_MUSCLE_FOOD_COMPONENT)));
	public static final RegistryObject<Item> SMALL_GILLS = ITEMS.register("small_gills", () -> new Item(new Item.Properties().stacksTo(1).tab(ChestCavity.ORGAN_ITEM_GROUP).food(CCFoodComponents.SMALL_ANIMAL_MUSCLE_FOOD_COMPONENT)));
	public static final RegistryObject<Item> SMALL_CARNIVORE_STOMACH = ITEMS.register("small_carnivore_stomach", () -> new Item(new Item.Properties().stacksTo(1).tab(ChestCavity.ORGAN_ITEM_GROUP).food(CCFoodComponents.SMALL_ANIMAL_MUSCLE_FOOD_COMPONENT)));
	public static final RegistryObject<Item> SMALL_CARNIVORE_INTESTINE = ITEMS.register("small_carnivore_intestine", () -> new Item(new Item.Properties().stacksTo(1).tab(ChestCavity.ORGAN_ITEM_GROUP).food(CCFoodComponents.SMALL_ANIMAL_MUSCLE_FOOD_COMPONENT)));
	public static final RegistryObject<Item> SMALL_HERBIVORE_STOMACH = ITEMS.register("small_herbivore_stomach", () -> new Item(new Item.Properties().stacksTo(1).tab(ChestCavity.ORGAN_ITEM_GROUP).food(CCFoodComponents.SMALL_ANIMAL_MUSCLE_FOOD_COMPONENT)));
	public static final RegistryObject<Item> SMALL_HERBIVORE_INTESTINE = ITEMS.register("small_herbivore_intestine", () -> new Item(new Item.Properties().stacksTo(1).tab(ChestCavity.ORGAN_ITEM_GROUP).food(CCFoodComponents.SMALL_ANIMAL_MUSCLE_FOOD_COMPONENT)));

	public static final RegistryObject<Item> INSECT_HEART = ITEMS.register("insect_heart", () -> new Item(new Item.Properties().stacksTo(1).tab(ChestCavity.ORGAN_ITEM_GROUP).food(CCFoodComponents.RAW_TOXIC_ORGAN_MEAT_FOOD_COMPONENT)));
	public static final RegistryObject<Item> INSECT_INTESTINE = ITEMS.register("insect_intestine", () -> new Item(new Item.Properties().stacksTo(1).tab(ChestCavity.ORGAN_ITEM_GROUP).food(CCFoodComponents.RAW_TOXIC_ORGAN_MEAT_FOOD_COMPONENT)));
	public static final RegistryObject<Item> INSECT_LUNG = ITEMS.register("insect_lung", () -> new Item(new Item.Properties().stacksTo(1).tab(ChestCavity.ORGAN_ITEM_GROUP).food(CCFoodComponents.RAW_TOXIC_ORGAN_MEAT_FOOD_COMPONENT)));
	public static final RegistryObject<Item> INSECT_MUSCLE = ITEMS.register("insect_muscle", () -> new Item(new Item.Properties().stacksTo(16).tab(ChestCavity.ORGAN_ITEM_GROUP).food(CCFoodComponents.INSECT_MUSCLE_FOOD_COMPONENT)));
	public static final RegistryObject<Item> INSECT_STOMACH = ITEMS.register("insect_stomach", () -> new Item(new Item.Properties().stacksTo(1).tab(ChestCavity.ORGAN_ITEM_GROUP).food(CCFoodComponents.RAW_TOXIC_ORGAN_MEAT_FOOD_COMPONENT)));
	public static final RegistryObject<Item> INSECT_CAECA = ITEMS.register("insect_caeca", () -> new Item(new Item.Properties().stacksTo(1).tab(ChestCavity.ORGAN_ITEM_GROUP).food(CCFoodComponents.RAW_TOXIC_ORGAN_MEAT_FOOD_COMPONENT)));
	public static final RegistryObject<Item> SILK_GLAND = ITEMS.register("silk_gland", () -> new Item(new Item.Properties().stacksTo(1).tab(ChestCavity.ORGAN_ITEM_GROUP).food(CCFoodComponents.RAW_TOXIC_ORGAN_MEAT_FOOD_COMPONENT)));
	public static final RegistryObject<Item> VENOM_GLAND = ITEMS.register("venom_gland", () -> new VenomGland());//.setOrganQuality(CCOrganScores.VENOMOUS,1f));

	public static final RegistryObject<Item> ENDER_APPENDIX = ITEMS.register("ender_appendix", () -> new Item(new Item.Properties().stacksTo(1).tab(ChestCavity.ORGAN_ITEM_GROUP).food(CCFoodComponents.RAW_ALIEN_ORGAN_MEAT_FOOD_COMPONENT)));
	public static final RegistryObject<Item> ENDER_HEART = ITEMS.register("ender_heart", () -> new Item(new Item.Properties().stacksTo(1).tab(ChestCavity.ORGAN_ITEM_GROUP).food(CCFoodComponents.RAW_ALIEN_ORGAN_MEAT_FOOD_COMPONENT)));
	public static final RegistryObject<Item> ENDER_INTESTINE = ITEMS.register("ender_intestine", () -> new Item(new Item.Properties().stacksTo(1).tab(ChestCavity.ORGAN_ITEM_GROUP).food(CCFoodComponents.RAW_ALIEN_ORGAN_MEAT_FOOD_COMPONENT)));
	public static final RegistryObject<Item> ENDER_KIDNEY = ITEMS.register("ender_kidney", () -> new Item(new Item.Properties().stacksTo(1).tab(ChestCavity.ORGAN_ITEM_GROUP).food(CCFoodComponents.RAW_ALIEN_ORGAN_MEAT_FOOD_COMPONENT)));
	public static final RegistryObject<Item> ENDER_LIVER = ITEMS.register("ender_liver", () -> new Item(new Item.Properties().stacksTo(1).tab(ChestCavity.ORGAN_ITEM_GROUP).food(CCFoodComponents.RAW_ALIEN_ORGAN_MEAT_FOOD_COMPONENT)));
	public static final RegistryObject<Item> ENDER_LUNG = ITEMS.register("ender_lung", () -> new Item(new Item.Properties().stacksTo(1).tab(ChestCavity.ORGAN_ITEM_GROUP).food(CCFoodComponents.RAW_ALIEN_ORGAN_MEAT_FOOD_COMPONENT)));
	public static final RegistryObject<Item> ENDER_MUSCLE = ITEMS.register("ender_muscle", () -> new Item(new Item.Properties().stacksTo(16).tab(ChestCavity.ORGAN_ITEM_GROUP).food(CCFoodComponents.ALIEN_MUSCLE_FOOD_COMPONENT)));
	public static final RegistryObject<Item> ENDER_RIB = ITEMS.register("ender_rib", () -> new Item(new Item.Properties().stacksTo(4).tab(ChestCavity.ORGAN_ITEM_GROUP)));
	public static final RegistryObject<Item> ENDER_SPINE = ITEMS.register("ender_spine", () -> new Item(new Item.Properties().stacksTo(1).tab(ChestCavity.ORGAN_ITEM_GROUP)));
	public static final RegistryObject<Item> ENDER_SPLEEN = ITEMS.register("ender_spleen", () -> new Item(new Item.Properties().stacksTo(1).tab(ChestCavity.ORGAN_ITEM_GROUP).food(CCFoodComponents.RAW_ALIEN_ORGAN_MEAT_FOOD_COMPONENT)));
	public static final RegistryObject<Item> ENDER_STOMACH = ITEMS.register("ender_stomach", () -> new Item(new Item.Properties().stacksTo(1).tab(ChestCavity.ORGAN_ITEM_GROUP).food(CCFoodComponents.RAW_ALIEN_ORGAN_MEAT_FOOD_COMPONENT)));

	public static final RegistryObject<Item> DRAGON_APPENDIX = ITEMS.register("dragon_appendix", () -> new Item(new Item.Properties().stacksTo(1).tab(ChestCavity.ORGAN_ITEM_GROUP).food(CCFoodComponents.RAW_DRAGON_ORGAN_MEAT_FOOD_COMPONENT)));
	public static final RegistryObject<Item> DRAGON_HEART = ITEMS.register("dragon_heart", () -> new Item(new Item.Properties().stacksTo(1).tab(ChestCavity.ORGAN_ITEM_GROUP).food(CCFoodComponents.DRAGON_HEART_FOOD_COMPONENT)));
	public static final RegistryObject<Item> DRAGON_KIDNEY = ITEMS.register("dragon_kidney", () -> new Item(new Item.Properties().stacksTo(1).tab(ChestCavity.ORGAN_ITEM_GROUP).food(CCFoodComponents.RAW_DRAGON_ORGAN_MEAT_FOOD_COMPONENT)));
	public static final RegistryObject<Item> DRAGON_LIVER = ITEMS.register("dragon_liver", () -> new Item(new Item.Properties().stacksTo(1).tab(ChestCavity.ORGAN_ITEM_GROUP).food(CCFoodComponents.RAW_DRAGON_ORGAN_MEAT_FOOD_COMPONENT)));
	public static final RegistryObject<Item> DRAGON_LUNG = ITEMS.register("dragon_lung", () -> new Item(new Item.Properties().stacksTo(1).tab(ChestCavity.ORGAN_ITEM_GROUP).food(CCFoodComponents.RAW_DRAGON_ORGAN_MEAT_FOOD_COMPONENT)));
	public static final RegistryObject<Item> DRAGON_MUSCLE = ITEMS.register("dragon_muscle", () -> new Item(new Item.Properties().stacksTo(16).tab(ChestCavity.ORGAN_ITEM_GROUP).food(CCFoodComponents.DRAGON_MUSCLE_FOOD_COMPONENT)));
	public static final RegistryObject<Item> DRAGON_RIB = ITEMS.register("dragon_rib", () -> new Item(new Item.Properties().stacksTo(4).tab(ChestCavity.ORGAN_ITEM_GROUP)));
	//TODO: Destructive RegistryObject<Item>isions
	public static final RegistryObject<Item> DRAGON_SPINE = ITEMS.register("dragon_spine", () -> new Item(new Item.Properties().stacksTo(1).tab(ChestCavity.ORGAN_ITEM_GROUP)));
	public static final RegistryObject<Item> DRAGON_SPLEEN = ITEMS.register("dragon_spleen", () -> new Item(new Item.Properties().stacksTo(1).tab(ChestCavity.ORGAN_ITEM_GROUP).food(CCFoodComponents.RAW_DRAGON_ORGAN_MEAT_FOOD_COMPONENT)));
	public static final RegistryObject<Item> MANA_REACTOR = ITEMS.register("mana_reactor", () -> new Item(new Item.Properties().stacksTo(1).tab(ChestCavity.ORGAN_ITEM_GROUP).food(CCFoodComponents.RAW_DRAGON_ORGAN_MEAT_FOOD_COMPONENT)));

	public static final RegistryObject<Item> ACTIVE_BLAZE_ROD = ITEMS.register("active_blaze_rod", () -> new Item(new Item.Properties().stacksTo(3).tab(ChestCavity.ORGAN_ITEM_GROUP)));
	public static final RegistryObject<Item> BLAZE_SHELL = ITEMS.register("blaze_shell", () -> new Item(new Item.Properties().stacksTo(4).tab(ChestCavity.ORGAN_ITEM_GROUP)));
	public static final RegistryObject<Item> BLAZE_CORE = ITEMS.register("blaze_core", () -> new Item(new Item.Properties().stacksTo(1).tab(ChestCavity.ORGAN_ITEM_GROUP)));

	public static final RegistryObject<Item> GAS_BLADDER = ITEMS.register("gas_bladder", () -> new Item(new Item.Properties().stacksTo(1).tab(ChestCavity.ORGAN_ITEM_GROUP)));
	public static final RegistryObject<Item> VOLATILE_STOMACH = ITEMS.register("volatile_stomach", () -> new Item(new Item.Properties().stacksTo(1).tab(ChestCavity.ORGAN_ITEM_GROUP)));

	public static final RegistryObject<Item> GOLEM_CABLE = ITEMS.register("golem_cable", () -> new Item(new Item.Properties().stacksTo(1).tab(ChestCavity.ORGAN_ITEM_GROUP)));
	public static final RegistryObject<Item> GOLEM_PLATING = ITEMS.register("golem_plating", () -> new Item(new Item.Properties().stacksTo(4).tab(ChestCavity.ORGAN_ITEM_GROUP)));
	public static final RegistryObject<Item> GOLEM_CORE = ITEMS.register("golem_core", () -> new Item(new Item.Properties().stacksTo(1).tab(ChestCavity.ORGAN_ITEM_GROUP)));
	public static final RegistryObject<Item> INNER_FURNACE = ITEMS.register("inner_furnace", () -> new Item(new Item.Properties().stacksTo(1).tab(ChestCavity.ORGAN_ITEM_GROUP)));
	public static final RegistryObject<Item> PISTON_MUSCLE = ITEMS.register("piston_muscle", () -> new Item(new Item.Properties().stacksTo(16).tab(ChestCavity.ORGAN_ITEM_GROUP)));
	public static final RegistryObject<Item> IRON_SCRAP = ITEMS.register("iron_scrap", () -> new Item(new Item.Properties().tab(CreativeModeTab.TAB_MATERIALS)));

	public static final RegistryObject<Item> SALTWATER_HEART = ITEMS.register("saltwater_heart", () -> new Item(new Item.Properties().stacksTo(1).tab(ChestCavity.ORGAN_ITEM_GROUP)));
	public static final RegistryObject<Item> SALTWATER_LUNG = ITEMS.register("saltwater_lung", () -> new Item(new Item.Properties().stacksTo(1).tab(ChestCavity.ORGAN_ITEM_GROUP)));
	public static final RegistryObject<Item> SALTWATER_MUSCLE = ITEMS.register("saltwater_muscle", () -> new Item(new Item.Properties().stacksTo(16).tab(ChestCavity.ORGAN_ITEM_GROUP)));
	public static final RegistryObject<Item> CREEPER_APPENDIX = ITEMS.register("creeper_appendix", () -> new CreeperAppendix());
	public static final RegistryObject<Item> SHIFTING_LEAVES = ITEMS.register("shifting_leaves", () -> new Item(new Item.Properties().stacksTo(16).tab(ChestCavity.ORGAN_ITEM_GROUP)));
	public static final RegistryObject<Item> SHULKER_SPLEEN = ITEMS.register("shulker_spleen", () -> new Item(new Item.Properties().stacksTo(1).tab(ChestCavity.ORGAN_ITEM_GROUP)));

	public static final RegistryObject<Item> SAUSAGE_SKIN = ITEMS.register("sausage_skin", () -> new Item(new Item.Properties().stacksTo(64).tab(CreativeModeTab.TAB_MISC)));
	public static final RegistryObject<Item> MINI_SAUSAGE_SKIN = ITEMS.register("mini_sausage_skin", () -> new Item(new Item.Properties().stacksTo(64)));

	public static final RegistryObject<Item> BURNT_MEAT_CHUNK = ITEMS.register("burnt_meat_chunk", () -> new Item(FOOD_ITEM_PROPERTIES.food(CCFoodComponents.BURNT_MEAT_CHUNK_COMPONENT)));
	public static final RegistryObject<Item> RAW_ORGAN_MEAT = ITEMS.register("raw_organ_meat", () -> new Item(FOOD_ITEM_PROPERTIES.food(CCFoodComponents.RAW_ORGAN_MEAT_FOOD_COMPONENT)));
	public static final RegistryObject<Item> COOKED_ORGAN_MEAT = ITEMS.register("cooked_organ_meat", () -> new Item(FOOD_ITEM_PROPERTIES.food(CCFoodComponents.COOKED_ORGAN_MEAT_FOOD_COMPONENT)));
	public static final RegistryObject<Item> RAW_BUTCHERED_MEAT = ITEMS.register("raw_butchered_meat", () -> new Item(FOOD_ITEM_PROPERTIES.food(CCFoodComponents.RAW_BUTCHERED_MEAT_FOOD_COMPONENT)));
	public static final RegistryObject<Item> COOKED_BUTCHERED_MEAT = ITEMS.register("cooked_butchered_meat", () -> new Item(FOOD_ITEM_PROPERTIES.food(CCFoodComponents.COOKED_BUTCHERED_MEAT_FOOD_COMPONENT)));
	public static final RegistryObject<Item> RAW_SAUSAGE = ITEMS.register("raw_sausage", () -> new Item(FOOD_ITEM_PROPERTIES.food(CCFoodComponents.RAW_SAUSAGE_FOOD_COMPONENT)));
	public static final RegistryObject<Item> COOKED_SAUSAGE = ITEMS.register("sausage", () -> new Item(FOOD_ITEM_PROPERTIES.food(CCFoodComponents.COOKED_SAUSAGE_FOOD_COMPONENT)));
	public static final RegistryObject<Item> RAW_RICH_SAUSAGE = ITEMS.register("raw_rich_sausage", () -> new Item(FOOD_ITEM_PROPERTIES.food(CCFoodComponents.RAW_RICH_SAUSAGE_FOOD_COMPONENT)));
	public static final RegistryObject<Item> COOKED_RICH_SAUSAGE = ITEMS.register("rich_sausage", () -> new Item(FOOD_ITEM_PROPERTIES.food(CCFoodComponents.COOKED_RICH_SAUSAGE_FOOD_COMPONENT)));
	public static final RegistryObject<Item> RAW_MINI_SAUSAGE = ITEMS.register("raw_mini_sausage", () -> new Item(FOOD_ITEM_PROPERTIES.food(CCFoodComponents.RAW_MINI_SAUSAGE_FOOD_COMPONENT)));
	public static final RegistryObject<Item> COOKED_MINI_SAUSAGE = ITEMS.register("mini_sausage", () -> new Item(FOOD_ITEM_PROPERTIES.food(CCFoodComponents.COOKED_MINI_SAUSAGE_FOOD_COMPONENT)));
	public static final RegistryObject<Item> RAW_RICH_MINI_SAUSAGE = ITEMS.register("raw_rich_mini_sausage", () -> new Item(FOOD_ITEM_PROPERTIES.food(CCFoodComponents.RAW_RICH_MINI_SAUSAGE_FOOD_COMPONENT)));
	public static final RegistryObject<Item> COOKED_RICH_MINI_SAUSAGE = ITEMS.register("rich_mini_sausage", () -> new Item(FOOD_ITEM_PROPERTIES.food(CCFoodComponents.COOKED_RICH_MINI_SAUSAGE_FOOD_COMPONENT)));

	public static final RegistryObject<Item> ROTTEN_SAUSAGE = ITEMS.register("rotten_sausage", () -> new Item(FOOD_ITEM_PROPERTIES.food(CCFoodComponents.ROTTEN_SAUSAGE_FOOD_COMPONENT)));

	public static final RegistryObject<Item> RAW_TOXIC_ORGAN_MEAT = ITEMS.register("raw_toxic_organ_meat", () -> new Item(FOOD_ITEM_PROPERTIES.food(CCFoodComponents.RAW_TOXIC_ORGAN_MEAT_FOOD_COMPONENT)));
	public static final RegistryObject<Item> COOKED_TOXIC_ORGAN_MEAT = ITEMS.register("cooked_toxic_organ_meat", () -> new Item(FOOD_ITEM_PROPERTIES.food(CCFoodComponents.COOKED_TOXIC_ORGAN_MEAT_FOOD_COMPONENT)));
	public static final RegistryObject<Item> RAW_TOXIC_MEAT = ITEMS.register("raw_toxic_meat", () -> new Item(FOOD_ITEM_PROPERTIES.food(CCFoodComponents.RAW_TOXIC_MEAT_FOOD_COMPONENT)));
	public static final RegistryObject<Item> COOKED_TOXIC_MEAT = ITEMS.register("cooked_toxic_meat", () -> new Item(FOOD_ITEM_PROPERTIES.food(CCFoodComponents.COOKED_TOXIC_MEAT_FOOD_COMPONENT)));
	public static final RegistryObject<Item> RAW_TOXIC_SAUSAGE = ITEMS.register("raw_toxic_sausage", () -> new Item(FOOD_ITEM_PROPERTIES.food(CCFoodComponents.RAW_TOXIC_SAUSAGE_FOOD_COMPONENT)));
	public static final RegistryObject<Item> COOKED_TOXIC_SAUSAGE = ITEMS.register("toxic_sausage", () -> new Item(FOOD_ITEM_PROPERTIES.food(CCFoodComponents.COOKED_TOXIC_SAUSAGE_FOOD_COMPONENT)));
	public static final RegistryObject<Item> RAW_RICH_TOXIC_SAUSAGE = ITEMS.register("raw_rich_toxic_sausage", () -> new Item(FOOD_ITEM_PROPERTIES.food(CCFoodComponents.RAW_RICH_TOXIC_SAUSAGE_FOOD_COMPONENT)));
	public static final RegistryObject<Item> COOKED_RICH_TOXIC_SAUSAGE = ITEMS.register("rich_toxic_sausage", () -> new Item(FOOD_ITEM_PROPERTIES.food(CCFoodComponents.COOKED_RICH_TOXIC_SAUSAGE_FOOD_COMPONENT)));

	public static final RegistryObject<Item> RAW_HUMAN_ORGAN_MEAT = ITEMS.register("raw_human_organ_meat", () -> new Item(FOOD_ITEM_PROPERTIES.food(CCFoodComponents.RAW_HUMAN_ORGAN_MEAT_FOOD_COMPONENT)));
	public static final RegistryObject<Item> COOKED_HUMAN_ORGAN_MEAT = ITEMS.register("cooked_human_organ_meat", () -> new Item(FOOD_ITEM_PROPERTIES.food(CCFoodComponents.COOKED_HUMAN_ORGAN_MEAT_FOOD_COMPONENT)));
	public static final RegistryObject<Item> RAW_MAN_MEAT = ITEMS.register("raw_man_meat", () -> new Item(FOOD_ITEM_PROPERTIES.food(CCFoodComponents.RAW_MAN_MEAT_FOOD_COMPONENT)));
	public static final RegistryObject<Item> COOKED_MAN_MEAT = ITEMS.register("cooked_man_meat", () -> new Item(FOOD_ITEM_PROPERTIES.food(CCFoodComponents.COOKED_MAN_MEAT_FOOD_COMPONENT)));
	public static final RegistryObject<Item> RAW_HUMAN_SAUSAGE = ITEMS.register("raw_human_sausage", () -> new Item(FOOD_ITEM_PROPERTIES.food(CCFoodComponents.RAW_HUMAN_SAUSAGE_FOOD_COMPONENT)));
	public static final RegistryObject<Item> COOKED_HUMAN_SAUSAGE = ITEMS.register("human_sausage", () -> new Item(FOOD_ITEM_PROPERTIES.food(CCFoodComponents.COOKED_HUMAN_SAUSAGE_FOOD_COMPONENT)));
	public static final RegistryObject<Item> RAW_RICH_HUMAN_SAUSAGE = ITEMS.register("raw_rich_human_sausage", () -> new Item(FOOD_ITEM_PROPERTIES.food(CCFoodComponents.RAW_RICH_HUMAN_SAUSAGE_FOOD_COMPONENT)));
	public static final RegistryObject<Item> COOKED_RICH_HUMAN_SAUSAGE = ITEMS.register("rich_human_sausage", () -> new Item(FOOD_ITEM_PROPERTIES.food(CCFoodComponents.COOKED_RICH_HUMAN_SAUSAGE_FOOD_COMPONENT)));

	public static final RegistryObject<Item> RAW_ALIEN_ORGAN_MEAT = ITEMS.register("raw_alien_organ_meat", () -> new Item(FOOD_ITEM_PROPERTIES.food(CCFoodComponents.RAW_ALIEN_ORGAN_MEAT_FOOD_COMPONENT)));
	public static final RegistryObject<Item> COOKED_ALIEN_ORGAN_MEAT = ITEMS.register("cooked_alien_organ_meat", () -> new Item(FOOD_ITEM_PROPERTIES.food(CCFoodComponents.COOKED_ALIEN_ORGAN_MEAT_FOOD_COMPONENT)));
	public static final RegistryObject<Item> RAW_ALIEN_MEAT = ITEMS.register("raw_alien_meat", () -> new Item(FOOD_ITEM_PROPERTIES.food(CCFoodComponents.RAW_ALIEN_MEAT_FOOD_COMPONENT)));
	public static final RegistryObject<Item> COOKED_ALIEN_MEAT = ITEMS.register("cooked_alien_meat", () -> new Item(FOOD_ITEM_PROPERTIES.food(CCFoodComponents.COOKED_ALIEN_MEAT_FOOD_COMPONENT)));
	public static final RegistryObject<Item> RAW_ALIEN_SAUSAGE = ITEMS.register("raw_alien_sausage", () -> new Item(FOOD_ITEM_PROPERTIES.food(CCFoodComponents.RAW_ALIEN_SAUSAGE_FOOD_COMPONENT)));
	public static final RegistryObject<Item> COOKED_ALIEN_SAUSAGE = ITEMS.register("alien_sausage", () -> new Item(FOOD_ITEM_PROPERTIES.food(CCFoodComponents.COOKED_ALIEN_SAUSAGE_FOOD_COMPONENT)));
	public static final RegistryObject<Item> RAW_RICH_ALIEN_SAUSAGE = ITEMS.register("raw_rich_alien_sausage", () -> new Item(FOOD_ITEM_PROPERTIES.food(CCFoodComponents.RAW_RICH_ALIEN_SAUSAGE_FOOD_COMPONENT)));
	public static final RegistryObject<Item> COOKED_RICH_ALIEN_SAUSAGE = ITEMS.register("rich_alien_sausage", () -> new Item(FOOD_ITEM_PROPERTIES.food(CCFoodComponents.COOKED_RICH_ALIEN_SAUSAGE_FOOD_COMPONENT)));

	public static final RegistryObject<Item> RAW_DRAGON_ORGAN_MEAT = ITEMS.register("raw_dragon_organ_meat", () -> new Item(FOOD_ITEM_PROPERTIES.food(CCFoodComponents.RAW_DRAGON_ORGAN_MEAT_FOOD_COMPONENT)));
	public static final RegistryObject<Item> COOKED_DRAGON_ORGAN_MEAT = ITEMS.register("cooked_dragon_organ_meat", () -> new Item(FOOD_ITEM_PROPERTIES.food(CCFoodComponents.COOKED_DRAGON_ORGAN_MEAT_FOOD_COMPONENT)));
	public static final RegistryObject<Item> RAW_DRAGON_MEAT = ITEMS.register("raw_dragon_meat", () -> new Item(FOOD_ITEM_PROPERTIES.food(CCFoodComponents.RAW_DRAGON_MEAT_FOOD_COMPONENT)));
	public static final RegistryObject<Item> COOKED_DRAGON_MEAT = ITEMS.register("cooked_dragon_meat", () -> new Item(FOOD_ITEM_PROPERTIES.food(CCFoodComponents.COOKED_DRAGON_MEAT_FOOD_COMPONENT)));
	public static final RegistryObject<Item> RAW_DRAGON_SAUSAGE = ITEMS.register("raw_dragon_sausage", () -> new Item(FOOD_ITEM_PROPERTIES.food(CCFoodComponents.RAW_DRAGON_SAUSAGE_FOOD_COMPONENT)));
	public static final RegistryObject<Item> COOKED_DRAGON_SAUSAGE = ITEMS.register("dragon_sausage", () -> new Item(FOOD_ITEM_PROPERTIES.food(CCFoodComponents.COOKED_DRAGON_SAUSAGE_FOOD_COMPONENT)));
	public static final RegistryObject<Item> RAW_RICH_DRAGON_SAUSAGE = ITEMS.register("raw_rich_dragon_sausage", () -> new Item(FOOD_ITEM_PROPERTIES.food(CCFoodComponents.RAW_RICH_DRAGON_SAUSAGE_FOOD_COMPONENT)));
	public static final RegistryObject<Item> COOKED_RICH_DRAGON_SAUSAGE = ITEMS.register("rich_dragon_sausage", () -> new Item(FOOD_ITEM_PROPERTIES.food(CCFoodComponents.COOKED_RICH_DRAGON_SAUSAGE_FOOD_COMPONENT)));

	public static final RegistryObject<Item> CUD = ITEMS.register("cud", () -> new Item(FOOD_ITEM_PROPERTIES.food(CCFoodComponents.CUD_FOOD_COMPONENT)));
	public static final RegistryObject<Item> FURNACE_POWER = ITEMS.register("furnace_power", () -> new Item(FOOD_ITEM_PROPERTIES.food(CCFoodComponents.FURNACE_POWER_FOOD_COMPONENT)));
}
