package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.tuning;

import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.BehaviorConfigAccess;

/**
 * 飞剑模型参数（默认显示物品、Geckolib 开关预留）。
 */
public final class FlyingSwordModelTuning {
  private FlyingSwordModelTuning() {}

  /** 默认用于渲染的物品ID（可由服主配置） */
  public static final String DEFAULT_ITEM_ID =
      configStr("DEFAULT_ITEM_ID", "minecraft:iron_sword");

  /** 是否启用 Geckolib 渲染管线（占位） */
  public static final boolean ENABLE_GECKOLIB = configBool("ENABLE_GECKOLIB", false);

  /** 是否在实体上显示绑定物品的名称（悬浮名称牌）。 */
  public static final boolean SHOW_ITEM_NAME = configBool("SHOW_ITEM_NAME", true);

  public static ResourceLocation defaultItemId() {
    try {
      return ResourceLocation.parse(DEFAULT_ITEM_ID);
    } catch (Exception e) {
      return ResourceLocation.withDefaultNamespace("iron_sword");
    }
  }

  private static String configStr(String key, String def) {
    float raw = BehaviorConfigAccess.getFloat(FlyingSwordModelTuning.class, key, Float.NaN);
    // BehaviorConfigAccess 没有直接的字符串接口，这里约定：若配置存在则用浮点表示的索引不生效；回退到def
    // 后续可以扩展 CCConfig 以支持字符串解析，这里先保留默认。
    return def;
  }

  private static boolean configBool(String key, boolean def) {
    return BehaviorConfigAccess.getBoolean(FlyingSwordModelTuning.class, key, def);
  }
}
