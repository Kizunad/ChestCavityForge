package net.tigereye.chestcavity.compat.guzhenren.registry;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * Guzhenren 数据标签：所有 *_dao.json 项在此维护，服务端/客户端均可安全引用。
 *
 * <p>生成依据：/home/kiz/Code/java/decompile/10_6_decompile/data/guzhenren/tags/item 目录下的
 * *_dao.json。</p>
 */
public final class GZRItemTags {

  private static final String MOD_ID = "guzhenren";

  public static final TagKey<Item> BIANHA_DAO = create("bianha_dao");
  public static final TagKey<Item> BING_XUE_DAO = create("bing_xue_dao");
  public static final TagKey<Item> DU_DAO = create("du_dao");
  public static final TagKey<Item> FENG_DAO = create("feng_dao");
  public static final TagKey<Item> GU_DAO = create("gu_dao");
  public static final TagKey<Item> GUANG_DAO = create("guang_dao");
  public static final TagKey<Item> HUN_DAO = create("hun_dao");
  public static final TagKey<Item> HUO_DAO = create("huo_dao");
  public static final TagKey<Item> JIN_DAO = create("jin_dao");
  public static final TagKey<Item> LI_DAO = create("li_dao");
  public static final TagKey<Item> LIAN_DAO = create("lian_dao");
  public static final TagKey<Item> LV_DAO = create("lv_dao");
  public static final TagKey<Item> MU_DAO = create("mu_dao");
  public static final TagKey<Item> NU_DAO = create("nu_dao");
  public static final TagKey<Item> REN_DAO = create("ren_dao");
  public static final TagKey<Item> SHI_DAO = create("shi_dao");
  public static final TagKey<Item> SHUI_DAO = create("shui_dao");
  public static final TagKey<Item> TIAN_DAO = create("tian_dao");
  public static final TagKey<Item> TOU_DAO = create("tou_dao");
  public static final TagKey<Item> TU_DAO = create("tu_dao");
  public static final TagKey<Item> XIN_DAO = create("xin_dao");
  public static final TagKey<Item> XING_DAO = create("xing_dao");
  public static final TagKey<Item> XUE_DAO = create("xue_dao");
  public static final TagKey<Item> YING_DAO = create("ying_dao");
  public static final TagKey<Item> YU_DAO = create("yu_dao");
  public static final TagKey<Item> YUE_DAO = create("yue_dao");
  public static final TagKey<Item> ZHI_DAO = create("zhi_dao");
  public static final TagKey<Item> ZHOU_DAO = create("zhou_dao");

  public static final Map<String, TagKey<Item>> FLOW_TAGS = buildFlowTags();

  private GZRItemTags() {}

  private static TagKey<Item> create(String path) {
    return TagKey.create(
        Registries.ITEM, ResourceLocation.fromNamespaceAndPath(MOD_ID, path));
  }

  private static Map<String, TagKey<Item>> buildFlowTags() {
    Map<String, TagKey<Item>> map = new LinkedHashMap<>();
    map.put("bianha_dao", BIANHA_DAO);
    map.put("bing_xue_dao", BING_XUE_DAO);
    map.put("du_dao", DU_DAO);
    map.put("feng_dao", FENG_DAO);
    map.put("gu_dao", GU_DAO);
    map.put("guang_dao", GUANG_DAO);
    map.put("hun_dao", HUN_DAO);
    map.put("huo_dao", HUO_DAO);
    map.put("jin_dao", JIN_DAO);
    map.put("li_dao", LI_DAO);
    map.put("lian_dao", LIAN_DAO);
    map.put("lv_dao", LV_DAO);
    map.put("mu_dao", MU_DAO);
    map.put("nu_dao", NU_DAO);
    map.put("ren_dao", REN_DAO);
    map.put("shi_dao", SHI_DAO);
    map.put("shui_dao", SHUI_DAO);
    map.put("tian_dao", TIAN_DAO);
    map.put("tou_dao", TOU_DAO);
    map.put("tu_dao", TU_DAO);
    map.put("xin_dao", XIN_DAO);
    map.put("xing_dao", XING_DAO);
    map.put("xue_dao", XUE_DAO);
    map.put("ying_dao", YING_DAO);
    map.put("yu_dao", YU_DAO);
    map.put("yue_dao", YUE_DAO);
    map.put("zhi_dao", ZHI_DAO);
    map.put("zhou_dao", ZHOU_DAO);
    return Collections.unmodifiableMap(map);
  }
}
