package net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.tuning;

import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class YuShiSummonTuning {
  public static final String MOD_ID = "guzhenren";
  public static final ResourceLocation ABILITY_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "yu_shi_summon");

  public static final String COOLDOWN_KEY = "YuShiSummonReadyAt";
  public static final int COOLDOWN_TICKS = 20 * 15;
  public static final double ZHENYUAN_COST = 900.0;
  public static final double JINGLI_COST = 10.0;
  public static final int HUNGER_COST = 1;
  public static final int TTL_TICKS = 20 * 120;
  public static final int OFFERING_COST = 64; // 每次召唤消耗 64 个
  public static final int OFFHAND_SLOT = 40; // Vanilla 副手槽位索引

  public static final TagKey<Item> MATERIALS_TAG = TagKey.create(
      BuiltInRegistries.ITEM.key(), ResourceLocation.fromNamespaceAndPath(MOD_ID, "shark_materials"));
  public static final TagKey<Item> TIER1_TAG = TagKey.create(
      BuiltInRegistries.ITEM.key(), ResourceLocation.fromNamespaceAndPath(MOD_ID, "shark_materials_tiered/tier1"));
  public static final TagKey<Item> TIER2_TAG = TagKey.create(
      BuiltInRegistries.ITEM.key(), ResourceLocation.fromNamespaceAndPath(MOD_ID, "shark_materials_tiered/tier2"));
  public static final TagKey<Item> TIER3_TAG = TagKey.create(
      BuiltInRegistries.ITEM.key(), ResourceLocation.fromNamespaceAndPath(MOD_ID, "shark_materials_tiered/tier3"));
  public static final TagKey<Item> TIER4_TAG = TagKey.create(
      BuiltInRegistries.ITEM.key(), ResourceLocation.fromNamespaceAndPath(MOD_ID, "shark_materials_tiered/tier4"));
  public static final TagKey<Item> TIER5_TAG = TagKey.create(
      BuiltInRegistries.ITEM.key(), ResourceLocation.fromNamespaceAndPath(MOD_ID, "shark_materials_tiered/tier5"));

  public static final ResourceLocation[] TIER_ENTITY_IDS = new ResourceLocation[] {
      null,
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "bing_jiao_sha"),
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "bing_lin_sha"),
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "xuan_shuang_bing_sha"),
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "han_yuan_bing_sha"),
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "bing_po_long_wen_sha")
  };

  public static final Map<ResourceLocation, Integer> FALLBACK_MATERIAL_TIERS = Map.ofEntries(
      Map.entry(ResourceLocation.fromNamespaceAndPath(MOD_ID, "sha_yu_ya_chi"), 1),
      Map.entry(ResourceLocation.fromNamespaceAndPath(MOD_ID, "sha_yu_yu_chi"), 1),
      Map.entry(ResourceLocation.fromNamespaceAndPath(MOD_ID, "sha_yu_ya_chi_1"), 2),
      Map.entry(ResourceLocation.fromNamespaceAndPath(MOD_ID, "sha_yu_yu_chi_1"), 2),
      Map.entry(ResourceLocation.fromNamespaceAndPath(MOD_ID, "sha_yu_ya_chi_2"), 3),
      Map.entry(ResourceLocation.fromNamespaceAndPath(MOD_ID, "sha_yu_yu_chi_2"), 3),
      Map.entry(ResourceLocation.fromNamespaceAndPath(MOD_ID, "sha_yu_ya_chi_3"), 4),
      Map.entry(ResourceLocation.fromNamespaceAndPath(MOD_ID, "sha_yu_yu_chi_3"), 4),
      Map.entry(ResourceLocation.fromNamespaceAndPath(MOD_ID, "sha_yu_ya_chi_4"), 5),
      Map.entry(ResourceLocation.fromNamespaceAndPath(MOD_ID, "sha_yu_yu_chi_4"), 5));

  private YuShiSummonTuning() {}
}
