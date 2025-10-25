package net.tigereye.chestcavity.client.modernui.config.docs;

import java.util.HashMap;
import java.util.Map;

/**
 * Provides Chinese translations for category and subcategory folder names used in the docs
 * directory structure.
 */
public final class CategoryTranslations {

  private static final Map<String, String> CATEGORY_NAMES = new HashMap<>();
  private static final Map<String, String> SUBCATEGORY_NAMES = new HashMap<>();

  static {
    // Main categories
    CATEGORY_NAMES.put("human", "人道");
    CATEGORY_NAMES.put("animal", "兽道");

    // Subcategories (dao paths) - based on src/main/resources/assets/guzhenren/docs/human
    SUBCATEGORY_NAMES.put("bian_hua_dao", "变化道");
    SUBCATEGORY_NAMES.put("bing_xue_dao", "冰雪道");
    SUBCATEGORY_NAMES.put("du_dao", "毒道");
    SUBCATEGORY_NAMES.put("feng_dao", "风道");
    SUBCATEGORY_NAMES.put("gu_dao", "骨道");
    SUBCATEGORY_NAMES.put("guang_dao", "光道");
    SUBCATEGORY_NAMES.put("hun_dao", "魂道");
    SUBCATEGORY_NAMES.put("jian_dao", "剑道");
    SUBCATEGORY_NAMES.put("jin_dao", "金道");
    SUBCATEGORY_NAMES.put("lei_dao", "雷道");
    SUBCATEGORY_NAMES.put("li_dao", "力道");
    SUBCATEGORY_NAMES.put("mu_dao", "木道");
    SUBCATEGORY_NAMES.put("ren_dao", "炼道");
    SUBCATEGORY_NAMES.put("shi_dao", "食道");
    SUBCATEGORY_NAMES.put("shui_dao", "水道");
    SUBCATEGORY_NAMES.put("tian_dao", "天道");
    SUBCATEGORY_NAMES.put("tu_dao", "土道");
    SUBCATEGORY_NAMES.put("xin_dao", "心道");
    SUBCATEGORY_NAMES.put("xue_dao", "血道");
    SUBCATEGORY_NAMES.put("yan_dao", "炎道");
    SUBCATEGORY_NAMES.put("yu_dao", "玉道");
    SUBCATEGORY_NAMES.put("yue_dao", "月道");
    SUBCATEGORY_NAMES.put("yun_dao_cloud", "云道");
    SUBCATEGORY_NAMES.put("zhi_dao", "智道");

    // Animal subcategories
    SUBCATEGORY_NAMES.put("tiger_teeth", "虎牙");
    SUBCATEGORY_NAMES.put("eye", "眼睛");
    SUBCATEGORY_NAMES.put("normal", "普通");
  }

  private CategoryTranslations() {}

  /**
   * Gets the Chinese translation for a category folder name.
   *
   * @param category The category folder name (e.g., "human", "animal")
   * @return The Chinese translation, or the original name if no translation exists
   */
  public static String getCategoryName(String category) {
    if (category == null || category.isEmpty()) {
      return "";
    }
    return CATEGORY_NAMES.getOrDefault(category, category);
  }

  /**
   * Gets the Chinese translation for a subcategory folder name.
   *
   * @param subcategory The subcategory folder name (e.g., "bian_hua_dao", "feng_dao")
   * @return The Chinese translation, or the original name if no translation exists
   */
  public static String getSubcategoryName(String subcategory) {
    if (subcategory == null || subcategory.isEmpty()) {
      return "";
    }
    return SUBCATEGORY_NAMES.getOrDefault(subcategory, subcategory);
  }

  /**
   * Gets the full display name for a category and subcategory combination.
   *
   * @param category The category folder name
   * @param subcategory The subcategory folder name
   * @return The combined Chinese display name (e.g., "人道 - 变化道")
   */
  public static String getFullName(String category, String subcategory) {
    String catName = getCategoryName(category);
    String subName = getSubcategoryName(subcategory);

    if (catName.isEmpty() && subName.isEmpty()) {
      return "未分类";
    } else if (catName.isEmpty()) {
      return subName;
    } else if (subName.isEmpty()) {
      return catName;
    } else {
      return catName + " - " + subName;
    }
  }
}
