package net.tigereye.chestcavity.registration;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.food.Foods;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.DuochongjianyingGuItem;
import net.tigereye.chestcavity.items.ChestOpener;
import net.tigereye.chestcavity.items.CreeperAppendix;
import net.tigereye.chestcavity.items.VenomGland;

public class CCItems {
  public static final DeferredRegister.Items ITEMS =
      DeferredRegister.createItems(ChestCavity.MODID);

  public static final Item.Properties CHEST_OPENER_PROPERTIES = new Item.Properties().stacksTo(1);
  public static final Item.Properties FOOD_ITEM_PROPERTIES = new Item.Properties().stacksTo(64);

  public static final DeferredItem<Item> CHEST_OPENER =
      ITEMS.register("chest_opener", ChestOpener::new);

  // -- 蛊材
  public static final Item GUZHENREN_JIANJITENG = resolveExternalItem("guzhenren", "jianjiteng");

  // -- 毒道
  public static final Item GUZHENREN_CHOU_PI_GU = resolveExternalItem("guzhenren", "chou_pi_gu");

  // -- 血道
  public static final Item GUZHENREN_TIE_XUE_GU = resolveExternalItem("guzhenren", "tiexuegu");
  public static final Item GUZHENREN_XUE_FEI_GU = resolveExternalItem("guzhenren", "xie_fei_gu");
  public static final Item GUZHENREN_XIE_DI_GU = resolveExternalItem("guzhenren", "xie_di_gu");
  public static final Item GUZHENREN_XIE_YAN_GU = resolveExternalItem("guzhenren", "xie_yan_gu");
  public static final Item GUZHENREN_BING_BU_GU = resolveExternalItem("guzhenren", "bing_bu_gu");
  public static final Item GUZHENREN_BING_JI_GU = resolveExternalItem("guzhenren", "bing_ji_gu");
  public static final Item GUZHENREN_SHUANG_XI_GU =
      resolveExternalItem("guzhenren", "shuang_xi_gu");
  public static final Item GUZHENREN_QING_RE_GU = resolveExternalItem("guzhenren", "qing_re_gu");
  public static final Item GUZHENREN_XIE_WANG_GU = resolveExternalItem("guzhenren", "xie_wang_gu");

  // -- 水道
  public static final Item GUZHENREN_LING_XIAN_GU =
      resolveExternalItem("guzhenren", "ling_xian_gu");

  // -- 剑道
  public static final Item GUZHENREN_JIAN_YING_GU =
      resolveExternalItem("guzhenren", "jian_ying_gu");

  // 多重剑影蛊 (外部物品 - 通过器官系统实现功能)
  // 重构说明: 从物品模式迁移到器官模式 (2025-11-14)
  // 参见: JiandaoOrganRegistry - DuochongjianyingGuOrganBehavior
  public static final Item GUZHENREN_DUOCHONGJIANYING =
      resolveExternalItem("guzhenren", "duochongjianying");

  // -- 骨道
  public static final Item GUZHENREN_GU_QIANG_GU = resolveExternalItem("guzhenren", "gu_qiang_gu");
  public static final Item GUZHENREN_GAN_JIN_GU = resolveExternalItem("guzhenren", "ganjingu");
  public static final Item GUZHENREN_TIE_GU_GU = resolveExternalItem("guzhenren", "tie_gu_gu");
  public static final Item GUZHENREN_JING_TIE_GU_GU =
      resolveExternalItem("guzhenren", "jingtiegugu");

  // -- 模型类 item
  public static final Item GUZHENREN_GU_QIANG = resolveExternalItem("guzhenren", "gu_qiang");
  public static final Item GUZHENREN_XIE_NING_JIAN =
      resolveExternalItem("guzhenren", "xie_ning_jian");

  // -- 未炼化 蛊虫
  public static final Item GUZHENREN_WEI_LIAN_HUA_JIAN_XIA_GU =
      resolveExternalItem("guzhenren", "weilianhuajianxiagu");
  public static final Item GUZHENREN_WEI_LIAN_HUA_JIAN_ZHI_GU_3 =
      resolveExternalItem("guzhenren", "wei_lian_hua_jian_zhi_gu_3");
  public static final Item GUZHENREN_WEI_LIAN_HUA_JIN_WEN_JIAN_XIA_GU =
      resolveExternalItem("guzhenren", "weilianhuajinwenjianxiagu");
  public static final Item GUZHENREN_WEI_LIAN_HUA_JIN_HEN_GU =
      resolveExternalItem("guzhenren", "weilianhuajinhengu");
  public static final Item GUZHENREN_WEI_LIAN_HUA_JIAN_MAI_GU =
      resolveExternalItem("guzhenren", "weilianhuajianmaigu");

  // 待移除 - 不应该在items中
  private static final Item[] GUZHENREN_JIANDAO_BONUS_ITEMS =
      new Item[] {
        GUZHENREN_WEI_LIAN_HUA_JIAN_XIA_GU,
        GUZHENREN_WEI_LIAN_HUA_JIAN_ZHI_GU_3,
        GUZHENREN_WEI_LIAN_HUA_JIN_WEN_JIAN_XIA_GU,
        GUZHENREN_WEI_LIAN_HUA_JIN_HEN_GU,
        GUZHENREN_WEI_LIAN_HUA_JIAN_MAI_GU
      };

  // 待移除
  public static final Item GUZHENREN_QING_LAN_PO_GU_JIAN =
      resolveExternalItem("guzhenren", "qinglanpogujian");

  // 待移除 - 不应该在items中
  public static Item pickRandomGuzhenrenJiandaoBonus(RandomSource random) {
    if (random == null || GUZHENREN_JIANDAO_BONUS_ITEMS.length == 0) {
      return GUZHENREN_WEI_LIAN_HUA_JIAN_MAI_GU;
    }
    int index = random.nextInt(GUZHENREN_JIANDAO_BONUS_ITEMS.length);
    Item selected = GUZHENREN_JIANDAO_BONUS_ITEMS[index];
    return selected == Items.AIR ? GUZHENREN_WEI_LIAN_HUA_JIAN_MAI_GU : selected;
  }

  public static final DeferredItem<Item> WOODEN_CLEAVER =
      ITEMS.register(
          "wooden_cleaver",
          () ->
              new SwordItem(
                  Tiers.WOOD,
                  new Item.Properties()
                      .attributes(SwordItem.createAttributes(Tiers.WOOD, 6.0F, -3.2F))));
  public static final DeferredItem<Item> GOLD_CLEAVER =
      ITEMS.register(
          "gold_cleaver",
          () ->
              new SwordItem(
                  Tiers.GOLD,
                  new Item.Properties()
                      .attributes(SwordItem.createAttributes(Tiers.GOLD, 6.0F, -3.0F))));
  public static final DeferredItem<Item> STONE_CLEAVER =
      ITEMS.register(
          "stone_cleaver",
          () ->
              new SwordItem(
                  Tiers.STONE,
                  new Item.Properties()
                      .attributes(SwordItem.createAttributes(Tiers.STONE, 7.0F, -3.2F))));
  public static final DeferredItem<Item> IRON_CLEAVER =
      ITEMS.register(
          "iron_cleaver",
          () ->
              new SwordItem(
                  Tiers.IRON,
                  new Item.Properties()
                      .attributes(SwordItem.createAttributes(Tiers.IRON, 6.0F, -3.1F))));
  public static final DeferredItem<Item> DIAMOND_CLEAVER =
      ITEMS.register(
          "diamond_cleaver",
          () ->
              new SwordItem(
                  Tiers.DIAMOND,
                  new Item.Properties()
                      .attributes(SwordItem.createAttributes(Tiers.DIAMOND, 5.0F, -3.0F))));
  public static final DeferredItem<Item> NETHERITE_CLEAVER =
      ITEMS.register(
          "netherite_cleaver",
          () ->
              new SwordItem(
                  Tiers.NETHERITE,
                  new Item.Properties()
                      .fireResistant()
                      .attributes(SwordItem.createAttributes(Tiers.NETHERITE, 5.0F, -3.0F))));

  public static final DeferredItem<Item> HUMAN_APPENDIX =
      ITEMS.register(
          "appendix",
          () ->
              new Item(
                  new Item.Properties()
                      .stacksTo(1)
                      .food(CCFoodComponents.RAW_HUMAN_ORGAN_MEAT_FOOD_COMPONENT)));
  public static final DeferredItem<Item> HUMAN_HEART =
      ITEMS.register(
          "heart",
          () ->
              new Item(
                  new Item.Properties()
                      .stacksTo(1)
                      .food(CCFoodComponents.RAW_HUMAN_ORGAN_MEAT_FOOD_COMPONENT)));
  public static final DeferredItem<Item> HUMAN_INTESTINE =
      ITEMS.register(
          "intestine",
          () ->
              new Item(
                  new Item.Properties()
                      .stacksTo(1)
                      .food(CCFoodComponents.RAW_HUMAN_ORGAN_MEAT_FOOD_COMPONENT)));
  public static final DeferredItem<Item> HUMAN_KIDNEY =
      ITEMS.register(
          "kidney",
          () ->
              new Item(
                  new Item.Properties()
                      .stacksTo(1)
                      .food(CCFoodComponents.RAW_HUMAN_ORGAN_MEAT_FOOD_COMPONENT)));
  public static final DeferredItem<Item> HUMAN_LIVER =
      ITEMS.register(
          "liver",
          () ->
              new Item(
                  new Item.Properties()
                      .stacksTo(1)
                      .food(CCFoodComponents.RAW_HUMAN_ORGAN_MEAT_FOOD_COMPONENT)));
  public static final DeferredItem<Item> HUMAN_LUNG =
      ITEMS.register(
          "lung",
          () ->
              new Item(
                  new Item.Properties()
                      .stacksTo(1)
                      .food(CCFoodComponents.RAW_HUMAN_ORGAN_MEAT_FOOD_COMPONENT)));
  public static final DeferredItem<Item> HUMAN_MUSCLE =
      ITEMS.register(
          "muscle",
          () ->
              new Item(
                  new Item.Properties()
                      .stacksTo(16)
                      .food(CCFoodComponents.HUMAN_MUSCLE_FOOD_COMPONENT)));
  public static final DeferredItem<Item> HUMAN_RIB =
      ITEMS.register("rib", () -> new Item(new Item.Properties().stacksTo(4)));
  public static final DeferredItem<Item> HUMAN_SPINE =
      ITEMS.register("spine", () -> new Item(new Item.Properties().stacksTo(1)));
  public static final DeferredItem<Item> HUMAN_SPLEEN =
      ITEMS.register(
          "spleen",
          () ->
              new Item(
                  new Item.Properties()
                      .stacksTo(1)
                      .food(CCFoodComponents.RAW_HUMAN_ORGAN_MEAT_FOOD_COMPONENT)));
  public static final DeferredItem<Item> HUMAN_STOMACH =
      ITEMS.register(
          "stomach",
          () ->
              new Item(
                  new Item.Properties()
                      .stacksTo(1)
                      .food(CCFoodComponents.RAW_HUMAN_ORGAN_MEAT_FOOD_COMPONENT)));

  public static final DeferredItem<Item> ROTTEN_APPENDIX =
      ITEMS.register(
          "rotten_appendix",
          () -> new Item(new Item.Properties().stacksTo(1).food(Foods.ROTTEN_FLESH)));
  public static final DeferredItem<Item> ROTTEN_HEART =
      ITEMS.register(
          "rotten_heart",
          () -> new Item(new Item.Properties().stacksTo(1).food(Foods.ROTTEN_FLESH)));
  public static final DeferredItem<Item> ROTTEN_INTESTINE =
      ITEMS.register(
          "rotten_intestine",
          () -> new Item(new Item.Properties().stacksTo(1).food(Foods.ROTTEN_FLESH)));
  public static final DeferredItem<Item> ROTTEN_KIDNEY =
      ITEMS.register(
          "rotten_kidney",
          () -> new Item(new Item.Properties().stacksTo(1).food(Foods.ROTTEN_FLESH)));
  public static final DeferredItem<Item> ROTTEN_LIVER =
      ITEMS.register(
          "rotten_liver",
          () -> new Item(new Item.Properties().stacksTo(1).food(Foods.ROTTEN_FLESH)));
  public static final DeferredItem<Item> ROTTEN_LUNG =
      ITEMS.register(
          "rotten_lung",
          () -> new Item(new Item.Properties().stacksTo(1).food(Foods.ROTTEN_FLESH)));
  public static final DeferredItem<Item> ROTTEN_MUSCLE =
      ITEMS.register(
          "rotten_muscle",
          () ->
              new Item(
                  new Item.Properties()
                      .stacksTo(16)
                      .food(CCFoodComponents.ROTTEN_MUSCLE_FOOD_COMPONENT)));
  public static final DeferredItem<Item> ROTTEN_RIB =
      ITEMS.register("rotten_rib", () -> new Item(new Item.Properties().stacksTo(4)));
  public static final DeferredItem<Item> ROTTEN_SPINE =
      ITEMS.register("rotten_spine", () -> new Item(new Item.Properties().stacksTo(1)));
  public static final DeferredItem<Item> ROTTEN_SPLEEN =
      ITEMS.register(
          "rotten_spleen",
          () -> new Item(new Item.Properties().stacksTo(1).food(Foods.ROTTEN_FLESH)));
  public static final DeferredItem<Item> ROTTEN_STOMACH =
      ITEMS.register(
          "rotten_stomach",
          () -> new Item(new Item.Properties().stacksTo(1).food(Foods.ROTTEN_FLESH)));
  public static final DeferredItem<Item> WITHERED_RIB =
      ITEMS.register("withered_rib", () -> new Item(new Item.Properties().stacksTo(4)));
  public static final DeferredItem<Item> WITHERED_SPINE =
      ITEMS.register("withered_spine", () -> new Item(new Item.Properties().stacksTo(1)));
  public static final DeferredItem<Item> WRITHING_SOULSAND =
      ITEMS.register("writhing_soulsand", () -> new Item(new Item.Properties().stacksTo(16)));

  public static final DeferredItem<Item> ANIMAL_APPENDIX =
      ITEMS.register(
          "animal_appendix",
          () ->
              new Item(
                  new Item.Properties()
                      .stacksTo(1)
                      .food(CCFoodComponents.RAW_ORGAN_MEAT_FOOD_COMPONENT)));
  public static final DeferredItem<Item> ANIMAL_HEART =
      ITEMS.register(
          "animal_heart",
          () ->
              new Item(
                  new Item.Properties()
                      .stacksTo(1)
                      .food(CCFoodComponents.RAW_ORGAN_MEAT_FOOD_COMPONENT)));
  public static final DeferredItem<Item> ANIMAL_INTESTINE =
      ITEMS.register(
          "animal_intestine",
          () ->
              new Item(
                  new Item.Properties()
                      .stacksTo(1)
                      .food(CCFoodComponents.RAW_ORGAN_MEAT_FOOD_COMPONENT)));
  public static final DeferredItem<Item> ANIMAL_KIDNEY =
      ITEMS.register(
          "animal_kidney",
          () ->
              new Item(
                  new Item.Properties()
                      .stacksTo(1)
                      .food(CCFoodComponents.RAW_ORGAN_MEAT_FOOD_COMPONENT)));
  public static final DeferredItem<Item> ANIMAL_LIVER =
      ITEMS.register(
          "animal_liver",
          () ->
              new Item(
                  new Item.Properties()
                      .stacksTo(1)
                      .food(CCFoodComponents.RAW_ORGAN_MEAT_FOOD_COMPONENT)));
  public static final DeferredItem<Item> ANIMAL_LUNG =
      ITEMS.register(
          "animal_lung",
          () ->
              new Item(
                  new Item.Properties()
                      .stacksTo(1)
                      .food(CCFoodComponents.RAW_ORGAN_MEAT_FOOD_COMPONENT)));
  public static final DeferredItem<Item> ANIMAL_MUSCLE =
      ITEMS.register(
          "animal_muscle",
          () ->
              new Item(
                  new Item.Properties()
                      .stacksTo(16)
                      .food(CCFoodComponents.ANIMAL_MUSCLE_FOOD_COMPONENT)));
  public static final DeferredItem<Item> ANIMAL_RIB =
      ITEMS.register("animal_rib", () -> new Item(new Item.Properties().stacksTo(4)));
  public static final DeferredItem<Item> ANIMAL_SPINE =
      ITEMS.register("animal_spine", () -> new Item(new Item.Properties().stacksTo(1)));
  public static final DeferredItem<Item> ANIMAL_SPLEEN =
      ITEMS.register(
          "animal_spleen",
          () ->
              new Item(
                  new Item.Properties()
                      .stacksTo(1)
                      .food(CCFoodComponents.RAW_ORGAN_MEAT_FOOD_COMPONENT)));
  public static final DeferredItem<Item> ANIMAL_STOMACH =
      ITEMS.register(
          "animal_stomach",
          () ->
              new Item(
                  new Item.Properties()
                      .stacksTo(1)
                      .food(CCFoodComponents.RAW_ORGAN_MEAT_FOOD_COMPONENT)));
  public static final DeferredItem<Item> AQUATIC_MUSCLE =
      ITEMS.register(
          "aquatic_muscle",
          () ->
              new Item(
                  new Item.Properties()
                      .stacksTo(16)
                      .food(CCFoodComponents.ANIMAL_MUSCLE_FOOD_COMPONENT)));
  public static final DeferredItem<Item> FISH_MUSCLE =
      ITEMS.register(
          "fish_muscle",
          () ->
              new Item(
                  new Item.Properties()
                      .stacksTo(16)
                      .food(CCFoodComponents.ANIMAL_MUSCLE_FOOD_COMPONENT)));
  public static final DeferredItem<Item> GILLS =
      ITEMS.register(
          "gills",
          () ->
              new Item(
                  new Item.Properties()
                      .stacksTo(1)
                      .food(CCFoodComponents.RAW_ORGAN_MEAT_FOOD_COMPONENT)));
  public static final DeferredItem<Item> LLAMA_LUNG =
      ITEMS.register(
          "llama_lung",
          () ->
              new Item(
                  new Item.Properties()
                      .stacksTo(1)
                      .food(CCFoodComponents.RAW_ORGAN_MEAT_FOOD_COMPONENT)));
  public static final DeferredItem<Item> CARNIVORE_STOMACH =
      ITEMS.register(
          "carnivore_stomach",
          () ->
              new Item(
                  new Item.Properties()
                      .stacksTo(1)
                      .food(CCFoodComponents.RAW_ORGAN_MEAT_FOOD_COMPONENT)));
  public static final DeferredItem<Item> CARNIVORE_INTESTINE =
      ITEMS.register(
          "carnivore_intestine",
          () ->
              new Item(
                  new Item.Properties()
                      .stacksTo(1)
                      .food(CCFoodComponents.RAW_ORGAN_MEAT_FOOD_COMPONENT)));
  public static final DeferredItem<Item> HERBIVORE_RUMEN =
      ITEMS.register(
          "herbivore_rumen",
          () ->
              new Item(
                  new Item.Properties()
                      .stacksTo(1)
                      .food(CCFoodComponents.RAW_ORGAN_MEAT_FOOD_COMPONENT)));
  public static final DeferredItem<Item> HERBIVORE_STOMACH =
      ITEMS.register(
          "herbivore_stomach",
          () ->
              new Item(
                  new Item.Properties()
                      .stacksTo(1)
                      .food(CCFoodComponents.RAW_ORGAN_MEAT_FOOD_COMPONENT)));
  public static final DeferredItem<Item> HERBIVORE_INTESTINE =
      ITEMS.register(
          "herbivore_intestine",
          () ->
              new Item(
                  new Item.Properties()
                      .stacksTo(1)
                      .food(CCFoodComponents.RAW_ORGAN_MEAT_FOOD_COMPONENT)));
  public static final DeferredItem<Item> BRUTISH_MUSCLE =
      ITEMS.register(
          "brutish_muscle",
          () ->
              new Item(
                  new Item.Properties()
                      .stacksTo(16)
                      .food(CCFoodComponents.ANIMAL_MUSCLE_FOOD_COMPONENT)));
  public static final DeferredItem<Item> SWIFT_MUSCLE =
      ITEMS.register(
          "swift_muscle",
          () ->
              new Item(
                  new Item.Properties()
                      .stacksTo(16)
                      .food(CCFoodComponents.ANIMAL_MUSCLE_FOOD_COMPONENT)));
  public static final DeferredItem<Item> SPRINGY_MUSCLE =
      ITEMS.register(
          "springy_muscle",
          () ->
              new Item(
                  new Item.Properties()
                      .stacksTo(16)
                      .food(CCFoodComponents.ANIMAL_MUSCLE_FOOD_COMPONENT)));

  public static final DeferredItem<Item> FIREPROOF_APPENDIX =
      ITEMS.register(
          "fireproof_appendix",
          () ->
              new Item(
                  new Item.Properties()
                      .stacksTo(1)
                      .food(CCFoodComponents.RAW_ORGAN_MEAT_FOOD_COMPONENT)));
  public static final DeferredItem<Item> FIREPROOF_HEART =
      ITEMS.register(
          "fireproof_heart",
          () ->
              new Item(
                  new Item.Properties()
                      .stacksTo(1)
                      .food(CCFoodComponents.RAW_ORGAN_MEAT_FOOD_COMPONENT)));
  public static final DeferredItem<Item> FIREPROOF_INTESTINE =
      ITEMS.register(
          "fireproof_intestine",
          () ->
              new Item(
                  new Item.Properties()
                      .stacksTo(1)
                      .food(CCFoodComponents.RAW_ORGAN_MEAT_FOOD_COMPONENT)));
  public static final DeferredItem<Item> FIREPROOF_KIDNEY =
      ITEMS.register(
          "fireproof_kidney",
          () ->
              new Item(
                  new Item.Properties()
                      .stacksTo(1)
                      .food(CCFoodComponents.RAW_ORGAN_MEAT_FOOD_COMPONENT)));
  public static final DeferredItem<Item> FIREPROOF_LIVER =
      ITEMS.register(
          "fireproof_liver",
          () ->
              new Item(
                  new Item.Properties()
                      .stacksTo(1)
                      .food(CCFoodComponents.RAW_ORGAN_MEAT_FOOD_COMPONENT)));
  public static final DeferredItem<Item> FIREPROOF_LUNG =
      ITEMS.register(
          "fireproof_lung",
          () ->
              new Item(
                  new Item.Properties()
                      .stacksTo(1)
                      .food(CCFoodComponents.RAW_ORGAN_MEAT_FOOD_COMPONENT)));
  public static final DeferredItem<Item> FIREPROOF_MUSCLE =
      ITEMS.register(
          "fireproof_muscle",
          () ->
              new Item(
                  new Item.Properties()
                      .stacksTo(16)
                      .food(CCFoodComponents.ANIMAL_MUSCLE_FOOD_COMPONENT)));
  public static final DeferredItem<Item> FIREPROOF_RIB =
      ITEMS.register("fireproof_rib", () -> new Item(new Item.Properties().stacksTo(4)));
  public static final DeferredItem<Item> FIREPROOF_SPINE =
      ITEMS.register("fireproof_spine", () -> new Item(new Item.Properties().stacksTo(1)));
  public static final DeferredItem<Item> FIREPROOF_SPLEEN =
      ITEMS.register(
          "fireproof_spleen",
          () ->
              new Item(
                  new Item.Properties()
                      .stacksTo(1)
                      .food(CCFoodComponents.RAW_ORGAN_MEAT_FOOD_COMPONENT)));
  public static final DeferredItem<Item> FIREPROOF_STOMACH =
      ITEMS.register(
          "fireproof_stomach",
          () ->
              new Item(
                  new Item.Properties()
                      .stacksTo(1)
                      .food(CCFoodComponents.RAW_ORGAN_MEAT_FOOD_COMPONENT)));

  public static final DeferredItem<Item> SMALL_ANIMAL_APPENDIX =
      ITEMS.register(
          "small_animal_appendix",
          () ->
              new Item(
                  new Item.Properties()
                      .stacksTo(1)
                      .food(CCFoodComponents.SMALL_ANIMAL_MUSCLE_FOOD_COMPONENT)));
  public static final DeferredItem<Item> SMALL_ANIMAL_HEART =
      ITEMS.register(
          "small_animal_heart",
          () ->
              new Item(
                  new Item.Properties()
                      .stacksTo(1)
                      .food(CCFoodComponents.SMALL_ANIMAL_MUSCLE_FOOD_COMPONENT)));
  public static final DeferredItem<Item> SMALL_ANIMAL_INTESTINE =
      ITEMS.register(
          "small_animal_intestine",
          () ->
              new Item(
                  new Item.Properties()
                      .stacksTo(1)
                      .food(CCFoodComponents.SMALL_ANIMAL_MUSCLE_FOOD_COMPONENT)));
  public static final DeferredItem<Item> SMALL_ANIMAL_KIDNEY =
      ITEMS.register(
          "small_animal_kidney",
          () ->
              new Item(
                  new Item.Properties()
                      .stacksTo(1)
                      .food(CCFoodComponents.SMALL_ANIMAL_MUSCLE_FOOD_COMPONENT)));
  public static final DeferredItem<Item> SMALL_ANIMAL_LIVER =
      ITEMS.register(
          "small_animal_liver",
          () ->
              new Item(
                  new Item.Properties()
                      .stacksTo(1)
                      .food(CCFoodComponents.SMALL_ANIMAL_MUSCLE_FOOD_COMPONENT)));
  public static final DeferredItem<Item> SMALL_ANIMAL_LUNG =
      ITEMS.register(
          "small_animal_lung",
          () ->
              new Item(
                  new Item.Properties()
                      .stacksTo(1)
                      .food(CCFoodComponents.SMALL_ANIMAL_MUSCLE_FOOD_COMPONENT)));
  public static final DeferredItem<Item> SMALL_ANIMAL_MUSCLE =
      ITEMS.register(
          "small_animal_muscle",
          () ->
              new Item(
                  new Item.Properties()
                      .stacksTo(16)
                      .food(CCFoodComponents.SMALL_ANIMAL_MUSCLE_FOOD_COMPONENT)));
  public static final DeferredItem<Item> SMALL_ANIMAL_RIB =
      ITEMS.register("small_animal_rib", () -> new Item(new Item.Properties().stacksTo(4)));
  public static final DeferredItem<Item> SMALL_ANIMAL_SPINE =
      ITEMS.register("small_animal_spine", () -> new Item(new Item.Properties().stacksTo(1)));
  public static final DeferredItem<Item> SMALL_ANIMAL_SPLEEN =
      ITEMS.register(
          "small_animal_spleen",
          () ->
              new Item(
                  new Item.Properties()
                      .stacksTo(1)
                      .food(CCFoodComponents.SMALL_ANIMAL_MUSCLE_FOOD_COMPONENT)));
  public static final DeferredItem<Item> SMALL_ANIMAL_STOMACH =
      ITEMS.register(
          "small_animal_stomach",
          () ->
              new Item(
                  new Item.Properties()
                      .stacksTo(1)
                      .food(CCFoodComponents.SMALL_ANIMAL_MUSCLE_FOOD_COMPONENT)));
  public static final DeferredItem<Item> RABBIT_HEART =
      ITEMS.register(
          "rabbit_heart",
          () ->
              new Item(
                  new Item.Properties()
                      .stacksTo(1)
                      .food(CCFoodComponents.SMALL_ANIMAL_MUSCLE_FOOD_COMPONENT)));
  public static final DeferredItem<Item> SMALL_AQUATIC_MUSCLE =
      ITEMS.register(
          "small_aquatic_muscle",
          () ->
              new Item(
                  new Item.Properties()
                      .stacksTo(16)
                      .food(CCFoodComponents.SMALL_ANIMAL_MUSCLE_FOOD_COMPONENT)));
  public static final DeferredItem<Item> SMALL_FISH_MUSCLE =
      ITEMS.register(
          "small_fish_muscle",
          () ->
              new Item(
                  new Item.Properties()
                      .stacksTo(16)
                      .food(CCFoodComponents.SMALL_ANIMAL_MUSCLE_FOOD_COMPONENT)));
  public static final DeferredItem<Item> SMALL_SPRINGY_MUSCLE =
      ITEMS.register(
          "small_springy_muscle",
          () ->
              new Item(
                  new Item.Properties()
                      .stacksTo(16)
                      .food(CCFoodComponents.SMALL_ANIMAL_MUSCLE_FOOD_COMPONENT)));
  public static final DeferredItem<Item> SMALL_GILLS =
      ITEMS.register(
          "small_gills",
          () ->
              new Item(
                  new Item.Properties()
                      .stacksTo(1)
                      .food(CCFoodComponents.SMALL_ANIMAL_MUSCLE_FOOD_COMPONENT)));
  public static final DeferredItem<Item> SMALL_CARNIVORE_STOMACH =
      ITEMS.register(
          "small_carnivore_stomach",
          () ->
              new Item(
                  new Item.Properties()
                      .stacksTo(1)
                      .food(CCFoodComponents.SMALL_ANIMAL_MUSCLE_FOOD_COMPONENT)));
  public static final DeferredItem<Item> SMALL_CARNIVORE_INTESTINE =
      ITEMS.register(
          "small_carnivore_intestine",
          () ->
              new Item(
                  new Item.Properties()
                      .stacksTo(1)
                      .food(CCFoodComponents.SMALL_ANIMAL_MUSCLE_FOOD_COMPONENT)));
  public static final DeferredItem<Item> SMALL_HERBIVORE_STOMACH =
      ITEMS.register(
          "small_herbivore_stomach",
          () ->
              new Item(
                  new Item.Properties()
                      .stacksTo(1)
                      .food(CCFoodComponents.SMALL_ANIMAL_MUSCLE_FOOD_COMPONENT)));
  public static final DeferredItem<Item> SMALL_HERBIVORE_INTESTINE =
      ITEMS.register(
          "small_herbivore_intestine",
          () ->
              new Item(
                  new Item.Properties()
                      .stacksTo(1)
                      .food(CCFoodComponents.SMALL_ANIMAL_MUSCLE_FOOD_COMPONENT)));

  public static final DeferredItem<Item> INSECT_HEART =
      ITEMS.register(
          "insect_heart",
          () ->
              new Item(
                  new Item.Properties()
                      .stacksTo(1)
                      .food(CCFoodComponents.RAW_TOXIC_ORGAN_MEAT_FOOD_COMPONENT)));
  public static final DeferredItem<Item> INSECT_INTESTINE =
      ITEMS.register(
          "insect_intestine",
          () ->
              new Item(
                  new Item.Properties()
                      .stacksTo(1)
                      .food(CCFoodComponents.RAW_TOXIC_ORGAN_MEAT_FOOD_COMPONENT)));
  public static final DeferredItem<Item> INSECT_LUNG =
      ITEMS.register(
          "insect_lung",
          () ->
              new Item(
                  new Item.Properties()
                      .stacksTo(1)
                      .food(CCFoodComponents.RAW_TOXIC_ORGAN_MEAT_FOOD_COMPONENT)));
  public static final DeferredItem<Item> INSECT_MUSCLE =
      ITEMS.register(
          "insect_muscle",
          () ->
              new Item(
                  new Item.Properties()
                      .stacksTo(16)
                      .food(CCFoodComponents.INSECT_MUSCLE_FOOD_COMPONENT)));
  public static final DeferredItem<Item> INSECT_STOMACH =
      ITEMS.register(
          "insect_stomach",
          () ->
              new Item(
                  new Item.Properties()
                      .stacksTo(1)
                      .food(CCFoodComponents.RAW_TOXIC_ORGAN_MEAT_FOOD_COMPONENT)));
  public static final DeferredItem<Item> INSECT_CAECA =
      ITEMS.register(
          "insect_caeca",
          () ->
              new Item(
                  new Item.Properties()
                      .stacksTo(1)
                      .food(CCFoodComponents.RAW_TOXIC_ORGAN_MEAT_FOOD_COMPONENT)));
  public static final DeferredItem<Item> SILK_GLAND =
      ITEMS.register(
          "silk_gland",
          () ->
              new Item(
                  new Item.Properties()
                      .stacksTo(1)
                      .food(CCFoodComponents.RAW_TOXIC_ORGAN_MEAT_FOOD_COMPONENT)));
  public static final DeferredItem<Item> VENOM_GLAND =
      ITEMS.register(
          "venom_gland", () -> new VenomGland()); // .setOrganQuality(CCOrganScores.VENOMOUS,1f));

  public static final DeferredItem<Item> ENDER_APPENDIX =
      ITEMS.register(
          "ender_appendix",
          () ->
              new Item(
                  new Item.Properties()
                      .stacksTo(1)
                      .food(CCFoodComponents.RAW_ALIEN_ORGAN_MEAT_FOOD_COMPONENT)));
  public static final DeferredItem<Item> ENDER_HEART =
      ITEMS.register(
          "ender_heart",
          () ->
              new Item(
                  new Item.Properties()
                      .stacksTo(1)
                      .food(CCFoodComponents.RAW_ALIEN_ORGAN_MEAT_FOOD_COMPONENT)));
  public static final DeferredItem<Item> ENDER_INTESTINE =
      ITEMS.register(
          "ender_intestine",
          () ->
              new Item(
                  new Item.Properties()
                      .stacksTo(1)
                      .food(CCFoodComponents.RAW_ALIEN_ORGAN_MEAT_FOOD_COMPONENT)));
  public static final DeferredItem<Item> ENDER_KIDNEY =
      ITEMS.register(
          "ender_kidney",
          () ->
              new Item(
                  new Item.Properties()
                      .stacksTo(1)
                      .food(CCFoodComponents.RAW_ALIEN_ORGAN_MEAT_FOOD_COMPONENT)));
  public static final DeferredItem<Item> ENDER_LIVER =
      ITEMS.register(
          "ender_liver",
          () ->
              new Item(
                  new Item.Properties()
                      .stacksTo(1)
                      .food(CCFoodComponents.RAW_ALIEN_ORGAN_MEAT_FOOD_COMPONENT)));
  public static final DeferredItem<Item> ENDER_LUNG =
      ITEMS.register(
          "ender_lung",
          () ->
              new Item(
                  new Item.Properties()
                      .stacksTo(1)
                      .food(CCFoodComponents.RAW_ALIEN_ORGAN_MEAT_FOOD_COMPONENT)));
  public static final DeferredItem<Item> ENDER_MUSCLE =
      ITEMS.register(
          "ender_muscle",
          () ->
              new Item(
                  new Item.Properties()
                      .stacksTo(16)
                      .food(CCFoodComponents.ALIEN_MUSCLE_FOOD_COMPONENT)));
  public static final DeferredItem<Item> ENDER_RIB =
      ITEMS.register("ender_rib", () -> new Item(new Item.Properties().stacksTo(4)));
  public static final DeferredItem<Item> ENDER_SPINE =
      ITEMS.register("ender_spine", () -> new Item(new Item.Properties().stacksTo(1)));
  public static final DeferredItem<Item> ENDER_SPLEEN =
      ITEMS.register(
          "ender_spleen",
          () ->
              new Item(
                  new Item.Properties()
                      .stacksTo(1)
                      .food(CCFoodComponents.RAW_ALIEN_ORGAN_MEAT_FOOD_COMPONENT)));
  public static final DeferredItem<Item> ENDER_STOMACH =
      ITEMS.register(
          "ender_stomach",
          () ->
              new Item(
                  new Item.Properties()
                      .stacksTo(1)
                      .food(CCFoodComponents.RAW_ALIEN_ORGAN_MEAT_FOOD_COMPONENT)));

  public static final DeferredItem<Item> DRAGON_APPENDIX =
      ITEMS.register(
          "dragon_appendix",
          () ->
              new Item(
                  new Item.Properties()
                      .stacksTo(1)
                      .food(CCFoodComponents.RAW_DRAGON_ORGAN_MEAT_FOOD_COMPONENT)));
  public static final DeferredItem<Item> DRAGON_HEART =
      ITEMS.register(
          "dragon_heart",
          () ->
              new Item(
                  new Item.Properties()
                      .stacksTo(1)
                      .food(CCFoodComponents.DRAGON_HEART_FOOD_COMPONENT)));
  public static final DeferredItem<Item> DRAGON_KIDNEY =
      ITEMS.register(
          "dragon_kidney",
          () ->
              new Item(
                  new Item.Properties()
                      .stacksTo(1)
                      .food(CCFoodComponents.RAW_DRAGON_ORGAN_MEAT_FOOD_COMPONENT)));
  public static final DeferredItem<Item> DRAGON_LIVER =
      ITEMS.register(
          "dragon_liver",
          () ->
              new Item(
                  new Item.Properties()
                      .stacksTo(1)
                      .food(CCFoodComponents.RAW_DRAGON_ORGAN_MEAT_FOOD_COMPONENT)));
  public static final DeferredItem<Item> DRAGON_LUNG =
      ITEMS.register(
          "dragon_lung",
          () ->
              new Item(
                  new Item.Properties()
                      .stacksTo(1)
                      .food(CCFoodComponents.RAW_DRAGON_ORGAN_MEAT_FOOD_COMPONENT)));
  public static final DeferredItem<Item> DRAGON_MUSCLE =
      ITEMS.register(
          "dragon_muscle",
          () ->
              new Item(
                  new Item.Properties()
                      .stacksTo(16)
                      .food(CCFoodComponents.DRAGON_MUSCLE_FOOD_COMPONENT)));
  public static final DeferredItem<Item> DRAGON_RIB =
      ITEMS.register("dragon_rib", () -> new Item(new Item.Properties().stacksTo(4)));
  // TODO: Destructive DeferredItem<Item>isions
  public static final DeferredItem<Item> DRAGON_SPINE =
      ITEMS.register("dragon_spine", () -> new Item(new Item.Properties().stacksTo(1)));
  public static final DeferredItem<Item> DRAGON_SPLEEN =
      ITEMS.register(
          "dragon_spleen",
          () ->
              new Item(
                  new Item.Properties()
                      .stacksTo(1)
                      .food(CCFoodComponents.RAW_DRAGON_ORGAN_MEAT_FOOD_COMPONENT)));
  public static final DeferredItem<Item> MANA_REACTOR =
      ITEMS.register(
          "mana_reactor",
          () ->
              new Item(
                  new Item.Properties()
                      .stacksTo(1)
                      .food(CCFoodComponents.RAW_DRAGON_ORGAN_MEAT_FOOD_COMPONENT)));

  public static final DeferredItem<Item> ACTIVE_BLAZE_ROD =
      ITEMS.register("active_blaze_rod", () -> new Item(new Item.Properties().stacksTo(3)));
  public static final DeferredItem<Item> BLAZE_SHELL =
      ITEMS.register("blaze_shell", () -> new Item(new Item.Properties().stacksTo(4)));
  public static final DeferredItem<Item> BLAZE_CORE =
      ITEMS.register("blaze_core", () -> new Item(new Item.Properties().stacksTo(1)));

  public static final DeferredItem<Item> GAS_BLADDER =
      ITEMS.register("gas_bladder", () -> new Item(new Item.Properties().stacksTo(1)));
  public static final DeferredItem<Item> VOLATILE_STOMACH =
      ITEMS.register("volatile_stomach", () -> new Item(new Item.Properties().stacksTo(1)));

  public static final DeferredItem<Item> GOLEM_CABLE =
      ITEMS.register("golem_cable", () -> new Item(new Item.Properties().stacksTo(1)));
  public static final DeferredItem<Item> GOLEM_PLATING =
      ITEMS.register("golem_plating", () -> new Item(new Item.Properties().stacksTo(4)));
  public static final DeferredItem<Item> GOLEM_CORE =
      ITEMS.register("golem_core", () -> new Item(new Item.Properties().stacksTo(1)));
  public static final DeferredItem<Item> INNER_FURNACE =
      ITEMS.register("inner_furnace", () -> new Item(new Item.Properties().stacksTo(1)));
  public static final DeferredItem<Item> PISTON_MUSCLE =
      ITEMS.register("piston_muscle", () -> new Item(new Item.Properties().stacksTo(16)));
  public static final DeferredItem<Item> IRON_SCRAP =
      ITEMS.register("iron_scrap", () -> new Item(new Item.Properties()));

  public static final DeferredItem<Item> SALTWATER_HEART =
      ITEMS.register("saltwater_heart", () -> new Item(new Item.Properties().stacksTo(1)));
  public static final DeferredItem<Item> SALTWATER_LUNG =
      ITEMS.register("saltwater_lung", () -> new Item(new Item.Properties().stacksTo(1)));
  public static final DeferredItem<Item> SALTWATER_MUSCLE =
      ITEMS.register("saltwater_muscle", () -> new Item(new Item.Properties().stacksTo(16)));
  public static final DeferredItem<Item> CREEPER_APPENDIX =
      ITEMS.register("creeper_appendix", () -> new CreeperAppendix());
  public static final DeferredItem<Item> SHIFTING_LEAVES =
      ITEMS.register("shifting_leaves", () -> new Item(new Item.Properties().stacksTo(16)));
  public static final DeferredItem<Item> SHULKER_SPLEEN =
      ITEMS.register("shulker_spleen", () -> new Item(new Item.Properties().stacksTo(1)));

  public static final DeferredItem<Item> SAUSAGE_SKIN =
      ITEMS.register("sausage_skin", () -> new Item(new Item.Properties().stacksTo(64)));
  public static final DeferredItem<Item> MINI_SAUSAGE_SKIN =
      ITEMS.register("mini_sausage_skin", () -> new Item(new Item.Properties().stacksTo(64)));

  public static final DeferredItem<Item> BURNT_MEAT_CHUNK =
      ITEMS.register(
          "burnt_meat_chunk",
          () -> new Item(FOOD_ITEM_PROPERTIES.food(CCFoodComponents.BURNT_MEAT_CHUNK_COMPONENT)));
  public static final DeferredItem<Item> RAW_ORGAN_MEAT =
      ITEMS.register(
          "raw_organ_meat",
          () ->
              new Item(FOOD_ITEM_PROPERTIES.food(CCFoodComponents.RAW_ORGAN_MEAT_FOOD_COMPONENT)));
  public static final DeferredItem<Item> COOKED_ORGAN_MEAT =
      ITEMS.register(
          "cooked_organ_meat",
          () ->
              new Item(
                  FOOD_ITEM_PROPERTIES.food(CCFoodComponents.COOKED_ORGAN_MEAT_FOOD_COMPONENT)));
  public static final DeferredItem<Item> RAW_BUTCHERED_MEAT =
      ITEMS.register(
          "raw_butchered_meat",
          () ->
              new Item(
                  FOOD_ITEM_PROPERTIES.food(CCFoodComponents.RAW_BUTCHERED_MEAT_FOOD_COMPONENT)));
  public static final DeferredItem<Item> COOKED_BUTCHERED_MEAT =
      ITEMS.register(
          "cooked_butchered_meat",
          () ->
              new Item(
                  FOOD_ITEM_PROPERTIES.food(
                      CCFoodComponents.COOKED_BUTCHERED_MEAT_FOOD_COMPONENT)));
  public static final DeferredItem<Item> RAW_SAUSAGE =
      ITEMS.register(
          "raw_sausage",
          () -> new Item(FOOD_ITEM_PROPERTIES.food(CCFoodComponents.RAW_SAUSAGE_FOOD_COMPONENT)));
  public static final DeferredItem<Item> COOKED_SAUSAGE =
      ITEMS.register(
          "sausage",
          () ->
              new Item(FOOD_ITEM_PROPERTIES.food(CCFoodComponents.COOKED_SAUSAGE_FOOD_COMPONENT)));
  public static final DeferredItem<Item> RAW_RICH_SAUSAGE =
      ITEMS.register(
          "raw_rich_sausage",
          () ->
              new Item(
                  FOOD_ITEM_PROPERTIES.food(CCFoodComponents.RAW_RICH_SAUSAGE_FOOD_COMPONENT)));
  public static final DeferredItem<Item> COOKED_RICH_SAUSAGE =
      ITEMS.register(
          "rich_sausage",
          () ->
              new Item(
                  FOOD_ITEM_PROPERTIES.food(CCFoodComponents.COOKED_RICH_SAUSAGE_FOOD_COMPONENT)));
  public static final DeferredItem<Item> RAW_MINI_SAUSAGE =
      ITEMS.register(
          "raw_mini_sausage",
          () ->
              new Item(
                  FOOD_ITEM_PROPERTIES.food(CCFoodComponents.RAW_MINI_SAUSAGE_FOOD_COMPONENT)));
  public static final DeferredItem<Item> COOKED_MINI_SAUSAGE =
      ITEMS.register(
          "mini_sausage",
          () ->
              new Item(
                  FOOD_ITEM_PROPERTIES.food(CCFoodComponents.COOKED_MINI_SAUSAGE_FOOD_COMPONENT)));
  public static final DeferredItem<Item> RAW_RICH_MINI_SAUSAGE =
      ITEMS.register(
          "raw_rich_mini_sausage",
          () ->
              new Item(
                  FOOD_ITEM_PROPERTIES.food(
                      CCFoodComponents.RAW_RICH_MINI_SAUSAGE_FOOD_COMPONENT)));
  public static final DeferredItem<Item> COOKED_RICH_MINI_SAUSAGE =
      ITEMS.register(
          "rich_mini_sausage",
          () ->
              new Item(
                  FOOD_ITEM_PROPERTIES.food(
                      CCFoodComponents.COOKED_RICH_MINI_SAUSAGE_FOOD_COMPONENT)));

  public static final DeferredItem<Item> ROTTEN_SAUSAGE =
      ITEMS.register(
          "rotten_sausage",
          () ->
              new Item(FOOD_ITEM_PROPERTIES.food(CCFoodComponents.ROTTEN_SAUSAGE_FOOD_COMPONENT)));

  public static final DeferredItem<Item> RAW_TOXIC_ORGAN_MEAT =
      ITEMS.register(
          "raw_toxic_organ_meat",
          () ->
              new Item(
                  FOOD_ITEM_PROPERTIES.food(CCFoodComponents.RAW_TOXIC_ORGAN_MEAT_FOOD_COMPONENT)));
  public static final DeferredItem<Item> COOKED_TOXIC_ORGAN_MEAT =
      ITEMS.register(
          "cooked_toxic_organ_meat",
          () ->
              new Item(
                  FOOD_ITEM_PROPERTIES.food(
                      CCFoodComponents.COOKED_TOXIC_ORGAN_MEAT_FOOD_COMPONENT)));
  public static final DeferredItem<Item> RAW_TOXIC_MEAT =
      ITEMS.register(
          "raw_toxic_meat",
          () ->
              new Item(FOOD_ITEM_PROPERTIES.food(CCFoodComponents.RAW_TOXIC_MEAT_FOOD_COMPONENT)));
  public static final DeferredItem<Item> COOKED_TOXIC_MEAT =
      ITEMS.register(
          "cooked_toxic_meat",
          () ->
              new Item(
                  FOOD_ITEM_PROPERTIES.food(CCFoodComponents.COOKED_TOXIC_MEAT_FOOD_COMPONENT)));
  public static final DeferredItem<Item> RAW_TOXIC_SAUSAGE =
      ITEMS.register(
          "raw_toxic_sausage",
          () ->
              new Item(
                  FOOD_ITEM_PROPERTIES.food(CCFoodComponents.RAW_TOXIC_SAUSAGE_FOOD_COMPONENT)));
  public static final DeferredItem<Item> COOKED_TOXIC_SAUSAGE =
      ITEMS.register(
          "toxic_sausage",
          () ->
              new Item(
                  FOOD_ITEM_PROPERTIES.food(CCFoodComponents.COOKED_TOXIC_SAUSAGE_FOOD_COMPONENT)));
  public static final DeferredItem<Item> RAW_RICH_TOXIC_SAUSAGE =
      ITEMS.register(
          "raw_rich_toxic_sausage",
          () ->
              new Item(
                  FOOD_ITEM_PROPERTIES.food(
                      CCFoodComponents.RAW_RICH_TOXIC_SAUSAGE_FOOD_COMPONENT)));
  public static final DeferredItem<Item> COOKED_RICH_TOXIC_SAUSAGE =
      ITEMS.register(
          "rich_toxic_sausage",
          () ->
              new Item(
                  FOOD_ITEM_PROPERTIES.food(
                      CCFoodComponents.COOKED_RICH_TOXIC_SAUSAGE_FOOD_COMPONENT)));

  public static final DeferredItem<Item> RAW_HUMAN_ORGAN_MEAT =
      ITEMS.register(
          "raw_human_organ_meat",
          () ->
              new Item(
                  FOOD_ITEM_PROPERTIES.food(CCFoodComponents.RAW_HUMAN_ORGAN_MEAT_FOOD_COMPONENT)));
  public static final DeferredItem<Item> COOKED_HUMAN_ORGAN_MEAT =
      ITEMS.register(
          "cooked_human_organ_meat",
          () ->
              new Item(
                  FOOD_ITEM_PROPERTIES.food(
                      CCFoodComponents.COOKED_HUMAN_ORGAN_MEAT_FOOD_COMPONENT)));
  public static final DeferredItem<Item> RAW_MAN_MEAT =
      ITEMS.register(
          "raw_man_meat",
          () -> new Item(FOOD_ITEM_PROPERTIES.food(CCFoodComponents.RAW_MAN_MEAT_FOOD_COMPONENT)));
  public static final DeferredItem<Item> COOKED_MAN_MEAT =
      ITEMS.register(
          "cooked_man_meat",
          () ->
              new Item(FOOD_ITEM_PROPERTIES.food(CCFoodComponents.COOKED_MAN_MEAT_FOOD_COMPONENT)));
  public static final DeferredItem<Item> RAW_HUMAN_SAUSAGE =
      ITEMS.register(
          "raw_human_sausage",
          () ->
              new Item(
                  FOOD_ITEM_PROPERTIES.food(CCFoodComponents.RAW_HUMAN_SAUSAGE_FOOD_COMPONENT)));
  public static final DeferredItem<Item> COOKED_HUMAN_SAUSAGE =
      ITEMS.register(
          "human_sausage",
          () ->
              new Item(
                  FOOD_ITEM_PROPERTIES.food(CCFoodComponents.COOKED_HUMAN_SAUSAGE_FOOD_COMPONENT)));
  public static final DeferredItem<Item> RAW_RICH_HUMAN_SAUSAGE =
      ITEMS.register(
          "raw_rich_human_sausage",
          () ->
              new Item(
                  FOOD_ITEM_PROPERTIES.food(
                      CCFoodComponents.RAW_RICH_HUMAN_SAUSAGE_FOOD_COMPONENT)));
  public static final DeferredItem<Item> COOKED_RICH_HUMAN_SAUSAGE =
      ITEMS.register(
          "rich_human_sausage",
          () ->
              new Item(
                  FOOD_ITEM_PROPERTIES.food(
                      CCFoodComponents.COOKED_RICH_HUMAN_SAUSAGE_FOOD_COMPONENT)));

  public static final DeferredItem<Item> RAW_ALIEN_ORGAN_MEAT =
      ITEMS.register(
          "raw_alien_organ_meat",
          () ->
              new Item(
                  FOOD_ITEM_PROPERTIES.food(CCFoodComponents.RAW_ALIEN_ORGAN_MEAT_FOOD_COMPONENT)));
  public static final DeferredItem<Item> COOKED_ALIEN_ORGAN_MEAT =
      ITEMS.register(
          "cooked_alien_organ_meat",
          () ->
              new Item(
                  FOOD_ITEM_PROPERTIES.food(
                      CCFoodComponents.COOKED_ALIEN_ORGAN_MEAT_FOOD_COMPONENT)));
  public static final DeferredItem<Item> RAW_ALIEN_MEAT =
      ITEMS.register(
          "raw_alien_meat",
          () ->
              new Item(FOOD_ITEM_PROPERTIES.food(CCFoodComponents.RAW_ALIEN_MEAT_FOOD_COMPONENT)));
  public static final DeferredItem<Item> COOKED_ALIEN_MEAT =
      ITEMS.register(
          "cooked_alien_meat",
          () ->
              new Item(
                  FOOD_ITEM_PROPERTIES.food(CCFoodComponents.COOKED_ALIEN_MEAT_FOOD_COMPONENT)));
  public static final DeferredItem<Item> RAW_ALIEN_SAUSAGE =
      ITEMS.register(
          "raw_alien_sausage",
          () ->
              new Item(
                  FOOD_ITEM_PROPERTIES.food(CCFoodComponents.RAW_ALIEN_SAUSAGE_FOOD_COMPONENT)));
  public static final DeferredItem<Item> COOKED_ALIEN_SAUSAGE =
      ITEMS.register(
          "alien_sausage",
          () ->
              new Item(
                  FOOD_ITEM_PROPERTIES.food(CCFoodComponents.COOKED_ALIEN_SAUSAGE_FOOD_COMPONENT)));
  public static final DeferredItem<Item> RAW_RICH_ALIEN_SAUSAGE =
      ITEMS.register(
          "raw_rich_alien_sausage",
          () ->
              new Item(
                  FOOD_ITEM_PROPERTIES.food(
                      CCFoodComponents.RAW_RICH_ALIEN_SAUSAGE_FOOD_COMPONENT)));
  public static final DeferredItem<Item> COOKED_RICH_ALIEN_SAUSAGE =
      ITEMS.register(
          "rich_alien_sausage",
          () ->
              new Item(
                  FOOD_ITEM_PROPERTIES.food(
                      CCFoodComponents.COOKED_RICH_ALIEN_SAUSAGE_FOOD_COMPONENT)));

  public static final DeferredItem<Item> RAW_DRAGON_ORGAN_MEAT =
      ITEMS.register(
          "raw_dragon_organ_meat",
          () ->
              new Item(
                  FOOD_ITEM_PROPERTIES.food(
                      CCFoodComponents.RAW_DRAGON_ORGAN_MEAT_FOOD_COMPONENT)));
  public static final DeferredItem<Item> COOKED_DRAGON_ORGAN_MEAT =
      ITEMS.register(
          "cooked_dragon_organ_meat",
          () ->
              new Item(
                  FOOD_ITEM_PROPERTIES.food(
                      CCFoodComponents.COOKED_DRAGON_ORGAN_MEAT_FOOD_COMPONENT)));
  public static final DeferredItem<Item> RAW_DRAGON_MEAT =
      ITEMS.register(
          "raw_dragon_meat",
          () ->
              new Item(FOOD_ITEM_PROPERTIES.food(CCFoodComponents.RAW_DRAGON_MEAT_FOOD_COMPONENT)));
  public static final DeferredItem<Item> COOKED_DRAGON_MEAT =
      ITEMS.register(
          "cooked_dragon_meat",
          () ->
              new Item(
                  FOOD_ITEM_PROPERTIES.food(CCFoodComponents.COOKED_DRAGON_MEAT_FOOD_COMPONENT)));
  public static final DeferredItem<Item> RAW_DRAGON_SAUSAGE =
      ITEMS.register(
          "raw_dragon_sausage",
          () ->
              new Item(
                  FOOD_ITEM_PROPERTIES.food(CCFoodComponents.RAW_DRAGON_SAUSAGE_FOOD_COMPONENT)));
  public static final DeferredItem<Item> COOKED_DRAGON_SAUSAGE =
      ITEMS.register(
          "dragon_sausage",
          () ->
              new Item(
                  FOOD_ITEM_PROPERTIES.food(
                      CCFoodComponents.COOKED_DRAGON_SAUSAGE_FOOD_COMPONENT)));
  public static final DeferredItem<Item> RAW_RICH_DRAGON_SAUSAGE =
      ITEMS.register(
          "raw_rich_dragon_sausage",
          () ->
              new Item(
                  FOOD_ITEM_PROPERTIES.food(
                      CCFoodComponents.RAW_RICH_DRAGON_SAUSAGE_FOOD_COMPONENT)));
  public static final DeferredItem<Item> COOKED_RICH_DRAGON_SAUSAGE =
      ITEMS.register(
          "rich_dragon_sausage",
          () ->
              new Item(
                  FOOD_ITEM_PROPERTIES.food(
                      CCFoodComponents.COOKED_RICH_DRAGON_SAUSAGE_FOOD_COMPONENT)));

  public static final DeferredItem<Item> CUD =
      ITEMS.register(
          "cud", () -> new Item(FOOD_ITEM_PROPERTIES.food(CCFoodComponents.CUD_FOOD_COMPONENT)));
  public static final DeferredItem<Item> FURNACE_POWER =
      ITEMS.register(
          "furnace_power",
          () -> new Item(FOOD_ITEM_PROPERTIES.food(CCFoodComponents.FURNACE_POWER_FOOD_COMPONENT)));

  private static Item resolveExternalItem(String namespace, String path) {
    Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(namespace, path));
    return item == null ? Items.AIR : item;
  }
}
